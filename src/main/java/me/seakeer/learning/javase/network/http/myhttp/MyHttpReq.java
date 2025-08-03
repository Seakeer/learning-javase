package me.seakeer.learning.javase.network.http.myhttp;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MyHttpReq;
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
public class MyHttpReq {

    private String method;
    private String path;
    private Map<String, String> params;
    private String version = "HTTP/1.1";
    private Map<String, String> headers;
    private String body;

    public MyHttpReq(String method) {
        this.method = method.toUpperCase();
        this.headers = new HashMap<>();
        this.params = new HashMap<>();
    }

    public String getMethod() {
        return method;
    }

    public MyHttpReq setMethod(String method) {
        this.method = method.toUpperCase();
        return this;
    }

    public String getPath() {
        return path;
    }

    public MyHttpReq setPath(String path) {
        this.path = path.toLowerCase();
        return this;
    }

    public String getVersion() {
        return version;
    }

    public MyHttpReq setVersion(String version) {
        this.version = version.toUpperCase();
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public MyHttpReq setHeaders(Map<String, String> headers) {
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

    public MyHttpReq setBody(String body) {
        this.body = body;
        return this;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public MyHttpReq setParams(Map<String, String> params) {
        if (null == params) {
            this.params = new HashMap<>();
            return this;
        }
        this.params = params.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue));
        return this;
    }

    public MyHttpReq addHeader(String key, String value) {
        this.headers.put(MyHttpProtHandler.formatHeaderKey(key), value);
        return this;
    }

    public String getHeader(String key) {
        return this.headers.get(MyHttpProtHandler.formatHeaderKey(key));
    }

    public boolean containsHeader(String key) {
        return this.headers.containsKey(MyHttpProtHandler.formatHeaderKey(key));
    }

    public MyHttpReq addParam(String key, String value) {
        this.params.put(key.toLowerCase(), value);
        return this;
    }

    public String getUrl() {
        if (params.isEmpty()) {
            return path;
        }
        String paramsStr = params.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((kv1, kv2) -> kv1 + "&" + kv2)
                .orElse("");
        return path + "?" + paramsStr;
    }

    public String fullUrl() {
        String prot = version.split("/")[0].toLowerCase();
        String host = getHeader("Host");
        return prot + "://" + host + getUrl();
//        StringBuilder sb = new StringBuilder();
//        sb.append(uri).append("?");

//        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
//        sb.deleteCharAt(sb.length() - 1);
//        return sb.toString();
    }

    public MyHttpReq parseFullUrl(String fullUrl) {
        String[] hostPathAndParams = getHostPathParams(fullUrl).split("\\?");
        String[] hostAndPath = hostPathAndParams[0].split("/", 2);
        addHeader("Host", hostAndPath[0]);
        if (hostAndPath.length == 1) {
            this.path = "";
        } else {
            this.path = "/" + hostAndPath[1].toLowerCase();
        }
        if (hostPathAndParams.length == 1) {
            return this;
        }
        String[] params = hostPathAndParams[1].split("&");
        for (String kv : params) {
            String[] kvArr = kv.split("=");
            this.params.put(kvArr[0].toLowerCase(), kvArr[1]);
        }
        return this;
    }

    private static String getHostPathParams(String fullUrl) {
        String[] protAndOthers = fullUrl.split("://");
        String hostPathParams;
        if (protAndOthers.length == 1) {
            hostPathParams = fullUrl;
        } else {
            hostPathParams = protAndOthers[1];
        }
        return hostPathParams;
    }
}
