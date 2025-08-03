package me.seakeer.learning.javase.network.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SunHttpServer;
 *
 * @author Seakeer;
 * @date 2024/10/29;
 */
public class SunHttpServer {

    private final int port;

    public SunHttpServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        SunHttpServer myHttpServer = new SunHttpServer(8000);
        myHttpServer.start();
    }

    public void start() throws IOException {
        // 指定端口和反向链接队列长度创建HttpServer
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 设置路径和处理器
        server.createContext("/api/http/save", new SaveHandler());
        server.createContext("/api/http/find", new FindHandler());
        server.createContext("/api/http/delete", new DeleteHandler());

        // 设置线程池
        server.setExecutor(new ThreadPoolExecutor(8, 16, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()));

        // 启动服务
        server.start();
        System.out.println("[SunHttpServer] [Running] [Port: " + port + "]");
    }

    static class FindHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            System.out.println("[Find][Query: " + query + "]");
            String response = "Find Okko; ReqUri: " + exchange.getRequestURI();
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class SaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String reqBody = getReqBody(exchange);
            System.out.println("[Save][ReqBody: " + reqBody + "]");
            String response = "Save Okko; ReqUri: " + exchange.getRequestURI() + "; ReqBody: " + reqBody;
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class DeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String reqBody = getReqBody(exchange);
            System.out.println("[Delete][ReqBody: " + reqBody + "]");
            String response = "Delete Okko; ReqUri: " + exchange.getRequestURI() + "; ReqBody: " + reqBody;
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static String getReqBody(HttpExchange exchange) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String str;
        while ((str = bufferedReader.readLine()) != null) {
            stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }
}
