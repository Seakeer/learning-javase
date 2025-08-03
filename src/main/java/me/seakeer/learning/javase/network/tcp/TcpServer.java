package me.seakeer.learning.javase.network.tcp;

import me.seakeer.learning.javase.network.MsgEnDecoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;

/**
 * TcpServer;
 *
 * @author Seakeer;
 * @date 2024/10/29;
 */
public class TcpServer {

    public static final String DELIMITER = "v^";

    public static final String TCP_SERVER_LOG = "[TcpServer] ";
    public static final String CMD_LIST = "[START, RESTART, STOP, SHUTDOWN, TO $CLIENT_ID $MSG]";

    private final int port;

    private volatile ServerSocketChannel serverSocketChannel;

    private volatile Selector selector;

    private volatile boolean running = false;

    private final Map<SocketChannel, String> SOCKET_CHANNEL_CLIENT_ID_MAP = new ConcurrentHashMap<>();
    private final Map<String, SocketChannel> CLIENT_ID_SOCKET_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 发送消息时，如果Channel不可写，则缓存消息到待发送消息队列
     */
    private final Map<SocketChannel, ConcurrentLinkedQueue<String>> SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP = new ConcurrentHashMap<>();
    private final Map<SocketChannel, StringBuilder> SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP = new ConcurrentHashMap<>();

