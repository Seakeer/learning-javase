package me.seakeer.learning.javase.network.http.myhttp;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MyHttpResp;
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
public class MyHttpResp {

    private String version = "HTTP/1.1";
    private int statusCode;
    private String statusMsg;
    private Map<String, String> headers;
    private String body;

    public MyHttpResp(int statusCode, String statusMsg) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
        this.headers = new HashMap<>();
    }

    public String getVersion() {
        return version;
    }

    public MyHttpResp setVersion(String version) {
        this.version = version.toUpperCase();
        return this;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public MyHttpResp setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public MyHttpResp setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public MyHttpResp setHeaders(Map<String, String> headers) {
        if (null == headers) {
            this.headers = new HashMap<>();
            return this;
        }
        this.headers = headers.entrySet().stream()
                .collect(Collectors.toMap(entry -> MyHttpProtHandler.formatHeaderKey(entry.getKey()), Map.Entry::getValue));
        return this;
    }

    public String getBody() {
        return body;
    }

    public MyHttpResp setBody(String body) {
        this.body = body;
        return this;
    }

    public MyHttpResp addHeader(String key, String value) {
        this.headers.put(MyHttpProtHandler.formatHeaderKey(key), value);
        return this;
    }

    public String getHeader(String key) {
        return this.headers.get(MyHttpProtHandler.formatHeaderKey(key));
    }

    public boolean containsHeader(String key) {
        return this.headers.containsKey(MyHttpProtHandler.formatHeaderKey(key));
    }

    public boolean isOk() {
        return 200 == statusCode;
    }

    public static MyHttpResp success(String data) {
        MyHttpResp myHttpResp = new MyHttpResp(200, "OK");
        myHttpResp.setBody(data);
        return myHttpResp;
    }

    public static MyHttpResp badRequest(String msg) {
        MyHttpResp myHttpResp = new MyHttpResp(400, "Bad Request");
        myHttpResp.setBody(msg);
        return myHttpResp;
    }

    public static MyHttpResp serverError(String msg) {
        MyHttpResp myHttpResp = new MyHttpResp(500, "Server Error");
        myHttpResp.addHeader(MyHttpProtHandler.CONTENT_LENGTH, String.valueOf(msg.length()));
        myHttpResp.setBody(msg);
        return myHttpResp;
    }
}
