package me.seakeer.learning.javase.nio.javaiomodel.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaBioServer;
 *
 * @author Seakeer;
 * @date 2024/10/12;
 */
public class JavaBioServer {

    /**
     * 服务端端口号
     */
    private final int port;

    private ServerSocket serverSocket;

    private final Map<Socket, String> SOCKET_CLIENT_ID_MAP = new ConcurrentHashMap<>();
    private final Map<String, Socket> CLIENT_ID_SOCKET_MAP = new ConcurrentHashMap<>();

    public JavaBioServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        JavaBioServer server = new JavaBioServer(9999);
        mockSendMsg2Client(server);
        server.start();
    }

    private static void mockSendMsg2Client(JavaBioServer server) {
        Thread thread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String msg = scanner.nextLine();
                if (!msg.startsWith("TO ")) {
                    server.sendToAllClient(msg);
                    continue;
                }
                String[] msgParts = msg.split(" ");
                if (msgParts.length < 2) {
                    continue;
                }
                server.sendMsg(msgParts[1], msgParts[2]);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void init() {
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    public void run() {
        System.out.println("[Server] [Server is running]");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server][Accepted client: " + clientSocket.getRemoteSocketAddress() + "]");
                handleClientMsg(clientSocket);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void handleClientMsg(Socket clientSocket) {
        new Thread(() -> doHandleClientMsg(clientSocket)).start();
    }

    private void sendToAllClient(String msg) {
        for (Socket socket : SOCKET_CLIENT_ID_MAP.keySet()) {
            write(socket, msg);
        }
    }

    public boolean sendMsg(String clientId, String msg) {
        Socket targetSocket = CLIENT_ID_SOCKET_MAP.get(clientId);
        if (null == targetSocket) {
            return false;
        }
        write(targetSocket, msg);
        return true;
    }

    private void write(Socket socket, String msg) {
        try {
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            printWriter.println(msg);
            System.out.printf("[Server] [Send Msg] [To client: %s, msg: %s]\n", SOCKET_CLIENT_ID_MAP.get(socket), msg);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void doHandleClientMsg(Socket socket) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            while ((msg = bufferedReader.readLine()) != null) {
                System.out.println("[Server] [Received msg: " + msg + "; From Client: " + SOCKET_CLIENT_ID_MAP.get(socket) + "]");
                if (msg.startsWith("FROM ")) {
                    String clientId = msg.substring(5);
                    SOCKET_CLIENT_ID_MAP.put(socket, clientId);
                    CLIENT_ID_SOCKET_MAP.put(clientId, socket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        init();
        run();
    }
}
