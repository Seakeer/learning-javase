package me.seakeer.learning.javase.network.websocket;

import me.seakeer.learning.javase.network.http.myhttp.MyHttpProtHandler;
import me.seakeer.learning.javase.network.http.myhttp.MyHttpReq;
import me.seakeer.learning.javase.network.http.myhttp.MyHttpResp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocketClient;
 * WebSocket协议客户端
 * 基于JAVA BIO，支持心跳保活，断线重连
 *
 * @author Seakeer;
 * @date 2024/12/29;
 */
public class WebSocketClient {

    public static final String WEB_SOCKET_CLIENT_LOG = "[WebSocketClient] ";

    public static final String CMD_LIST = "[CmdList: START, RESTART, STOP, SHUTDOWN, TO $USERNAME $MSG]";


    private static final Random RANDOM = new Random();

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final String serverHostname;

    private final int serverPort;

    /**
     * 客户端socket
     */
    private volatile Socket socket;

    /**
     * 定时保活线程池
     */
    private volatile ScheduledExecutorService keepAliveThreadPool;

    /**
     * 运行状态
     * 0: 初始状态
     * -1: 已停止
     * 1: 运行中
     */
    private volatile int status = 0;

    /**
     * pong丢失次数
     */
    private volatile int pongLostCount = 0;

    /**
     * 重连次数
     */
    private volatile int reconnectCount = 0;

    /**
     * 最大重连次数，超过则停止运行
     */
    public static final int RECONNECT_MAX_COUNT = 3;

    /**
     * 最大pong丢失次数，超过则进行重连
     */
    public static final int PONG_LOST_MAX_COUNT = 3;

