package me.seakeer.learning.javase.network.ssltls;

import me.seakeer.learning.javase.network.MsgEnDecoder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;

/**
 * SslServer;
 *
 * @author Seakeer;
 * @date 2024/10/29;
 */
public class MainSslServer {

    public static final String DELIMITER = "v^";

    public static final String SSL_SERVER_LOG = "[SslServer] ";
    public static final String CMD_LIST = "[START, RESTART, STOP, SHUTDOWN, TO $CLIENT_ID $MSG, CLOSE $CLIENT_ID]";

    private final int port;

    private volatile ServerSocketChannel serverSocketChannel;

    private volatile SSLContext sslContext;

    private volatile Selector selector;

    private volatile boolean running = false;

    private final Map<SocketChannel, String> SOCKET_CHANNEL_CLIENT_ID_MAP = new ConcurrentHashMap<>();
    private final Map<String, SocketChannel> CLIENT_ID_SOCKET_CHANNEL_MAP = new ConcurrentHashMap<>();
    private final Map<SocketChannel, MainSslManager> SOCKET_CHANNEL_SSL_MANAGER_MAP = new ConcurrentHashMap<>();

    /**
     * 发送消息时，如果Channel不可写，则缓存消息到待发送消息队列
     */
    private final Map<SocketChannel, ConcurrentLinkedQueue<String>> SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP = new ConcurrentHashMap<>();
    private final Map<SocketChannel, StringBuilder> SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP = new ConcurrentHashMap<>();

