package me.seakeer.learning.javase.network.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * JavaNetHttpClient;
 *
 * @author Seakeer;
 * @date 2024/12/24;
 */
public class JavaNetHttpClient {

    public static void main(String[] args) throws IOException {
        String getResponse = send("http://localhost:8000/api/http/find", null, "GET");
        System.out.println("GET Response: " + getResponse);

        String postResponse = send("http://localhost:8000/api/http/save", "xxx", "POST");
        System.out.println("POST Response: " + postResponse);
    }

    public static String send(String urlStr, String reqBody, String method) throws IOException {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method.toUpperCase());
        conn.setDoOutput(true);

        if (null != reqBody) {
            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                out.writeBytes(reqBody);
                out.flush();
            }
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }
}