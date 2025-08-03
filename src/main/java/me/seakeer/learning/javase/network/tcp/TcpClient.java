package me.seakeer.learning.javase.network.tcp;

import me.seakeer.learning.javase.network.MsgEnDecoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * TcpClient;
 * TCP协议客户端，支持消息发送，消息接收，心跳保活，断线重连
 *
 * @author Seakeer;
 * @date 2024/10/29;
 */
public class TcpClient {

    public static final String TCP_CLIENT_LOG = "[TcpClient] ";

    public static final String CMD_LIST = "[CmdList: START, RESTART, STOP, SHUTDOWN, TO $USERNAME $MSG]";

    private final String serverHostname;

    private final int serverPort;

    private volatile SocketChannel socketChannel;

    private volatile Selector selector;

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

    /**
     * 定时保活线程池
     */
    private volatile ScheduledExecutorService keepAliveThreadPool;

    /**
     * 主要用于存储因粘包拆包导致的不完整的消息
     */
    public static final StringBuilder DATA_BUILDER = new StringBuilder();

    private static final ConcurrentLinkedQueue<String> SENDING_MSG_QUEUE = new ConcurrentLinkedQueue<>();

    public TcpClient(String serverHostname, int serverPort) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) {
        TcpClient tcpClient = new TcpClient("127.0.0.1", 9090);
        cmd(tcpClient);
    }

    /**
     * 控制台命令行交互
     * 控制客户端启动，停止，重启，退出，发送消息等
     */
    private static void cmd(TcpClient tcpClient) {
        System.out.println(TCP_CLIENT_LOG + "[Client Ready] [Please Input Cmd] " + CMD_LIST);
        System.out.println(TCP_CLIENT_LOG + "[Auto Start] [TcpClient will auto start in 3 seconds]");
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(tcpClient::start, 3, TimeUnit.SECONDS);
        scheduledExecutorService.shutdown();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                String cmd = scanner.nextLine();
                switch (cmd) {
                    case "START":
                        new Thread(tcpClient::start).start();
                        break;
                    case "STOP":
                        tcpClient.stop();
                        break;
                    case "RESTART":
                        new Thread(tcpClient::restart).start();
                        break;
                    case "SHUTDOWN":
                        tcpClient.stop();
                        return;
                    default:
                        if (cmd.contains(TcpServer.DELIMITER)) {
                            System.out.println(TCP_CLIENT_LOG + "[ContainsDelimiter]");
                        }
                        tcpClient.send(cmd);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean connect() {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            boolean connected = socketChannel.connect(new InetSocketAddress(serverHostname, serverPort));
            Selector selector = Selector.open();
            if (connected) {
                System.out.printf(TCP_CLIENT_LOG + "[Connected] [Server: %s:%d]\n", serverHostname, serverPort);
                socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                onConnected();
            } else {
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
            this.selector = selector;
            this.socketChannel = socketChannel;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return reconnect();
        }
    }

    public void start() {
        if (isRunning()) {
            System.out.println(TCP_CLIENT_LOG + "[Already Running]");
            return;
        }
        System.out.println(TCP_CLIENT_LOG + "[Starting]");
        init();
        connect();
        run();
    }


    public void stop() {
        try {
            System.out.println(TCP_CLIENT_LOG + "[Stopping]");
            status = -1;
            keepAliveThreadPool.shutdownNow();
            SENDING_MSG_QUEUE.clear();
            if (null != selector) {
                selector.close();
                selector = null;
            }
            if (null != socketChannel) {
                socketChannel.close();
                socketChannel = null;
            }
            System.out.println(TCP_CLIENT_LOG + "[Stopped]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restart() {
        System.out.println(TCP_CLIENT_LOG + "[Restarting]");
        stop();
        start();
    }

    private void init() {
        keepAliveThreadPool = Executors.newSingleThreadScheduledExecutor();
        reconnectCount = 0;
        pongLostCount = 0;
        status = 0;
    }

    private void onConnected() {
        reconnectCount = 0;
        pongLostCount = 0;
        mockSendAuthData();
    }

    private void scheduleKeepAlive() {
        this.keepAliveThreadPool.scheduleAtFixedRate(this::keepAlive, 10, 60, TimeUnit.SECONDS);
    }

    private void run() {
        System.out.println(TCP_CLIENT_LOG + "[Running]");
        status = 1;
        scheduleKeepAlive();
        while (isRunning()) {
            try {
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if (!selectionKey.isValid()) {
                        continue;
                    }
                    SocketChannel sc = (SocketChannel) selectionKey.channel();
                    if (selectionKey.isConnectable()) {
                        if (sc.finishConnect()) {
                            System.out.printf(TCP_CLIENT_LOG + "[Connected] [Server: %s:%d]\n", serverHostname, serverPort);
                            reconnectCount = 0;
                            sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            mockSendAuthData();
                        } else {
                            reconnect();
                        }
                    }
                    if (selectionKey.isValid() && selectionKey.isReadable()) {
                        handleOpRead();
                    }
                    if (selectionKey.isValid() && selectionKey.isWritable()) {
                        handleOpWrite();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                reconnect();
            }
        }
    }

    private boolean isRunning() {
        return status == 1;
    }

    private boolean isStopped() {
        return status == -1;
    }

    private void mockSendAuthData() {
        String str = "Seakeer" + new Random().nextInt(100);
        send("AUTH " + str + "=" + str);
    }

    private void handleOpWrite() {
        String msg;
        while ((msg = SENDING_MSG_QUEUE.peek()) != null) {
            if (write(msg)) {
                SENDING_MSG_QUEUE.poll();
            } else {
                break;
            }
        }
    }

    private void handleOpRead() {
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            int bytesRead;
            while ((bytesRead = socketChannel.read(buffer)) > 0) {
                buffer.flip();
                String msgNewPart = MsgEnDecoder.decodeMsg(buffer);
                DATA_BUILDER.append(msgNewPart);
                // 粘包拆包处理，得到消息列表
                List<String> msgList = handleServerData(DATA_BUILDER);
                handleServerMsgList(msgList);
            }
            if (bytesRead == -1) {
                System.out.println(TCP_CLIENT_LOG + "[Disconnected] [Will Try Reconnect]");
                reconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleServerMsgList(List<String> msgList) {
        if (null == msgList || msgList.isEmpty()) {
            return;
        }
        for (String msg : msgList) {
            handleServerMsg(msg);
        }
    }

    private void handleServerMsg(String msg) {
        try {
            switch (msg) {
                case "PING":
                    handleServerPing();
                    return;
                case "PONG":
                    handleServerPong();
                    return;
                default:
                    if (msg.startsWith("FROM ")) {
                        handleFromOtherClientMsg(msg);
                    } else {
                        System.out.printf(TCP_CLIENT_LOG + "[Received Server Msg] [Msg: %s]\n", msg);
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFromOtherClientMsg(String otherClientMsg) {
        String[] fromMsgArr = otherClientMsg.split(" ");
        if (fromMsgArr.length != 3) {
            System.out.printf(TCP_CLIENT_LOG + "[Received Msg] [Invalid Msg] [Msg: %s]\n", otherClientMsg);
            return;
        }
        System.out.printf(TCP_CLIENT_LOG + "[Received Msg] [From: %s] [Msg: %s]\n", fromMsgArr[1], fromMsgArr[2]);
    }

    private List<String> handleServerData(StringBuilder dataBuilder) {
        List<String> msgList = new ArrayList<>();
        int delimiterIndex;
        while ((delimiterIndex = dataBuilder.indexOf(TcpServer.DELIMITER)) != -1) {
            String msg = dataBuilder.substring(0, delimiterIndex);
            msgList.add(msg);
            dataBuilder.delete(0, delimiterIndex + TcpServer.DELIMITER.length());
        }
        return msgList;
    }


    public boolean send(String msg) {
        if (null == socketChannel) {
            return false;
        }
        if (write(msg)) {
            return true;
        }
        SENDING_MSG_QUEUE.offer(msg);
        return true;
    }

    private boolean write(String msg) {
        ByteBuffer byteBuffer = MsgEnDecoder.encodeMsg(msg + TcpServer.DELIMITER);
        try {
            while (byteBuffer.hasRemaining()) {
                int writeBytes = socketChannel.write(byteBuffer);
                if (writeBytes <= 0) {
                    return false;
                }
            }
            System.out.println(TCP_CLIENT_LOG + "[Send Msg] [Msg: " + msg + "]");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean reconnect() {
        try {
            TimeUnit.SECONDS.sleep((reconnectCount + 1) * 3L);
            if (isStopped()) {
                return false;
            }
            if (reconnectCount >= RECONNECT_MAX_COUNT) {
                System.out.println(TCP_CLIENT_LOG + "[Reconnect Stop] [ReconnectMaxCount: " + RECONNECT_MAX_COUNT + "]");
                stop();
                return false;
            }
            reconnectCount++;
            socketChannel.close();
            selector.close();
            System.out.println(TCP_CLIENT_LOG + "[Reconnecting] [ReconnectCount: " + reconnectCount + "]");
            return connect();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void keepAlive() {
        if (this.pongLostCount >= PONG_LOST_MAX_COUNT) {
            System.out.println(TCP_CLIENT_LOG + "[Pong Lost Max Count] [Will Try Reconnect] [MaxCount: " + PONG_LOST_MAX_COUNT + "]");
            reconnect();
        } else {
            ping();
            pongLostCount += 1;
        }
    }

    public void ping() {
        send("PING");
    }

    public void pong() {
        send("PONG");
    }

    public void handleServerPong() {
        System.out.println(TCP_CLIENT_LOG + "[Received Server Pong]");
        pongLostCount = 0;
    }

    public void handleServerPing() {
        System.out.println(TCP_CLIENT_LOG + "[Received Server Ping]");
        pong();
    }

}
