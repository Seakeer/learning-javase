package me.seakeer.learning.javase.nio.simpleiomodel.syncblocking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * SyncBlockingClient;
 *
 * @author Seakeer;
 * @date 2024/10/12;
 */
public class SyncBlockingClient {

    private final String serverHostname;

    private final int serverPort;

    private Socket socket;

    public SyncBlockingClient(String serverHostname, int serverPort) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) {
        SyncBlockingClient bioClient = new SyncBlockingClient("127.0.0.1", 9999);
        mockSendMsg2Server(bioClient);
        bioClient.start();
    }


    private static void mockSendMsg2Server(SyncBlockingClient client) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                client.sendMsg(scanner.nextLine());
            }
        }).start();
    }

    public SyncBlockingClient connect() {
        try {
            this.socket = new Socket(serverHostname, serverPort);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        System.out.printf("[Client] [Connected to server: %s:%d]\n", serverHostname, serverPort);
        return this;
    }

    public void start() {
        connect();
        run();
    }

    private void run() {
        receivingMsg();
    }

    private void receivingMsg() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            while ((msg = bufferedReader.readLine()) != null) {
                System.out.println("[Client] [Received msg: " + msg + "]");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SyncBlockingClient sendMsg(String msg) {
        if (null == socket) {
            throw new UnsupportedOperationException();
        }
        try {
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            printWriter.println(msg);
            System.out.println("[Client] [Send msg: " + msg + "]");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return this;
    }
}
