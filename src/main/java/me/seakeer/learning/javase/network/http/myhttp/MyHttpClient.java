package me.seakeer.learning.javase.network.http.myhttp;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;

/**
 * MyHttpClient;
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
public class MyHttpClient {

    public static void main(String[] args) {
        try {
            MyHttpResp resp = send("http://127.0.0.1:8080/test", "Seakeer", "POST");
            System.out.println(resp.getBody());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static MyHttpResp send(String fullUrl, String data, String method) throws IOException {
        URL url = new URL(fullUrl);
        Socket socket = new Socket(url.getHost(), url.getPort());
        MyHttpProtHandler.sendReq(socket.getOutputStream(),
                new MyHttpReq(method.toUpperCase()).parseFullUrl(fullUrl)
                        .setBody(data));
        MyHttpResp myHttpResp = MyHttpProtHandler.parseResp(socket.getInputStream());
        socket.close();
        return myHttpResp;
    }
}