    public TcpServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        TcpServer tcpServer = new TcpServer(9090);
        cmd(tcpServer);
    }

    private static void cmd(TcpServer tcpServer) {
        System.out.println(TCP_SERVER_LOG + "[Server Ready] [Please Input Cmd] " + CMD_LIST);
        System.out.println(TCP_SERVER_LOG + "[AutoStart] [Server will auto start in 3 seconds]");
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(tcpServer::start, 3, TimeUnit.SECONDS);
        scheduledExecutorService.shutdown();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                String cmd = scanner.nextLine();
                switch (cmd) {
                    case "START":
                        new Thread(tcpServer::start).start();
                        break;
                    case "STOP":
                        tcpServer.stop();
                        break;
                    case "RESTART":
                        new Thread(tcpServer::restart).start();
                        break;
                    case "SHUTDOWN":
                        tcpServer.stop();
                        return;
                    default:
                        if (cmd.startsWith("TO ")) {
                            if (cmd.contains(DELIMITER)) {
                                System.out.println(TCP_SERVER_LOG + "[ContainsDelimiter]");
                            }
                            String[] msgParts = cmd.split(" ", 3);
                            if (msgParts.length < 3) {
                                System.out.printf(TCP_SERVER_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                                continue;
                            }
                            tcpServer.send(msgParts[1], msgParts[2]);
                        } else {
                            System.out.printf(TCP_SERVER_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        if (running) {
            System.out.println(TCP_SERVER_LOG + "[AlreadyRunning]");
            return;
        }
        System.out.println(TCP_SERVER_LOG + "[Starting]");
        init();
        run();
    }

    public void stop() {
        try {
            System.out.println(TCP_SERVER_LOG + "[Stopping]");
            running = false;
            closeAllClient();
            SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.clear();
            SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP.clear();
            if (null != serverSocketChannel) {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
            if (null != selector) {
                selector.close();
                selector = null;
            }
            System.out.println(TCP_SERVER_LOG + "[Stopped]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restart() {
        System.out.println(TCP_SERVER_LOG + "[Restarting]");
        stop();
        start();
    }

    private void init() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(port));
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run() {
        running = true;
        System.out.println(TCP_SERVER_LOG + "[Running]");
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
            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            System.out.println(TCP_SERVER_LOG + "[Accepted Client] " + "[ClientAddr: " + socketChannel.getRemoteAddress() + "]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleOpWrite(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ConcurrentLinkedQueue<String> msgQueue = SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.getOrDefault(socketChannel, new ConcurrentLinkedQueue<>());
        String msg;
        while ((msg = msgQueue.peek()) != null) {
            if (send(socketChannel, msg)) {
                msgQueue.poll();
                System.out.printf(TCP_SERVER_LOG + "[Send Msg] [ClientId: %s] [Msg: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel), msg);
            } else {
                break;
            }
        }
    }

    private void handleOpRead(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        // 主要用于存储因粘包拆包导致的不完整的消息
        StringBuilder dataBuilder = SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP.getOrDefault(socketChannel, new StringBuilder());
        int bytesRead;
        while ((bytesRead = socketChannel.read(buffer)) > 0) {
            buffer.flip();

            // 将读取的字节进行解码，得到解码后的数据
            String msgNewPart = MsgEnDecoder.decodeMsg(buffer);
            dataBuilder.append(msgNewPart);

            // 粘包拆包处理，得到消息列表
            List<String> msgList = handleClientData(dataBuilder);

            // 处理消息
            handleClientMsgList(socketChannel, msgList);
        }
        if (bytesRead == -1) {
            handleDisconnect(socketChannel);
        } else if (bytesRead == 0) {
            if (dataBuilder.length() > 0) {
                // 缓存没有处理完成的部分，即客户端进行了拆包发送，服务端收到的不是一条完整的消息
                // 需要等待下一次可读时，获取剩余数据进行处理
                SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP.put(socketChannel, dataBuilder);
            } else {
                // 是完整的消息，则可以移除该缓存
                SOCKET_CHANNEL_RECEIVING_MSG_PART_MAP.remove(socketChannel);
            }
        }
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
                        System.out.printf(TCP_SERVER_LOG + "[Received Msg] [ClientId: %s] [Msg: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel), msg);
                        if (msg.startsWith("TO ")) {
                            String[] toClientIdMsg = msg.split(" ");
                            send(toClientIdMsg[1], "FROM " + toClientIdMsg[1] + " " + toClientIdMsg[2]);
                        }
                    } else {
                        System.out.println(TCP_SERVER_LOG + "[Not Auth] " + "[ClientAddr: " + socketChannel.getRemoteAddress() + "]");
                        socketChannel.close();
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClientPong(SocketChannel socketChannel) {
        System.out.printf(TCP_SERVER_LOG + "[Received Pong] [ClientId: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel));
    }

    private void handleClientPing(SocketChannel socketChannel) {
        System.out.printf(TCP_SERVER_LOG + "[Received Ping] [ClientId: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel));
        pong(socketChannel);
        System.out.printf(TCP_SERVER_LOG + "[Head Count] [SocketChannelCount: %d; ClientIdCount: %d]\n",
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
                System.out.printf(TCP_SERVER_LOG + "[ClientAuthFailed] [NotAuthData] [SocketChannelAddr: %s]\n", socketChannel.getRemoteAddress());
                return false;
            }
            String[] parts = authData.split(" ", 2);
            if (parts.length != 2) {
                send(socketChannel, "AUTH_FAILED:INVALID_AUTH_DATA");
                System.out.printf(TCP_SERVER_LOG + "[ClientAuthFailed] [InvalidAuthData] [SocketChannelAddr: %s]\n", socketChannel.getRemoteAddress());
                return false;
            }
            String[] usernameAndPwd = parts[1].split("=");
            if (usernameAndPwd.length != 2) {
                send(socketChannel, "AUTH_FAILED:USERNAME_PWD_NOT_MATCH");
                System.out.printf(TCP_SERVER_LOG + "[ClientAuthFailed] [UsernamePwdNotMatch] [SocketChannelAddr: %s]\n", socketChannel.getRemoteAddress());
                return false;
            }
            String username = usernameAndPwd[0];
            String password = usernameAndPwd[1];
            // 这里进行实际的用户名密码验证
            if (mockAuthenticate(username, password)) {
                SOCKET_CHANNEL_CLIENT_ID_MAP.put(socketChannel, username);
                CLIENT_ID_SOCKET_CHANNEL_MAP.put(username, socketChannel);
                send(socketChannel, "AUTH_SUCCESS:" + username);
                System.out.printf(TCP_SERVER_LOG + "[ClientAuthSuccess] [ClientId: %s]\n", username);
                return true;
            } else {
                send(socketChannel, "AUTH_FAILED:USERNAME_PWD_NOT_MATCH");
                System.out.printf(TCP_SERVER_LOG + "[ClientAuthFailed] [UsernamePwdNotMatch] [SocketChannelAddr: %s]\n", socketChannel.getRemoteAddress());
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

    private void handleDisconnect(SocketChannel socketChannel) throws IOException {
        String clientId = SOCKET_CHANNEL_CLIENT_ID_MAP.remove(socketChannel);
        if (clientId != null) {
            CLIENT_ID_SOCKET_CHANNEL_MAP.remove(clientId);
        }
        SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.remove(socketChannel);
        socketChannel.close();
        System.out.printf(TCP_SERVER_LOG + "[ClientDisconnected] [ClientId: %s]\n", clientId);
    }

    public void closeAllClient() {
        for (SocketChannel socketChannel : SOCKET_CHANNEL_CLIENT_ID_MAP.keySet()) {
            try {
                socketChannel.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        SOCKET_CHANNEL_CLIENT_ID_MAP.clear();
        CLIENT_ID_SOCKET_CHANNEL_MAP.clear();
    }

    public boolean send(String clientId, String msg) {
        SocketChannel socketChannel = CLIENT_ID_SOCKET_CHANNEL_MAP.get(clientId);
        if (null == socketChannel) {
            System.out.println(TCP_SERVER_LOG + "[Send Msg Failed] [InvalidClientId: " + clientId + "] [Msg: " + msg + "]");
            return false;
        }
        return send(socketChannel, msg);
    }

    private boolean send(SocketChannel socketChannel, String msg) {
        if (write(socketChannel, msg)) {
            System.out.printf(TCP_SERVER_LOG + "[Send Msg Succeed] [ClientId: %s] [Msg: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel), msg);
            return true;
        }
        ConcurrentLinkedQueue<String> msgQueue = SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.getOrDefault(socketChannel, new ConcurrentLinkedQueue<>());
        msgQueue.offer(msg);
        SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.put(socketChannel, msgQueue);
        System.out.printf(TCP_SERVER_LOG + "[Send Msg Queued] [ClientId: %s] [Msg: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel), msg);
        return true;
    }

    private boolean write(SocketChannel socketChannel, String msg) {
        ByteBuffer byteBuffer = MsgEnDecoder.encodeMsg(msg + DELIMITER);
        try {
            while (byteBuffer.hasRemaining()) {
                int writeBytes = socketChannel.write(byteBuffer);
                if (writeBytes <= 0) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