    public WebSocketClient(String serverHostname, int serverPort) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) {
        WebSocketClient webSocketClient = new WebSocketClient("127.0.0.1", 9999);
        cmd(webSocketClient);
    }

    private static void cmd(WebSocketClient webSocketClient) {
        System.out.println(WEB_SOCKET_CLIENT_LOG + "[Client Ready] [Please Input Cmd] " + CMD_LIST);
        System.out.println(WEB_SOCKET_CLIENT_LOG + "[Auto Start] [Client will auto start in 3 seconds]");
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(webSocketClient::start, 3, TimeUnit.SECONDS);
        scheduledExecutorService.shutdown();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                String cmd = scanner.nextLine();
                switch (cmd) {
                    case "START":
                        new Thread(webSocketClient::start).start();
                        break;
                    case "STOP":
                        webSocketClient.stop();
                        break;
                    case "RESTART":
                        new Thread(webSocketClient::restart).start();
                        break;
                    case "SHUTDOWN":
                        webSocketClient.stop();
                        return;
                    default:
                        webSocketClient.sendMsg(cmd);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void start() {
        if (isRunning()) {
            System.out.println(WEB_SOCKET_CLIENT_LOG + "[Already Running]");
            return;
        }
        System.out.println(WEB_SOCKET_CLIENT_LOG + "[Starting]");
        init();
        if (connect()) {
            run();
        }
    }

    public void stop() {
        try {
            System.out.println(WEB_SOCKET_CLIENT_LOG + "[Stopping]");
            status = -1;
            if (null != keepAliveThreadPool) {
                keepAliveThreadPool.shutdown();
                keepAliveThreadPool = null;
            }
            if (null != socket) {
                socket.close();
                socket = null;
            }
            System.out.println(WEB_SOCKET_CLIENT_LOG + "[Stopped]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restart() {
        System.out.println(WEB_SOCKET_CLIENT_LOG + "[Restarting]");
        stop();
        start();
    }

    private void init() {
        keepAliveThreadPool = Executors.newSingleThreadScheduledExecutor();
        reconnectCount = 0;
        pongLostCount = 0;
        status = 0;
    }

    public boolean connect() {
        try {
            this.socket = new Socket(serverHostname, serverPort);
            MyHttpReq req = genWebSocketHandshakeReq();
            MyHttpResp resp = MyHttpProtHandler.send(req, socket.getInputStream(), socket.getOutputStream());
            if (webSocketHandshakeOk(req, resp)) {
                System.out.println(WEB_SOCKET_CLIENT_LOG + "[Handshake Success]");
                onConnected();
                return true;
            } else {
                return reconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return reconnect();
        }
    }

    private void onConnected() {
        pongLostCount = 0;
        reconnectCount = 0;
        sendAuthData();
    }

    public boolean reconnect() {
        try {
            TimeUnit.SECONDS.sleep((reconnectCount + 1) * 3L);
            if (isStopped()) {
                return false;
            }
            if (reconnectCount >= RECONNECT_MAX_COUNT) {
                System.out.println(WEB_SOCKET_CLIENT_LOG + "[Reconnect Stop] [ReconnectMaxCount: " + RECONNECT_MAX_COUNT + "]");
                stop();
                return false;
            }
            reconnectCount++;
            if (null != socket) {
                socket.close();
            }
            System.out.println(WEB_SOCKET_CLIENT_LOG + "[Reconnecting] [ReconnectCount: " + reconnectCount + "]");
            return connect();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void run() {
        System.out.println(WEB_SOCKET_CLIENT_LOG + "[Running]");
        status = 1;
        scheduleKeepAlive();
        while (isRunning()) {
            receivingMsg();
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isRunning() {
        return status == 1;
    }

    private boolean isStopped() {
        return status == -1;
    }

    private void scheduleKeepAlive() {
        this.keepAliveThreadPool.scheduleAtFixedRate(this::keepAlive, 10, 30, TimeUnit.SECONDS);
    }

    private void sendAuthData() {
        String str = "Seakeer" + new Random().nextInt(100);
        sendMsg("AUTH " + str + "=" + str);
    }

    public boolean sendMsg(String msg) {
        try {
            if (socket == null) {
                return false;
            }
            socket.getOutputStream().write(WebSocketEnDecoder.encode(WebSocketFrame.clientTextFrame(msg)));
            socket.getOutputStream().flush();
            System.out.println(WEB_SOCKET_CLIENT_LOG + "[Send Msg] [Msg: " + msg + "]");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(WEB_SOCKET_CLIENT_LOG + "[Send Msg Failed] [Msg: " + msg + "]");
        return false;
    }


    private void keepAlive() {
        if (this.pongLostCount >= PONG_LOST_MAX_COUNT) {
            reconnect();
        } else {
            ping();
            pongLostCount += 1;
        }
    }

    public boolean ping() {
        try {
            this.socket.getOutputStream().write(WebSocketEnDecoder.encode(WebSocketFrame.clientPingFrame()));
            System.out.println(WEB_SOCKET_CLIENT_LOG + "[Send Ping]");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean pong() {
        try {
            socket.getOutputStream().write(WebSocketEnDecoder.encode(WebSocketFrame.clientPongFrame()));
            socket.getOutputStream().flush();
            System.out.println(WEB_SOCKET_CLIENT_LOG + "[Send Pong]");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void handleServerPong() {
        System.out.println(WEB_SOCKET_CLIENT_LOG + "[Received Server Pong]");
        pongLostCount = 0;
    }


    /**
     * 接收服务器发来的数据帧，处理方式同服务端
     */
    private void receivingMsg() {
        try {
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream oneFrameBaos = new ByteArrayOutputStream();
            ByteArrayOutputStream oneDataBaos = new ByteArrayOutputStream();
            while (true) {
                int bytesRead = bis.read(buffer);
                if (bytesRead == -1) {
                    System.out.println(WEB_SOCKET_CLIENT_LOG + "[Disconnected]");
                    reconnect();
                    break;
                }
                oneFrameBaos.write(buffer, 0, bytesRead);

                // 尝试解码当前帧
                WebSocketFrame frame = WebSocketEnDecoder.decode(oneFrameBaos.toByteArray());
                if (frame == null) {
                    continue;
                }
                oneFrameBaos.reset();

                switch (frame.getOpCode()) {
                    case CLOSE_FRAME:
                        byte[] closeFrame = WebSocketEnDecoder.encode(WebSocketFrame.clientCloseFrame());
                        socket.getOutputStream().write(closeFrame);
                        socket.getOutputStream().flush();
                        return;
                    case TEXT_FRAME:
                    case BINARY_FRAME:
                        oneDataBaos.write(frame.deMaskPayload());
                        if (frame.isFin()) {
                            handleOneData(oneDataBaos.toByteArray(), WebSocketFrame.OpCode.TEXT_FRAME.equals(frame.getOpCode()));
                            oneDataBaos.reset();
                        }
                        break;
                    case PING_FRAME:
                        handleServerPing();
                        break;
                    case PONG_FRAME:
                        handleServerPong();
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleServerPing() {
        System.out.println(WEB_SOCKET_CLIENT_LOG + "[Received Server Ping]");
        pong();
    }


    private void handleOneData(byte[] byteArray, boolean textFrame) throws IOException {
        if (textFrame) {
            String msg = new String(byteArray, StandardCharsets.UTF_8);
            if (msg.startsWith("FROM ")) {
                handleFromOtherClientMsg(msg);
            } else {
                System.out.printf(WEB_SOCKET_CLIENT_LOG + "[Received Server Msg] [Msg: %s]\n", msg);
            }
        } else {

        }
    }

    private void handleFromOtherClientMsg(String otherClientMsg) {
        String[] fromMsgArr = otherClientMsg.split(" ");
        if (fromMsgArr.length != 3) {
            System.out.printf(WEB_SOCKET_CLIENT_LOG + "[Received Msg] [Invalid Msg] [Msg: %s]\n", otherClientMsg);
            return;
        }
        System.out.printf(WEB_SOCKET_CLIENT_LOG + "[Received Msg] [From: %s] [Msg: %s]\n", fromMsgArr[1], fromMsgArr[2]);
    }

    private boolean webSocketHandshakeOk(MyHttpReq req, MyHttpResp resp) {
        try {
            String reqKey = req.getHeader("Sec-WebSocket-Key");
            byte[] digest = MessageDigest.getInstance("SHA-1").digest((reqKey + GUID).getBytes(StandardCharsets.UTF_8));
            String secWebSocketAccept = Base64.getEncoder().encodeToString(digest);
            return secWebSocketAccept.equals(resp.getHeader("Sec-WebSocket-Accept"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private MyHttpReq genWebSocketHandshakeReq() {
        String secWebSocketKey = genSecWebSocketKey();
        return new MyHttpReq("GET")
                .parseFullUrl("http://" + serverHostname + ":" + serverPort + "/chat")
                .addHeader("Origin", serverHostname + ":" + serverPort)
                .addHeader("Upgrade", "websocket")
                .addHeader("Connection", "Upgrade")
                .addHeader("Sec-WebSocket-Protocol", "chat")
                .addHeader("Sec-WebSocket-Key", secWebSocketKey)
                .addHeader("Sec-WebSocket-Version", "13");
    }

    private String genSecWebSocketKey() {
        byte[] key = new byte[16];
        RANDOM.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
