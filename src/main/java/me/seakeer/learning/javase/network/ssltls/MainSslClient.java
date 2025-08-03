package me.seakeer.learning.javase.network.ssltls;

import me.seakeer.learning.javase.network.MsgEnDecoder;
import me.seakeer.learning.javase.network.tcp.TcpServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
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
 * SslClient;
 * TCP协议客户端，支持消息发送，消息接收，心跳保活，断线重连
 *
 * @author Seakeer;
 * @date 2024/10/29;
 */
public class MainSslClient {

    public static final String SSL_CLIENT_LOG = "[SslClient] ";

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

    private MainSslManager sslManager;


    public MainSslClient(String serverHostname, int serverPort) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) {
        MainSslClient sslClient = new MainSslClient("127.0.0.1", 9090);
        cmd(sslClient);
    }

    /**
     * 控制台命令行交互
     * 控制客户端启动，停止，重启，退出，发送消息等
     */
    private static void cmd(MainSslClient sslClient) {
        System.out.println(SSL_CLIENT_LOG + "[Client Ready] [Please Input Cmd] " + CMD_LIST);
        System.out.println(SSL_CLIENT_LOG + "[Auto Start] [SslClient will auto start in 3 seconds]");
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(sslClient::start, 3, TimeUnit.SECONDS);
        scheduledExecutorService.shutdown();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                String cmd = scanner.nextLine();
                switch (cmd) {
                    case "START":
                        new Thread(sslClient::start).start();
                        break;
                    case "STOP":
                        sslClient.stop();
                        break;
                    case "RESTART":
                        new Thread(sslClient::restart).start();
                        break;
                    case "SHUTDOWN":
                        sslClient.stop();
                        System.out.println(SSL_CLIENT_LOG + "[Shutdown]");
                        return;
                    default:
                        if (cmd.contains(TcpServer.DELIMITER)) {
                            System.out.println(SSL_CLIENT_LOG + "[ContainsDelimiter]");
                        }
                        sslClient.send(cmd);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean connect() {
        try {
            // 创建 SocketChannel
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            selector = Selector.open();
            boolean connected = socketChannel.connect(new InetSocketAddress(serverHostname, serverPort));
            if (connected) {
                return handshake();
            } else {
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return reconnect();
        }
    }

    private boolean handshake() throws Exception {
        // 初始化 SSLContext
        SSLContext sslContext = MainSslManager.crtSslContext("Client.jks", "seakeer", "SeakeerCaCert.jks", "seakeer");
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(true);
        sslManager = new MainSslManager(sslEngine);
        if (sslManager.openHandshake(socketChannel)) {
            System.out.printf(SSL_CLIENT_LOG + "[Connected] [Server: %s:%d]\n", serverHostname, serverPort);
            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            onConnected();
            return true;
        }
        return false;
    }

    public void start() {
        if (isRunning()) {
            System.out.println(SSL_CLIENT_LOG + "[Already Running]");
            return;
        }
        System.out.println(SSL_CLIENT_LOG + "[Starting]");
        init();
        if (connect()) {
            run();
        }

    }

    public void stop() {
        try {
            System.out.println(SSL_CLIENT_LOG + "[Stopping]");
            status = -1;
            keepAliveThreadPool.shutdownNow();
            SENDING_MSG_QUEUE.clear();
            disconnect();
            System.out.println(SSL_CLIENT_LOG + "[Stopped]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restart() {
        System.out.println(SSL_CLIENT_LOG + "[Restarting]");
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
        System.out.println(SSL_CLIENT_LOG + "[Running]");
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
                            if (handshake()) {
                                reconnectCount = 0;
                                sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            }
                        } else {
                            reconnect();
                        }
                    }
                    if (selectionKey.isValid() && selectionKey.isReadable()) {
                        handleOpRead(selectionKey);
                    }
                    if (selectionKey.isValid() && selectionKey.isWritable()) {
                        handleOpWrite(selectionKey);
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

    private void handleOpWrite(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        String msg;
        while ((msg = SENDING_MSG_QUEUE.peek()) != null) {
            if (doSend(socketChannel, msg)) {
                SENDING_MSG_QUEUE.poll();
            } else {
                break;
            }
        }
    }

    private void handleOpRead(SelectionKey selectionKey) {
        try {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            // 循环读取数据到peerNetBuffer
            int bytesRead;
            while ((bytesRead = socketChannel.read(sslManager.peerNetBuffer)) > 0) {
                // 处理读取到到的peerNetBuffer：解密、粘包拆包处理、处理业务逻辑等
                boolean done = handlePeerNetBuffer();
                // 返回结果为null表示处理有问题则中断读取：连接关闭、处理异常等。
                if (done) {
                    return;
                }
            }
            // 客户端异常关闭连接
            if (bytesRead == -1) {
                handleEndOfStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean handlePeerNetBuffer() {
        try {
            sslManager.peerNetBuffer.flip();
            // 循环解密peerNetBuffer，直到没有数据可以处理，因为通常unwrap每次处理16KB数据
            while (sslManager.peerNetBuffer.hasRemaining()) {
                // 准备好接受解密后的数据
                // sslManager.peerAppBuffer.clear();
                // 解密数据
                SSLEngineResult unwrapResult = sslManager.sslEngine.unwrap(sslManager.peerNetBuffer, sslManager.peerAppBuffer);
                switch (unwrapResult.getStatus()) {
                    case OK:
                        // 解密成功则处理解密后的数据peerAppBuffer
                        handlePeerAppBuffer();
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
                        handleDisconnect();
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

    private void handlePeerAppBuffer() {
        sslManager.peerAppBuffer.flip();
        String msgNewPart = MsgEnDecoder.decodeMsg(sslManager.peerAppBuffer);
        DATA_BUILDER.append(msgNewPart);
        // 粘包拆包处理，得到消息列表
        List<String> msgList = handleServerData();
        // 处理消息
        handleServerMsgList(msgList);
    }

    private ByteBuffer sslUnwrapBuffer(SSLEngine sslEngine, ByteBuffer netInBuffer, ByteBuffer appInBuffer) {
        try {
            // 解密数据
            SSLEngineResult unwrapResult = sslEngine.unwrap(netInBuffer, appInBuffer);
            switch (unwrapResult.getStatus()) {
                case OK:
                    // 解密成功后清空netInBuffer以复用
                    netInBuffer.clear();
                    return appInBuffer;
                case BUFFER_UNDERFLOW:
                    return readAndReUnwrapBuffer(sslEngine, netInBuffer, appInBuffer);
                case BUFFER_OVERFLOW:
                    appInBuffer = enlargeBuffer(appInBuffer, sslEngine.getSession().getApplicationBufferSize());
                    return sslUnwrapBuffer(sslEngine, netInBuffer, appInBuffer);
                case CLOSED:
                    handleDisconnect();
                    return null;
                default:
                    return null;
            }
        } catch (SSLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ByteBuffer readAndReUnwrapBuffer(SSLEngine sslEngine, ByteBuffer netInBuffer, ByteBuffer appInBuffer) {
        try {
            netInBuffer = SslHandler.handleUnWrapPeerNetBufferUnderflow(sslEngine, netInBuffer);
            int bytesRead;
            while ((bytesRead = socketChannel.read(netInBuffer)) > 0) {
                netInBuffer.flip();
                return sslUnwrapBuffer(sslEngine, netInBuffer, appInBuffer);
            }
            if (bytesRead == -1) {
                System.out.println(SSL_CLIENT_LOG + "[Disconnected] [EndOfSteam] [Will Try Reconnect]");
                handleEndOfStream();
                return null;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
                        System.out.printf(SSL_CLIENT_LOG + "[Received Server Msg] [Msg: %s]\n", msg);
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFromOtherClientMsg(String otherClientMsg) {
        String[] fromMsgArr = otherClientMsg.split(" ");
        if (fromMsgArr.length != 3) {
            System.out.printf(SSL_CLIENT_LOG + "[Received Msg] [Invalid Msg] [Msg: %s]\n", otherClientMsg);
            return;
        }
        System.out.printf(SSL_CLIENT_LOG + "[Received Msg] [From: %s] [Msg: %s]\n", fromMsgArr[1], fromMsgArr[2]);
    }

    private List<String> handleServerData() {
        List<String> msgList = new ArrayList<>();
        int delimiterIndex;
        while ((delimiterIndex = DATA_BUILDER.indexOf(TcpServer.DELIMITER)) != -1) {
            String msg = DATA_BUILDER.substring(0, delimiterIndex);
            msgList.add(msg);
            DATA_BUILDER.delete(0, delimiterIndex + TcpServer.DELIMITER.length());
        }
        return msgList;
    }

    public boolean send(String msg) {
        if (null == socketChannel || null == sslManager) {
            return false;
        }
        if (doSend(socketChannel, msg)) {
            return true;
        }
        SENDING_MSG_QUEUE.offer(msg);
        System.out.println(SSL_CLIENT_LOG + "[Send Msg Queued] [Msg: " + msg + "]");
        return true;
    }

    private boolean doSend(SocketChannel socketChannel, String msg) {
        if (write(socketChannel, msg)) {
            System.out.println(SSL_CLIENT_LOG + "[Send Msg Succeed] [Msg: " + msg + "]");
            return true;
        }
        return false;
    }


    private boolean write(SocketChannel socketChannel, String msg) {
        if (sslManager == null) {
            return false;
        }
        try {
            while (true) {
                int msgBytes = MsgEnDecoder.encodeMsg(msg + SslServer.DELIMITER, sslManager.myAppBuffer);
                if (msgBytes > 0) {
                    sslManager.enlargeMyAppBuffer(msgBytes);
                } else {
                    break;
                }
            }
            while (sslManager.myAppBuffer.hasRemaining()) {
                // 加密数据
                SSLEngineResult wrapResult = sslManager.sslEngine.wrap(sslManager.myAppBuffer, sslManager.myNetBuffer);
                switch (wrapResult.getStatus()) {
                    case OK:
                        sslManager.myNetBuffer.flip();
                        while (sslManager.myNetBuffer.hasRemaining()) {
                            int writeBytes = socketChannel.write(sslManager.myNetBuffer);
                            if (writeBytes <= 0) {
                                return false;
                            }
                        }
                        sslManager.myNetBuffer.compact();
                        break;
                    case CLOSED:
                        handleDisconnect();
                        return false;
                    case BUFFER_OVERFLOW:
                        // 加密数据超过Buffer容量，则扩容后再次进行加密
                        sslManager.enlargeMyNetBuffer();
                        break;
                    default:
                        return false;
                }
            }
            sslManager.myAppBuffer.clear();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private ByteBuffer sslWrapBuffer(SSLEngine sslEngine, ByteBuffer appOutBuffer) {
        ByteBuffer netOutBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        return sslWrapBuffer(sslEngine, appOutBuffer, netOutBuffer);
    }

    private ByteBuffer sslWrapBuffer(SSLEngine sslEngine, ByteBuffer appOutBuffer, ByteBuffer netOutBuffer) {
        try {
            // 加密数据
            SSLEngineResult wrapResult = sslEngine.wrap(appOutBuffer, netOutBuffer);
            switch (wrapResult.getStatus()) {
                case OK:
                    return netOutBuffer;
                case CLOSED:
                    handleDisconnect();
                    return null;
                case BUFFER_OVERFLOW:
                    // 加密数据超过Buffer容量，则扩容后再次进行加密
                    netOutBuffer = enlargeBuffer(netOutBuffer, sslEngine.getSession().getPacketBufferSize());
                    return sslWrapBuffer(sslEngine, appOutBuffer, netOutBuffer);
                default:
                    return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private ByteBuffer enlargeBuffer(ByteBuffer buffer, int sessionProposedCapacity) {
        // 确定目标容量
        int newCapacity = sessionProposedCapacity > buffer.capacity() ? sessionProposedCapacity : buffer.capacity() * 2;
        // 创建新缓冲区并复制原有数据
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
        if (buffer.position() > 0) {
            // 切换为读模式，准备复制剩余数据
            buffer.flip();
            // 复制剩余数据
            newBuffer.put(buffer);
        }
        return newBuffer;
    }

    private void closeConnection() {
        try {
            if (null != sslManager) {
                boolean sslClosed = sslManager.closeHandshake(socketChannel, sslManager.sslEngine);
                if (sslClosed) {
                    System.out.println(SSL_CLIENT_LOG + "[CloseHandshake Succeed] [SocketChannelAddr: " + socketChannel.getRemoteAddress() + "]");
                }
                sslManager = null;
            }
            if (null != selector) {
                selector.close();
                selector = null;
            }
            if (null != socketChannel) {
                socketChannel.close();
                socketChannel = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            closeConnection();
            System.out.println(SSL_CLIENT_LOG + "[Client Disconnect]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDisconnect() {
        System.out.println(SSL_CLIENT_LOG + "[Handle Disconnected]");
        reconnect();
    }

    private void handleEndOfStream() {
        try {
            sslManager.sslEngine.closeInbound();
        } catch (Exception e) {
            System.out.println(SSL_CLIENT_LOG + "[EndOfStream] [Server UnExcepted Disconnected]");
        }
        System.out.println(SSL_CLIENT_LOG + "[ServerUnExceptedDisconnected] [Handled]");
        // closeConnection();
        reconnect();
    }

    public boolean reconnect() {
        try {
            closeConnection();
            TimeUnit.SECONDS.sleep((reconnectCount + 1) * 3L);
            if (isStopped()) {
                return false;
            }
            if (reconnectCount >= RECONNECT_MAX_COUNT) {
                System.out.println(SSL_CLIENT_LOG + "[Reconnect Stop] [ReconnectMaxCount: " + RECONNECT_MAX_COUNT + "]");
                stop();
                return false;
            }
            reconnectCount++;
            System.out.println(SSL_CLIENT_LOG + "[Reconnecting] [ReconnectCount: " + reconnectCount + "]");
            return connect();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void keepAlive() {
        if (this.pongLostCount >= PONG_LOST_MAX_COUNT) {
            System.out.println(SSL_CLIENT_LOG + "[Pong Lost Max Count] [Will Try Reconnect] [MaxCount: " + PONG_LOST_MAX_COUNT + "]");
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
        System.out.println(SSL_CLIENT_LOG + "[Received Server Pong]");
        pongLostCount = 0;
    }

    public void handleServerPing() {
        System.out.println(SSL_CLIENT_LOG + "[Received Server Ping]");
        pong();
    }
}