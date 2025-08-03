package me.seakeer.learning.javase.network.http.myhttp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * MyHttpServer;
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
public class MyHttpServer {

    private volatile boolean running = false;

    private final int port;

    private ServerSocket serverSocket;

    private ExecutorService executorService;

    public MyHttpServer(int port) {
        this.port = port;
    }

    private void init() {
        try {
            this.serverSocket = new ServerSocket(port);
            this.executorService = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(32));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run() {
        this.running = true;
        System.out.println("[MyHttpServer] [Running] [Port: " + port + "]");
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new ClientHandler(clientSocket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        init();
        run();
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                MyHttpReq myHttpReq = MyHttpProtHandler.parseReq(clientSocket.getInputStream());
                if (null == myHttpReq) {
                    MyHttpProtHandler.sendResp(clientSocket.getOutputStream(), MyHttpResp.badRequest("Req Error"));
                } else {
                    MyHttpProtHandler.sendResp(clientSocket.getOutputStream(), MyHttpResp.success(myHttpReq.getBody()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new MyHttpServer(8080).start();
    }
}