    public MainSslServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        MainSslServer tcpServer = new MainSslServer(9090);
        cmd(tcpServer);
    }

    private static void cmd(MainSslServer sslServer) {
        System.out.println(SSL_SERVER_LOG + "[Server Ready] [Please Input Cmd] " + CMD_LIST);
        System.out.println(SSL_SERVER_LOG + "[AutoStart] [Server will auto start in 3 seconds]");
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(sslServer::start, 3, TimeUnit.SECONDS);
        scheduledExecutorService.shutdown();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                String cmd = scanner.nextLine();
                switch (cmd) {
                    case "START":
                        new Thread(sslServer::start).start();
                        break;
                    case "STOP":
                        sslServer.stop();
                        break;
                    case "RESTART":
                        new Thread(sslServer::restart).start();
                        break;
                    case "SHUTDOWN":
                        sslServer.stop();
                        System.out.println(SSL_SERVER_LOG + "[Shutdown]");
                        return;
                    default:
                        if (cmd.startsWith("TO ")) {
                            if (cmd.contains(DELIMITER)) {
                                System.out.println(SSL_SERVER_LOG + "[ContainsDelimiter]");
                            }
                            String[] msgParts = cmd.split(" ", 3);
                            if (msgParts.length < 3) {
                                System.out.printf(SSL_SERVER_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                                continue;
                            }
                            sslServer.send(msgParts[1], msgParts[2]);
                        } else if (cmd.startsWith("CLOSE ")) {
                            String[] msgParts = cmd.split(" ", 2);
                            if (msgParts.length < 2) {
                                System.out.printf(SSL_SERVER_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                                continue;
                            }
                            sslServer.closeClient(msgParts[1]);
                        } else {
                            System.out.printf(SSL_SERVER_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        if (running) {
            System.out.println(SSL_SERVER_LOG + "[AlreadyRunning]");
            return;
        }
        System.out.println(SSL_SERVER_LOG + "[Starting]");
        if (init()) {
            run();
        } else {
            System.out.println(SSL_SERVER_LOG + "[Init Failed]");
        }
    }

    public void stop() {
        try {
            System.out.println(SSL_SERVER_LOG + "[Stopping]");
            running = false;
            closeAllClient();
            if (null != serverSocketChannel) {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
            if (null != selector) {
                selector.close();
                selector = null;
            }
            System.out.println(SSL_SERVER_LOG + "[Stopped]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restart() {
        System.out.println(SSL_SERVER_LOG + "[Restarting]");
        stop();
        start();
    }

    private boolean init() {
        try {
            sslContext = MainSslManager.crtSslContext("Server.jks", "seakeer", "SeakeerCaCert.jks", "seakeer");
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(port));
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void run() {
        running = true;
        System.out.println(SSL_SERVER_LOG + "[Running]");
        while (running) {
            try {
                int keysNum = selector.select(1000);
                if (keysNum <= 0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    // 移除当前处理的 SelectionKey
                    iterator.remove();
                    if (!selectionKey.isValid()) {
                        continue;
                    }
                    if (selectionKey.isAcceptable()) {
                        handleOpAccept();
                    }
                    if (selectionKey.isReadable()) {
                        handleOpRead(selectionKey);
                    }
                    if (selectionKey.isValid() && selectionKey.isWritable()) {
                        handleOpWrite(selectionKey);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleOpAccept() {
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            System.out.println(SSL_SERVER_LOG + "[Accepted Client] " + "[ClientAddr: " + socketChannel.getRemoteAddress() + "]");

            // 创建 SSLEngine
            SSLEngine sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(false);
            MainSslManager sslManager = new MainSslManager(sslEngine);
            // 开始 SSL/TLS 握手
            if (sslManager.openHandshake(socketChannel)) {
                System.out.println(SSL_SERVER_LOG + "[Handshake Succeed] " + "[ClientAddr: " + socketChannel.getRemoteAddress() + "]");
                // 注册客户端通道
                socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, sslManager);
                SOCKET_CHANNEL_SSL_MANAGER_MAP.put(socketChannel, sslManager);
            } else {
                System.out.println(SSL_SERVER_LOG + "[Handshake Failed] " + "[ClientAddr: " + socketChannel.getRemoteAddress() + "]");
                closeConnection(socketChannel, sslManager);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleOpWrite(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        MainSslManager sslManager = (MainSslManager) selectionKey.attachment();
        ConcurrentLinkedQueue<String> msgQueue = SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.getOrDefault(socketChannel, new ConcurrentLinkedQueue<>());
        String msg;
        while ((msg = msgQueue.peek()) != null) {
            if (doSend(socketChannel, sslManager, msg)) {
                msgQueue.poll();
            } else {
                break;
            }
        }
    }

    private void handleOpRead(SelectionKey selectionKey) {
        try {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            MainSslManager sslManager = (MainSslManager) selectionKey.attachment();

            // 主要用于存储因粘包拆包导致的不完整的消息
            StringBuilder dataBuilder = SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP.getOrDefault(socketChannel, new StringBuilder());

            // 循环读取数据到peerNetBuffer
            int bytesRead;
            while ((bytesRead = socketChannel.read(sslManager.peerNetBuffer)) > 0) {
                // 处理读取到到的peerNetBuffer：解密、粘包拆包处理、处理业务逻辑等
                boolean done = handlePeerNetBuffer(socketChannel, sslManager, dataBuilder);
                // 返回结果为null表示处理有问题则中断读取：连接关闭、处理异常等。
                if (done) {
                    return;
                }
            }

            // 客户端异常关闭连接
            if (bytesRead == -1) {
                handleEndOfStream(socketChannel, sslManager);
            } else if (bytesRead == 0) {
                // 如果peerNetBuffer数据没有处理完，则应缓存起来等下次可读时继续处理

                if (dataBuilder.length() > 0) {
                    // 缓存没有处理完成的部分，即客户端进行了拆包发送，服务端收到的不是一条完整的消息
                    // 需要等待下一次可读时，获取剩余数据进行处理
                    SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP.put(socketChannel, dataBuilder);
                } else {
                    // 是完整的消息，则可以移除该缓存
                    SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP.remove(socketChannel);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean handlePeerNetBuffer(SocketChannel socketChannel, MainSslManager sslManager, StringBuilder dataBuilder) {
        try {
            sslManager.peerNetBuffer.flip();
            // 循环解密peerNetBuffer，直到没有数据可以处理，因为通常unwrap每次处理16KB数据
            while (sslManager.peerNetBuffer.hasRemaining()) {
                // 准备好接受解密后的数据
                sslManager.peerAppBuffer.clear();
                // 解密数据
                SSLEngineResult unwrapResult = sslManager.sslEngine.unwrap(sslManager.peerNetBuffer, sslManager.peerAppBuffer);
                switch (unwrapResult.getStatus()) {
                    case OK:
                        // 解密成功则处理解密后的数据peerAppBuffer
                        handlePeerAppBuffer(socketChannel, sslManager, dataBuilder);
                        // 继续循环处理peerNetBuffer没有解密完的数据
                        break;
                    case BUFFER_OVERFLOW:
                        // 处理BUFFER_OVERFLOW的情况，即peerAppBuffer太小，需要扩容，扩容后继续循环解密
                        sslManager.enlargePeerAppBuffer();
                        break;
                    case BUFFER_UNDERFLOW:
                        // 处理BUFFER_UNDERFLOW的情况，即peerNetBuffer不足以解码，需要继续读取新的数据
                        // 如果空间不足则先扩容，结束peerNetBuffer解码循环，继续读取数据。
                        // 由于peerNetBuffer数据还没被读取，通过compact()以供后续继续写入
                        sslManager.handleUnWrapPeerNetBufferUnderflow();
                        return false;
                    case CLOSED:
                        // 对端关闭情况，则响应关闭连接
                        handleDisconnect(socketChannel, sslManager);
                        // 返回null，结束解码循环和读取循环
                        return true;
                    default:
                        // 不会发生
                        return true;
                }
            }
            // 循环解密正常处理完成后，返回清空后的peerNetBuffer，如果有新数据则继续读取到peerNetBuffer
            sslManager.peerNetBuffer.clear();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private void handlePeerAppBuffer(SocketChannel socketChannel, MainSslManager sslManager, StringBuilder dataBuilder) {
        sslManager.peerAppBuffer.flip();
        String msgNewPart = MsgEnDecoder.decodeMsg(sslManager.peerAppBuffer);
        dataBuilder.append(msgNewPart);
        // 粘包拆包处理，得到消息列表
        List<String> msgList = handleClientData(dataBuilder);
        // 处理消息
        handleClientMsgList(socketChannel, msgList);
    }

    private void handleClientMsgList(SocketChannel socketChannel, List<String> msgList) {
        if (null == msgList || msgList.isEmpty()) {
            return;
        }
        for (String msg : msgList) {
            handleClientMsg(socketChannel, msg);
        }
    }

    private void handleClientMsg(SocketChannel socketChannel, String msg) {
        try {
            switch (msg) {
                case "PING":
                    handleClientPing(socketChannel);
                    return;
                case "PONG":
                    handleClientPong(socketChannel);
                    return;
                default:
                    if (msg.startsWith("AUTH ")) {
                        auth(socketChannel, msg);
                    } else if (isAuthed(socketChannel)) {
                        System.out.printf(SSL_SERVER_LOG + "[Received Msg] [ClientId: %s] [Msg: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel), msg);
                        if (msg.startsWith("TO ")) {
                            String[] toClientIdMsg = msg.split(" ");
                            send(toClientIdMsg[1], "FROM " + toClientIdMsg[1] + " " + toClientIdMsg[2]);
                        }
                    } else {
                        System.out.println(SSL_SERVER_LOG + "[Not Auth] " + "[ClientAddr: " + socketChannel.getRemoteAddress() + "]");
                        closeConnection(socketChannel, SOCKET_CHANNEL_SSL_MANAGER_MAP.get(socketChannel));
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClientPong(SocketChannel socketChannel) {
        System.out.printf(SSL_SERVER_LOG + "[Received Pong] [ClientId: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel));
    }

    private void handleClientPing(SocketChannel socketChannel) {
        System.out.printf(SSL_SERVER_LOG + "[Received Ping] [ClientId: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel));
        pong(socketChannel);
        System.out.printf(SSL_SERVER_LOG + "[Head Count] [SocketChannelCount: %d; ClientIdCount: %d]\n",
                SOCKET_CHANNEL_CLIENT_ID_MAP.size(),
                CLIENT_ID_SOCKET_CHANNEL_MAP.size());
    }

    private void ping(SocketChannel socketChannel) {
        send(socketChannel, "PING");
    }

    private void pong(SocketChannel socketChannel) {
        send(socketChannel, "PONG");
    }


    /**
     * 使用分隔符来处理粘包拆包问题，返回消息列表
     *
     * @param dataBuilder
     * @return
     */
    private List<String> handleClientData(StringBuilder dataBuilder) {
        List<String> msgList = new ArrayList<>();
        int delimiterIndex;
        while ((delimiterIndex = dataBuilder.indexOf(DELIMITER)) != -1) {
            String msg = dataBuilder.substring(0, delimiterIndex);
            msgList.add(msg);
            dataBuilder.delete(0, delimiterIndex + DELIMITER.length());
        }
        return msgList;
    }

    private boolean isAuthed(SocketChannel socketChannel) {
        return SOCKET_CHANNEL_CLIENT_ID_MAP.containsKey(socketChannel);
    }

    private boolean auth(SocketChannel socketChannel, String authData) {
        try {
            if (!authData.startsWith("AUTH ")) {
                System.out.printf(SSL_SERVER_LOG + "[ClientAuthFailed] [NotAuthData] [SocketChannelAddr: %s]\n", socketChannel.getRemoteAddress());
                return false;
            }
            String[] parts = authData.split(" ", 2);
            if (parts.length != 2) {
                send(socketChannel, "AUTH_FAILED:INVALID_AUTH_DATA");
                System.out.printf(SSL_SERVER_LOG + "[ClientAuthFailed] [InvalidAuthData] [SocketChannelAddr: %s]\n", socketChannel.getRemoteAddress());
                return false;
            }
            String[] usernameAndPwd = parts[1].split("=");
            if (usernameAndPwd.length != 2) {
                send(socketChannel, "AUTH_FAILED:USERNAME_PWD_NOT_MATCH");
                System.out.printf(SSL_SERVER_LOG + "[ClientAuthFailed] [UsernamePwdNotMatch] [SocketChannelAddr: %s]\n", socketChannel.getRemoteAddress());
                return false;
            }
            String username = usernameAndPwd[0];
            String password = usernameAndPwd[1];
            // 这里进行实际的用户名密码验证
            if (mockAuthenticate(username, password)) {
                SOCKET_CHANNEL_CLIENT_ID_MAP.put(socketChannel, username);
                CLIENT_ID_SOCKET_CHANNEL_MAP.put(username, socketChannel);
                send(socketChannel, "AUTH_SUCCESS:" + username);
                System.out.printf(SSL_SERVER_LOG + "[ClientAuthSuccess] [ClientId: %s]\n", username);
                return true;
            } else {
                send(socketChannel, "AUTH_FAILED:USERNAME_PWD_NOT_MATCH");
                System.out.printf(SSL_SERVER_LOG + "[ClientAuthFailed] [UsernamePwdNotMatch] [SocketChannelAddr: %s]\n", socketChannel.getRemoteAddress());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean mockAuthenticate(String username, String password) {
        return username.equals(password);
    }

    private void handleEndOfStream(SocketChannel socketChannel, MainSslManager sslManager) {
        // 客户端异常关闭连接的情况下，直接关闭入站
        String clientId = SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel);
        try {
            sslManager.sslEngine.closeInbound();
        } catch (Exception e) {
            System.out.printf(SSL_SERVER_LOG + "[EndOfStream] [Client UnExcepted Disconnected][ClientId: %s]\n",
                    clientId);
        }
        closeConnection(socketChannel, sslManager);
        clearData(socketChannel, clientId);
        System.out.printf(SSL_SERVER_LOG + "[ClientUnExceptedDisconnected] [ClientId: %s]\n", clientId);
    }

    private void handleDisconnect(SocketChannel socketChannel, MainSslManager sslManager) {
        closeConnection(socketChannel, sslManager);
        String clientId = SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel);
        clearData(socketChannel, clientId);
        System.out.printf(SSL_SERVER_LOG + "[ClientDisconnected] [ClientId: %s]\n", clientId);
    }


    private void closeConnection(SocketChannel socketChannel, MainSslManager sslManager) {
        try {
            boolean sslClosed = sslManager.closeHandshake(socketChannel, sslManager.sslEngine);
            if (sslClosed) {
                System.out.println(SSL_SERVER_LOG + "[CloseHandshake Succeed] [SocketChannelAddr: " + socketChannel.getRemoteAddress() + "]");
            }
            socketChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearData(SocketChannel socketChannel, String clientId) {
        CLIENT_ID_SOCKET_CHANNEL_MAP.remove(clientId);
        SOCKET_CHANNEL_CLIENT_ID_MAP.remove(socketChannel);
        SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.remove(socketChannel);
        SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP.remove(socketChannel);
        SOCKET_CHANNEL_SSL_MANAGER_MAP.remove(socketChannel);
    }

    public void closeClient(String clientId) {
        SocketChannel socketChannel = CLIENT_ID_SOCKET_CHANNEL_MAP.get(clientId);
        if (null == socketChannel) {
            System.out.println(SSL_SERVER_LOG + "[CloseClient] [InvalidClientId: " + clientId + "]");
            return;
        }
        closeConnection(socketChannel, SOCKET_CHANNEL_SSL_MANAGER_MAP.get(socketChannel));
        clearData(socketChannel, clientId);
        System.out.println(SSL_SERVER_LOG + "[CloseClient] [ClientId: " + clientId + "]");
    }

    public void closeAllClient() {
        for (Map.Entry<SocketChannel, MainSslManager> socketChannelSSLEngineEntry : SOCKET_CHANNEL_SSL_MANAGER_MAP.entrySet()) {
            closeConnection(socketChannelSSLEngineEntry.getKey(), socketChannelSSLEngineEntry.getValue());
        }
        SOCKET_CHANNEL_CLIENT_ID_MAP.clear();
        CLIENT_ID_SOCKET_CHANNEL_MAP.clear();
        SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.clear();
        SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP.clear();
    }

    public boolean send(String clientId, String msg) {
        SocketChannel socketChannel = CLIENT_ID_SOCKET_CHANNEL_MAP.get(clientId);
        if (null == socketChannel) {
            System.out.println(SSL_SERVER_LOG + "[Send Msg Failed] [InvalidClientId: " + clientId + "] [Msg: " + msg + "]");
            return false;
        }
        return send(socketChannel, msg);
    }

    public boolean send(SocketChannel socketChannel, String msg) {
        return send(socketChannel, SOCKET_CHANNEL_SSL_MANAGER_MAP.get(socketChannel), msg);
    }

    private boolean send(SocketChannel socketChannel, MainSslManager sslManager, String msg) {
        if (doSend(socketChannel, sslManager, msg)) {
            return true;
        }
        if (!SOCKET_CHANNEL_CLIENT_ID_MAP.containsKey(socketChannel)) {
            System.out.println(SSL_SERVER_LOG + "[Send Msg Failed] [InvalidSocketChannel] [Msg: " + msg + "]");
            return false;
        }
        ConcurrentLinkedQueue<String> msgQueue = SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.getOrDefault(socketChannel, new ConcurrentLinkedQueue<>());
        msgQueue.offer(msg);
        SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.put(socketChannel, msgQueue);
        System.out.printf(SSL_SERVER_LOG + "[Send Msg Queued] [ClientId: %s] [Msg: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel), msg);
        return true;
    }

    private boolean doSend(SocketChannel socketChannel, MainSslManager sslManager, String msg) {
        if (write(socketChannel, sslManager, msg)) {
            System.out.printf(SSL_SERVER_LOG + "[Send Msg Succeed] [ClientId: %s] [Msg: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel), msg);
            return true;
        }
        return false;
    }

    private boolean write(SocketChannel socketChannel, MainSslManager sslManager, String msg) {
        if (sslManager == null) {
            return false;
        }
        MainSslManager.WriteResult writeResult = sslManager.write(socketChannel, msg);
        switch (writeResult) {
            case DONE:
                return true;
            case ERROR:
                return false;
            case CLOSED:
                handleDisconnect(socketChannel, sslManager);
                return false;
            default:
                return false;
        }
    }
}
