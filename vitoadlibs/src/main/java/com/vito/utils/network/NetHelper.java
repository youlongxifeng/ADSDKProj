package com.vito.utils.network;

import com.google.gson.Gson;
import com.vito.utils.Log;
import com.vito.utils.MD5Util;
import com.vito.utils.StringUtil;
import com.vito.utils.ThreadExecutor;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetHelper {
    private static final Byte DEFAULT_TIME_OUT = 3;
    private static OkHttpClient client = new OkHttpClient();
    private static String UA = "";

//    Request request =
//httpClient.newCall(request).enqueue(handler);

    private static Request buildMyRequest(String url){
        if (NetHelper.UA==null||NetHelper.UA.isEmpty()){
            return new Request.Builder().url(url).build();
        }
        return new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
            NetHelper.UA).build();
    }

    private static Request buildMyRequest(String url, RequestBody postbody){
        if (NetHelper.UA==null||NetHelper.UA.isEmpty()){
            return new Request
                    .Builder()
                    .post(postbody)
                    .url(url)
                    .build();
        }
        return new Request
                .Builder()
                .url(url)
                .post(postbody)
                .removeHeader("User-Agent")
                .addHeader("User-Agent", NetHelper.UA)
                .build();
    }


    public static boolean doHttpCall(String url, int checkTimesOut) {
//        Request request = new Request.Builder().url(url).build();
        Request request = buildMyRequest(url);
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                Log.e("ADTEST", "success");
                return true;
            } else {
                // 失败重新请求
                if (checkTimesOut>0) {
                    checkTimesOut--;
                    doHttpCall(url, checkTimesOut);
                }else {
                    return false;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static void sendGetRequest(final String url) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                doHttpCall(url, DEFAULT_TIME_OUT);

            }
        };
        ThreadExecutor.getInstance().addTask(task);
    }

    public static void sendPostRequest(final String url, final JSONObject json, final int times) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                String result = doPostRequest(url, json, times);
                Log.e("ADTEST", "call post request = "+result);
            }
        };
        ThreadExecutor.getInstance().addTask(task);

    }

    /**
     *  发送普通的get请求
     * @param url
     * @param checkTimesOut
     * @return
     */

    public static String doGetHttpResponse(String url, int checkTimesOut) {
        String result = "";
//        Request request = new Request.Builder().url(url).build();
        Request request = buildMyRequest(url);
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                result = response.body().string();
                response.body().close();
                return result;
            } else {
                // 失败重新请求
                if (checkTimesOut>0) {
                    checkTimesOut--;
                    result = doGetHttpResponse(url, checkTimesOut);
                }else {
                    result = response.body().string();
                    response.body().close();
                    return result;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String doPostRequest(String url, Map<String,String> params, int times){
        String result = "";
        if (params == null) throw new NullPointerException("params is null");
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        Set<String> keySet = params.keySet();
        for(String key:keySet) {
            String value = params.get(key);
            formBodyBuilder.add(key,value);
        }
        FormBody formBody = formBodyBuilder.build();
//        Request request = new Request
//                .Builder()
//                .post(formBody)
//                .url(url)
//                .build();
        Request request = buildMyRequest(url, formBody);
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()){
             result = response.body().string();
             response.body().close();
             return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     *  使用 json
     * @param url
     * @param json
     * @param times
     * @return
     */
    public static String doPostRequest(String url, JSONObject json, int times){
        String result = "";
        if (json == null) throw new NullPointerException("params json is null");
        MediaType mediaType = MediaType.parse("application/json");
        //创建RequestBody对象，将参数按照指定的MediaType封装
        RequestBody jsonBody = RequestBody.create(mediaType,json.toString());
//        Request request = new Request
//                .Builder()
//                .post(jsonBody)
//                .url(url)
//                .build();
        Request request = buildMyRequest(url, jsonBody);
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()){
                result = response.body().string();
                response.body().close();
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     *  使用 json
     * @param url
     * @param json
     * @param times
     * @return
     */
    public static String doPostRequest(String url, String json, int times){
        String result = "";
        if (json == null) throw new NullPointerException("params json is null");
        MediaType mediaType = MediaType.parse("application/json");
        //创建RequestBody对象，将参数按照指定的MediaType封装
        RequestBody jsonBody = RequestBody.create(mediaType, json);
//        Request request = new Request
//                .Builder()
//                .post(jsonBody)
//                .url(url)
//                .build();
        Request request = buildMyRequest(url, jsonBody);
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()){
                result = response.body().string();
                response.body().close();
                return result;
            }else {
                if (times>0)
                    return doPostRequest(url, json, times--);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }



    /**
     * 将url参数转换成map
     * @param param aa=11&bb=22&cc=33
     * @return
     */
    public static Map<String, Object> getUrlParams(String param) {
        Map<String, Object> map = new HashMap<String, Object>(0);
        if (StringUtil.isEmpty(param)) {
            return map;
        }
        String[] params = param.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] p = params[i].split("=");
            if (p.length == 2) {
                map.put(p[0], p[1]);
            }
        }
        return map;
    }

    /**
     * 将map转换成url
     * @param map
     * @return
     */
    public static String getUrlParamsByMap(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue());
            sb.append("&");
        }
        sb.deleteCharAt(sb.length()-1);
        String s = sb.toString();
        Log.e("ADTEST", "m2p s = " +s);
        return s;
    }

    public static void callWithNoResponse(String url, String method,JSONObject params){
        StringBuilder sb = new StringBuilder(url);
        String timestamp=  System.currentTimeMillis()/1000+"";
        String token = MD5Util.encrypt(method+"wx2017"+timestamp);
        sb.append(method)
                .append("&param=")
                .append(encode(params.toString()))
                .append("&token=")
                .append(token)
                .append("&timestamp=")
                .append(timestamp);
        Log.e("ADTEST", sb.toString());
        NetHelper.sendGetRequest(sb.toString());
    }

    public static String callWithResponse(String url, String method,JSONObject jsonObject){

        StringBuilder sb = new StringBuilder(url);
        String timestamp=  System.currentTimeMillis()/1000+"";
        String token = MD5Util.encrypt(method+"wx2017"+timestamp);
        sb.append(method)
                .append("&param=")
                .append(encode(jsonObject.toString()))
                .append("&token=")
                .append(token)
                .append("&timestamp=")
                .append(timestamp);
        Log.e("ADTEST", sb.toString());
        return NetHelper.doGetHttpResponse(sb.toString(), 1);
    }


    public static String callWithResponse(String url, String method,String paramstr){

        StringBuilder sb = new StringBuilder(url);
        String timestamp=  System.currentTimeMillis()/1000+"";
        String token = MD5Util.encrypt(method+"wx2017"+timestamp);
        sb.append(method)
                .append("&param=")
                .append(encode(paramstr))
                .append("&token=")
                .append(token)
                .append("&timestamp=")
                .append(timestamp);
        Log.e("ADTEST", sb.toString());
        return NetHelper.doGetHttpResponse(sb.toString(), 1);
    }

    private static String encode(String paramstr){
        String params = "{}";
        try {
            params = URLEncoder.encode(paramstr, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return  params;
    }

    public static String getIP(){
        // http://pv.sohu.com/cityjson  通过访问网站获得ip返回
        String url = "http://pv.sohu.com/cityjson";
        String result = doGetHttpResponse(url,2);
        android.util.Log.e("IP", "result = "+ result);
        if (result.length()>0){
            result = result.substring(result.indexOf("{"), result.lastIndexOf("}")+1);
            Gson gson = new Gson();
            try {
                IPNode ipNode = gson.fromJson(result, IPNode.class);
                return ipNode.getCip();
            }catch (Exception e){
                android.util.Log.e("IPERROR", "error = "+ e.toString());
            }
        }
        return "";
    }

    private static String enCodeUserAgent(String userAgent) {

        StringBuffer sb = new StringBuffer();
        for (int i = 0, length = userAgent.length(); i < length; i++) {
            char c = userAgent.charAt(i);
            if (c <= '\u001f' || c >= '\u007f') {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void setUA(String UA) {
        NetHelper.UA = enCodeUserAgent(UA);
    }

    class IPNode {
        private String cip;
        private String cid;
        private String cname;

        public String getCip() {
            return cip;
        }

        public void setCip(String cip) {
            this.cip = cip;
        }

        public String getCid() {
            return cid;
        }

        public void setCid(String cid) {
            this.cid = cid;
        }

        public String getCname() {
            return cname;
        }

        public void setCname(String cname) {
            this.cname = cname;
        }
    }
}
