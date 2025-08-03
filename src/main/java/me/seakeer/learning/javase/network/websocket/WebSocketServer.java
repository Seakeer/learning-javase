package me.seakeer.learning.javase.network.websocket;

import me.seakeer.learning.javase.network.http.myhttp.MyHttpProtHandler;
import me.seakeer.learning.javase.network.http.myhttp.MyHttpReq;
import me.seakeer.learning.javase.network.http.myhttp.MyHttpResp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * WebSocketServer;
 * WebSocket协议服务端
 * 基于JAVA BIO
 *
 * @author Seakeer;
 * @date 2024/12/29;
 */
public class WebSocketServer {

    public static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    public static final String WEB_SOCKET_SERVER_LOG = "[WebSocketServer] ";
    public static final String CMD_LIST = "[START, RESTART, STOP, SHUTDOWN, TO $CLIENT_ID $MSG]";

    /**
     * 服务端端口号
     */
    private final int port;

    /**
     * 是否运行中
     */
    private volatile boolean running = false;

    /**
     * 服务端socket
     */
    private volatile ServerSocket serverSocket;

    /**
     * 处理连接的线程池
     */
    private volatile ExecutorService handleClientThreadPool;

    /**
     * 存储认证通过的客户端连接
     */
    private final Map<Socket, String> SOCKET_CLIENT_ID_MAP = new ConcurrentHashMap<>();
    private final Map<String, Socket> CLIENT_ID_SOCKET_MAP = new ConcurrentHashMap<>();


