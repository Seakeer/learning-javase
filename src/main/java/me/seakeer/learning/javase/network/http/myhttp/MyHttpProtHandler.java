package me.seakeer.learning.javase.network.http.myhttp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;


/**
 * MyHttpProtHandler;
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
public class MyHttpProtHandler {

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String CONTENT_LENGTH = "Content-Length";

    public static MyHttpReq parseReq(InputStream inputStream) {
        try {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            String reqLine;
            // 如果使用BufferedReader读取请求行，则再使用bis将无法读取请求体
            while ((reqLine = readLine(bis)) != null) {
                if (reqLine.isEmpty()) {
                    continue;
                }
                if (!reqLine.startsWith(GET) && !reqLine.startsWith(POST)) {
                    continue;
                }
                String[] firstLineParts = reqLine.split(" ");
                if (firstLineParts.length < 2) {
                    continue;
                }
                // 请求行
                MyHttpReq myHttpReq = new MyHttpReq(firstLineParts[0])
                        .setPath(firstLineParts[1]);
                if (firstLineParts.length == 3) {
                    myHttpReq.setVersion(firstLineParts[2]);
                }
                // 请求头
                String headerLine;
                while ((headerLine = readLine(bis)) != null && !headerLine.isEmpty()) {
                    String[] headerParts = headerLine.split(":\\s*", 2);
                    if (headerParts.length == 2) {
                        myHttpReq.addHeader(headerParts[0], headerParts[1]);
                    }
                }
                // 请求体
                if (myHttpReq.containsHeader(CONTENT_LENGTH)) {
                    int contentLength = Integer.parseInt(myHttpReq.getHeader(CONTENT_LENGTH));
                    myHttpReq.setBody(readBody(bis, contentLength));
                }
                return myHttpReq;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void sendResp(OutputStream out, MyHttpResp myHttpResp) throws IOException {

        StringBuilder responseBuilder = new StringBuilder();
        // 响应行
        responseBuilder.append(myHttpResp.getVersion())
                .append(" ")
                .append(myHttpResp.getStatusCode())
                .append(" ")
                .append(myHttpResp.getStatusMsg())
                .append("\r\n");

        // 响应头
        for (Map.Entry<String, String> entry : myHttpResp.getHeaders().entrySet()) {
            responseBuilder.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue()).append("\r\n");
        }
        if (myHttpResp.getBody() != null && !myHttpResp.getBody().isEmpty() && !myHttpResp.getHeaders().containsKey(CONTENT_LENGTH)) {
            responseBuilder
                    .append(CONTENT_LENGTH)
                    .append(": ")
                    .append(myHttpResp.getBody().getBytes(StandardCharsets.UTF_8).length)
                    .append("\r\n");
        }
        responseBuilder.append("\r\n");

        // 响应体
        if (myHttpResp.getBody() != null) {
            responseBuilder.append(myHttpResp.getBody());
        }

        out.write(responseBuilder.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public static void sendReq(OutputStream out, MyHttpReq myHttpReq) throws IOException {
        StringBuilder reqStrBuilder = new StringBuilder();

        // 请求行
        reqStrBuilder
                .append(myHttpReq.getMethod())
                .append(" ")
                .append(myHttpReq.getUrl())
                .append(" ")
                .append(myHttpReq.getVersion())
                .append("\r\n");

        // 请求头
        for (Map.Entry<String, String> entry : myHttpReq.getHeaders().entrySet()) {
            reqStrBuilder.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue()).append("\r\n");
        }
        if (myHttpReq.getBody() != null && !myHttpReq.getBody().isEmpty() && !myHttpReq.containsHeader(CONTENT_LENGTH)) {
            reqStrBuilder.append(CONTENT_LENGTH)
                    .append(": ")
                    .append(myHttpReq.getBody().getBytes(StandardCharsets.UTF_8).length)
                    .append("\r\n");
        }
        // 空行
        reqStrBuilder.append("\r\n");

        // 请求体
        if (myHttpReq.getBody() != null) {
            reqStrBuilder.append(myHttpReq.getBody());
        }

        out.write(reqStrBuilder.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public static MyHttpResp parseResp(InputStream inputStream) {
        try {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            String respLine;
            while ((respLine = readLine(bis)) != null) {
                if (respLine.isEmpty()) {
                    continue;
                }

                String[] respLineParts = respLine.split(" ", 3);
                if (respLineParts.length < 3) {
                    continue;
                }
                MyHttpResp myHttpResp = new MyHttpResp(Integer.parseInt(respLineParts[1]), respLineParts[2]);
                myHttpResp.setVersion(respLineParts[0]);

                // 响应头
                String headerLine;
                while ((headerLine = readLine(bis)) != null && !headerLine.isEmpty()) {
                    String[] headerParts = headerLine.split(":\\s*", 2);
                    if (headerParts.length == 2) {
                        myHttpResp.addHeader(headerParts[0], headerParts[1]);
                    }
                }

                // 响应体
                if (myHttpResp.containsHeader(CONTENT_LENGTH)) {
                    int contentLength = Integer.parseInt(myHttpResp.getHeader(CONTENT_LENGTH));
                    myHttpResp.setBody(readBody(bis, contentLength));
                }
                return myHttpResp;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MyHttpResp send(MyHttpReq myHttpReq, InputStream in, OutputStream out) throws IOException {
        sendReq(out, myHttpReq);
        return parseResp(in);
    }


    public static String formatHeaderKey(String headerKey) {
        String[] strArr = headerKey.split("-");
        if (strArr.length == 1) {
            return firstUpper(headerKey);
        }
        return Arrays.stream(strArr).map(MyHttpProtHandler::firstUpper)
                .reduce((str1, str2) -> str1 + "-" + str2)
                .orElse("");

    }

    public static String firstUpper(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private static String readLine(BufferedInputStream bis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        while ((c = bis.read()) != -1) {
            if (c == '\n') {
                break;
            }
            if (c != '\r') {
                baos.write(c);
            }
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    private static String readBody(BufferedInputStream bis, int contentLength) throws IOException {
        if (contentLength <= 0) {
            return null;
        }
        byte[] bodyBuffer = new byte[contentLength];
        int bytesRead;
        int totalBytesRead = 0;
        while (totalBytesRead < contentLength && (bytesRead = bis.read(bodyBuffer, totalBytesRead, contentLength - totalBytesRead)) != -1) {
            totalBytesRead += bytesRead;
        }
        if (totalBytesRead == contentLength) {
            return new String(bodyBuffer, StandardCharsets.UTF_8);
        }
        return null;
    }
}