    public WebSocketServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        WebSocketServer webSocketServer = new WebSocketServer(9999);
        cmd(webSocketServer);
    }

    private static void cmd(WebSocketServer webSocketServer) {
        System.out.println(WEB_SOCKET_SERVER_LOG + "[Server Ready] [Please Input Cmd]" + CMD_LIST);
        System.out.println(WEB_SOCKET_SERVER_LOG + "[AutoStart] [Server will auto start in 3 seconds]");
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(webSocketServer::start, 3, TimeUnit.SECONDS);
        scheduledExecutorService.shutdown();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                String cmd = scanner.nextLine();
                switch (cmd) {
                    case "START":
                        new Thread(webSocketServer::start).start();
                        break;
                    case "STOP":
                        webSocketServer.stop();
                        break;
                    case "RESTART":
                        new Thread(webSocketServer::restart).start();
                        break;
                    case "SHUTDOWN":
                        webSocketServer.stop();
                        return;
                    default:
                        if (cmd.startsWith("TO ")) {
                            String[] msgParts = cmd.split(" ", 3);
                            if (msgParts.length < 3) {
                                System.out.printf(WEB_SOCKET_SERVER_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                                continue;
                            }
                            webSocketServer.send(msgParts[1], msgParts[2]);
                        } else {
                            System.out.printf(WEB_SOCKET_SERVER_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        System.out.println(WEB_SOCKET_SERVER_LOG + "[Starting]");
        init();
        run();
    }

    public void stop() {
        try {
            System.out.println(WEB_SOCKET_SERVER_LOG + "[Stopping]");
            running = false;
            closeAllClient();
            if (handleClientThreadPool != null) {
                handleClientThreadPool.shutdown();
                handleClientThreadPool = null;
            }
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
            System.out.println(WEB_SOCKET_SERVER_LOG + "[Stopped]");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void closeAllClient() {
        for (Socket socket : SOCKET_CLIENT_ID_MAP.keySet()) {
            try {
                socket.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        SOCKET_CLIENT_ID_MAP.clear();
        CLIENT_ID_SOCKET_MAP.clear();
    }

    public void restart() {
        System.out.println(WEB_SOCKET_SERVER_LOG + "[Restarting]");
        stop();
        start();
    }

    public void init() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            this.handleClientThreadPool = new ThreadPoolExecutor(8, 16, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    public void run() {
        running = true;
        System.out.println(WEB_SOCKET_SERVER_LOG + "[Running]");
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println(WEB_SOCKET_SERVER_LOG + "[Accepted Client] " + "[ClientAddr: " + clientSocket.getRemoteSocketAddress() + "]");
                handleClientThreadPool.execute(() -> handleClient(clientSocket));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            MyHttpReq myHttpReq = MyHttpProtHandler.parseReq(clientSocket.getInputStream());
            if (isWebSocketHandshake(myHttpReq)) {
                MyHttpResp myHttpResp = genHandshakeResp(myHttpReq);
                if (agreeWebSocketHandshake(myHttpResp)) {
                    MyHttpProtHandler.sendResp(clientSocket.getOutputStream(), myHttpResp);
                    handleClientWebSocketData(clientSocket);
                } else {
                    clientSocket.close();
                }
            } else {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MyHttpResp genHandshakeResp(MyHttpReq myHttpReq) {
        try {
            String reqKey = myHttpReq.getHeader("Sec-WebSocket-Key");
            byte[] digest = MessageDigest.getInstance("SHA-1").digest((reqKey + GUID).getBytes(StandardCharsets.UTF_8));
            String secWebSocketAccept = Base64.getEncoder().encodeToString(digest);
            return new MyHttpResp(101, "Switching Protocols")
                    .addHeader("Upgrade", "websocket")
                    .addHeader("Connection", "Upgrade")
                    .addHeader("Sec-WebSocket-Accept", secWebSocketAccept);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return MyHttpResp.serverError(e.getMessage());
        }
    }

    private boolean isWebSocketHandshake(MyHttpReq myHttpReq) {
        if (null == myHttpReq) {
            return false;
        }
        String upgrade = myHttpReq.getHeader("Upgrade");
        String connection = myHttpReq.getHeader("Connection".toUpperCase());
        return "websocket".equalsIgnoreCase(upgrade) && "Upgrade".equalsIgnoreCase(connection);
    }

    private void handleClientWebSocketData(Socket clientSocket) {
        try {
            BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream());
            byte[] buffer = new byte[1024];
            // 存储一个数据帧的字节输出流
            ByteArrayOutputStream oneFrameBaos = new ByteArrayOutputStream();
            // 存储一条数据的字节输出流，可能包含多个数据帧
            ByteArrayOutputStream oneDataBaos = new ByteArrayOutputStream();
            while (true) {
                int bytesRead = bis.read(buffer);
                if (bytesRead == -1) {
                    handleDisconnectClient(clientSocket);
                    break;
                }
                oneFrameBaos.write(buffer, 0, bytesRead);

                while (true) {
                    // 尝试解码当前帧
                    WebSocketFrame frame = WebSocketEnDecoder.decode(oneFrameBaos.toByteArray());
                    if (frame == null) {
                        // 没有解码出一个数据帧，继续读取数据进行解码
                        break;
                    }
                    byte[] remainingData = oneFrameBaos.toByteArray();
                    oneFrameBaos.reset();
                    // 解码出一个帧后，可能多读取了一些字节，需要写入多读取的数据，交给下一个数据帧
                    oneFrameBaos.write(remainingData, frame.getFrameLengthBytes(), remainingData.length - frame.getFrameLengthBytes());
                    switch (frame.getOpCode()) {
                        case CLOSE_FRAME:
                            sendCloseFrame(clientSocket);
                            handleDisconnectClient(clientSocket);
                            return;
                        case TEXT_FRAME:
                        case BINARY_FRAME:
                            // 读取到的数据帧是文本帧或二进制帧，写入到一条数据的字节流中
                            oneDataBaos.write(frame.deMaskPayload());
                            if (frame.isFin()) {
                                // 数据帧是结束帧，说明一条数据读取完毕，可以处理了
                                handleOneData(oneDataBaos.toByteArray(),
                                        WebSocketFrame.OpCode.TEXT_FRAME.equals(frame.getOpCode()),
                                        clientSocket);
                                oneDataBaos.reset();
                            }
                            break;
                        case PING_FRAME:
                            handleClientPing(clientSocket);
                            break;
                        case PONG_FRAME:
                            handleClientPong(clientSocket);
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendCloseFrame(Socket clientSocket) {
        try {
            byte[] closeFrame = WebSocketEnDecoder.encode(WebSocketFrame.serverCloseFrame());
            clientSocket.getOutputStream().write(closeFrame);
            clientSocket.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDisconnectClient(Socket clientSocket) {
        if (clientSocket == null) {
            return;
        }
        String clientId = SOCKET_CLIENT_ID_MAP.get(clientSocket);
        System.out.println(WEB_SOCKET_SERVER_LOG + "[Client Disconnected][ClientId: " + clientId + "]");
        SOCKET_CLIENT_ID_MAP.remove(clientSocket);
        CLIENT_ID_SOCKET_MAP.remove(clientId);
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleOneData(byte[] data, boolean textFrame, Socket clientSocket) throws IOException {
        if (textFrame) {
            String msg = new String(data, StandardCharsets.UTF_8);
            System.out.printf(WEB_SOCKET_SERVER_LOG + "[Received Text Data] [Data: %s]\n", msg);
            handleClientMsg(clientSocket, msg);
        } else {
            System.out.printf(WEB_SOCKET_SERVER_LOG + "[Received Binary Data] [Data Bytes: %d]\n", data.length);
        }
    }

    public boolean send(String clientId, String msg) {
        if (send(CLIENT_ID_SOCKET_MAP.get(clientId), msg)) {
            System.out.printf(WEB_SOCKET_SERVER_LOG + "[Send Msg] [ClientId: %s] [Msg: %s]\n", clientId, msg);
            return true;
        } else {
            System.out.println(WEB_SOCKET_SERVER_LOG + "[Send Msg Failed] [ClientId: " + clientId + "]" + "Msg: " + msg);
            return false;
        }
    }

    public boolean send(Socket clientSocket, String msg) {
        try {
            if (null == clientSocket) {
                return false;
            }
            clientSocket.getOutputStream().write(WebSocketEnDecoder.encode(WebSocketFrame.clientTextFrame(msg)));
            clientSocket.getOutputStream().flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void handleClientPong(Socket clientSocket) {
        System.out.printf(WEB_SOCKET_SERVER_LOG + "[Received Pong][ClientId: %s]\n", SOCKET_CLIENT_ID_MAP.get(clientSocket));
    }

    private void handleClientPing(Socket clientSocket) throws IOException {
        // 处理 PING 帧，发送 PONG 帧作为响应
        System.out.printf(WEB_SOCKET_SERVER_LOG + "[Received Ping][ClientId: %s]\n", SOCKET_CLIENT_ID_MAP.get(clientSocket));
        pong(clientSocket);
    }

    private void pong(Socket clientSocket) throws IOException {
        byte[] pongFrame = WebSocketEnDecoder.encode(WebSocketFrame.serverPongFrame());
        clientSocket.getOutputStream().write(pongFrame);
        clientSocket.getOutputStream().flush();
        System.out.printf(WEB_SOCKET_SERVER_LOG + "[Send Pong] [ClientId: %s]\n", SOCKET_CLIENT_ID_MAP.get(clientSocket));
        System.out.printf(WEB_SOCKET_SERVER_LOG + "[Head Count] [SocketCount: %d; ClientIdCount: %d]\n",
                SOCKET_CLIENT_ID_MAP.size(),
                CLIENT_ID_SOCKET_MAP.size());
    }

    private void handleClientMsg(Socket clientSocket, String msg) {
        try {
            if (msg.startsWith("AUTH ")) {
                auth(clientSocket, msg);
            } else if (isAuthed(clientSocket)) {
                System.out.printf(WEB_SOCKET_SERVER_LOG + "[Received Msg] [ClientId: %s] [Msg: %s]\n", SOCKET_CLIENT_ID_MAP.get(clientSocket), msg);
                if (msg.startsWith("TO ")) {
                    String[] toClientIdMsg = msg.split(" ");
                    send(toClientIdMsg[1], "FROM " + toClientIdMsg[1] + " " + toClientIdMsg[2]);
                }
            } else {
                System.out.println(WEB_SOCKET_SERVER_LOG + "[Not Auth] " + "[ClientAddr: " + clientSocket.getRemoteSocketAddress() + "]");
                clientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean auth(Socket clientSocket, String authData) {
        try {
            if (!authData.startsWith("AUTH ")) {
                System.out.printf(WEB_SOCKET_SERVER_LOG + "[ClientAuthFailed] [NotAuthData] [SocketChannelAddr: %s]\n",
                        clientSocket.getRemoteSocketAddress());
                return false;
            }
            String[] parts = authData.split(" ", 2);
            if (parts.length != 2) {
                send(clientSocket, "AUTH_FAILED:INVALID_AUTH_DATA");
                System.out.printf(WEB_SOCKET_SERVER_LOG + "[ClientAuthFailed] [InvalidAuthData] [SocketChannelAddr: %s]\n",
                        clientSocket.getRemoteSocketAddress());
                return false;
            }
            String[] usernameAndPwdArr = parts[1].split("=");
            if (usernameAndPwdArr.length != 2) {
                send(clientSocket, "AUTH_FAILED:USERNAME_PWD_NOT_MATCH");
                System.out.printf(WEB_SOCKET_SERVER_LOG + "[ClientAuthFailed] [UsernamePwdNotMatch] [SocketChannelAddr: %s]\n",
                        clientSocket.getRemoteSocketAddress());
                return false;
            }
            String username = usernameAndPwdArr[0];
            String password = usernameAndPwdArr[1];
            if (mockAuthenticate(username, password)) {
                SOCKET_CLIENT_ID_MAP.put(clientSocket, username);
                CLIENT_ID_SOCKET_MAP.put(username, clientSocket);
                send(clientSocket, "AUTH_SUCCESS:" + username);
                System.out.printf(WEB_SOCKET_SERVER_LOG + "[ClientAuthSuccess] [ClientId: %s]\n", username);
                return true;
            } else {
                send(clientSocket, "AUTH_FAILED:USERNAME_PWD_NOT_MATCH");
                System.out.printf(WEB_SOCKET_SERVER_LOG + "[ClientAuthFailed] [UsernamePwdNotMatch] [SocketChannelAddr: %s]\n",
                        clientSocket.getRemoteSocketAddress());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean mockAuthenticate(String username, String pwd) {
        return username.equals(pwd);
    }

    private boolean isAuthed(Socket clientSocket) {
        return SOCKET_CLIENT_ID_MAP.containsKey(clientSocket);
    }

    private boolean agreeWebSocketHandshake(MyHttpResp myHttpResp) {
        return myHttpResp.getStatusCode() == 101;
    }

}
