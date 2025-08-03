package me.seakeer.learning.javase.network.ssltls;

import me.seakeer.learning.javase.network.MsgEnDecoder;

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SslManager;
 * 1. 维护SslEngine的缓冲区，实现缓冲区复用，非多线程安全
 * 2. 提供SSL握手的实现
 * 3. 提供SSL写的通用实现，读因为需要处理业务逻辑，由服务端和客户端分别实现
 *
 * @author Seakeer;
 * @date 2025/6/3;
 */
public class MainSslManager {

    public static final String SSL_HANDSHAKE_LOG = "[SslHandshake]";

    // 握手超时时间（毫秒）
    private static final long HANDSHAKE_TIMEOUT_MS = 5_000;


    /**
     * 标识握手过程中的处理结果
     */
    enum HandshakingResult {

        /**
         * 进行中，即需要继续握手过程
         */
        DOING,

        /**
         * 完成，即握手过程完成了
         */
        DONE,

        /**
         * 超时，则中断握手过程
         */
        TIMEOUT,

        /**
         * 发生异常，则中断握手过程
         */
        ERROR;
    }

    enum WriteResult {

        /**
         * 写完了
         */
        DONE,

        /**
         * 关闭了
         */
        CLOSED,

        /**
         * 发生异常
         */
        ERROR;
    }

    // 握手过程NEED_TASK的任务执行线程池
    private final ExecutorService TASK_THREAD_POOL = new ThreadPoolExecutor(2, 8,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(24),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );


    protected SSLEngine sslEngine;
    // 缓冲区复用，避免频繁创建
    protected ByteBuffer myAppBuffer;
    protected ByteBuffer myNetBuffer;
    protected ByteBuffer peerAppBuffer;
    protected ByteBuffer peerNetBuffer;

    public MainSslManager(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
        initBuffer(sslEngine);
    }

    public ByteBuffer enlargePeerAppBuffer() {
        peerAppBuffer = enlargeBuffer(peerAppBuffer, sslEngine.getSession().getApplicationBufferSize());
        return peerAppBuffer;
    }

    public ByteBuffer enlargePeerNetBuffer() {
        peerNetBuffer = enlargeBuffer(peerNetBuffer, sslEngine.getSession().getPacketBufferSize());
        return peerNetBuffer;
    }

    public ByteBuffer enlargeMyAppBuffer(int msgBytes) {
        int targetBytes = msgBytes > sslEngine.getSession().getPacketBufferSize() ? msgBytes : sslEngine.getSession().getApplicationBufferSize();
        myAppBuffer = enlargeBuffer(myAppBuffer, targetBytes);
        return myAppBuffer;
    }

    public ByteBuffer enlargeMyNetBuffer() {
        myNetBuffer = enlargeBuffer(myNetBuffer, sslEngine.getSession().getPacketBufferSize());
        return myNetBuffer;
    }

    public static SSLContext crtSslContext(String keystorePath, String keystorePassword,
                                           String truststorePath, String truststorePassword) throws Exception {
        // 加载密钥库
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(MainSslManager.class.getClassLoader().getResourceAsStream(keystorePath), keystorePassword.toCharArray());

        // 初始化 KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        // 加载信任库（可信任服务端证书的 CA）
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(MainSslManager.class.getClassLoader().getResourceAsStream(truststorePath), truststorePassword.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 初始化 SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setEnabledProtocols(new String[]{"TLSv1.2"});
        sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
        return sslContext;
    }


    /**
     * 建立连接握手
     *
     * @param socketChannel
     * @return true表示连接建立成功，false表示失败
     */
    public boolean openHandshake(SocketChannel socketChannel) {
        try {
            sslEngine.beginHandshake();
            handshaking(socketChannel);
            return sslEngine.getSession().isValid();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 关闭连接握手
     *
     * @param socketChannel
     * @param engine
     * @return true表示连接关闭成功，false表示失败
     */
    public boolean closeHandshake(SocketChannel socketChannel, SSLEngine engine) {
        try {
            engine.closeOutbound();
            handshaking(socketChannel);
            return engine.isInboundDone() && engine.isOutboundDone();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void handshaking(SocketChannel socketChannel) {

        // 记录开始时间，用于超时判定
        long startTimeMillis = System.currentTimeMillis();

        // 循环执行握手流程，根据结果决定是否退出循环
        while (true) {
            HandshakingResult result = doHandshake(socketChannel, startTimeMillis);
            // 耗时统计
            long costMillis = System.currentTimeMillis() - startTimeMillis;
            switch (result) {
                case DOING:
                    continue;

                case DONE:
                case TIMEOUT:
                case ERROR:
                    clearBuffer();
                    System.out.printf(SSL_HANDSHAKE_LOG + "[%s] [Cost: %dms]\n", result.name(), costMillis);
                    return;
                default:
                    clearBuffer();
                    System.out.printf(SSL_HANDSHAKE_LOG + "[Invalid Handshaking Result] [Cost: %d ms]\n", costMillis);
                    return;
            }
        }
    }

    private HandshakingResult doHandshake(SocketChannel socketChannel, long startTimeMillis) {
        try {
            // 超时则中断握手
            if (System.currentTimeMillis() - startTimeMillis > HANDSHAKE_TIMEOUT_MS) {
                return HandshakingResult.TIMEOUT;
            }

            HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
            switch (handshakeStatus) {
                // 需加密数据进行发送
                case NEED_WRAP:
                    return handleNeedWrap(socketChannel);

                // 需接收数据进行解密
                case NEED_UNWRAP:
                    return handleNeedUnwrap(socketChannel);

                // 需要执行任务
                case NEED_TASK:
                    return handleNeedTask();

                // 握手完成 & 没有在进行握手
                case FINISHED:
                case NOT_HANDSHAKING:
                    return HandshakingResult.DONE;
                default:
                    System.out.println(SSL_HANDSHAKE_LOG + "[Invalid Handshake Status]");
                    return HandshakingResult.ERROR;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return HandshakingResult.ERROR;
        }
    }

    private void initBuffer(SSLEngine engine) {
        // 初始化或复用缓冲区
        int appBufferSize = engine.getSession().getApplicationBufferSize();
        int packetBufferSize = engine.getSession().getPacketBufferSize();

        if (myAppBuffer == null || myAppBuffer.capacity() < appBufferSize) {
            myAppBuffer = ByteBuffer.allocate(appBufferSize);
        } else {
            myAppBuffer.clear();
        }

        if (peerAppBuffer == null || peerAppBuffer.capacity() < appBufferSize) {
            peerAppBuffer = ByteBuffer.allocate(appBufferSize);
        } else {
            peerAppBuffer.clear();
        }

        if (myNetBuffer == null || myNetBuffer.capacity() < packetBufferSize) {
            myNetBuffer = ByteBuffer.allocate(packetBufferSize);
        } else {
            myNetBuffer.clear();
        }

        if (peerNetBuffer == null || peerNetBuffer.capacity() < packetBufferSize) {
            peerNetBuffer = ByteBuffer.allocate(packetBufferSize);
        } else {
            peerNetBuffer.clear();
        }
    }

    private void clearBuffer() {
        myAppBuffer.clear();
        peerAppBuffer.clear();
        myNetBuffer.clear();
        peerNetBuffer.clear();
    }

    private HandshakingResult handleNeedWrap(SocketChannel socketChannel) {
        try {
            // 握手阶段，myAppBuffer不需要数据，SSLEngine会自动填充加密后的myNetBuffer
            SSLEngineResult result = sslEngine.wrap(myAppBuffer, myNetBuffer);
            switch (result.getStatus()) {
                // 表示数据加密完成
                case OK:
                    // 发送加密后的数据
                    myNetBuffer.flip();
                    while (myNetBuffer.hasRemaining()) {
                        socketChannel.write(myNetBuffer);
                    }
                    myNetBuffer.clear();
                    // 发送数据完成后，需要等待对方响应，所以继续进行握手
                    return HandshakingResult.DOING;

                // 表示myNetBuffer溢出，即无法容纳加密后的数据，需要扩容
                case BUFFER_OVERFLOW:
                    // 溢出的情况下myNetData还没有数据，可以直接扩容为新的Buffer
                    enlargeMyNetBuffer();

                    // 扩容后继续进行握手，下一次将继续加密数据进行发送
                    return HandshakingResult.DOING;

                // 表示myNetBuffer没有数据，NEED_WRAP情况下不会发生
                case BUFFER_UNDERFLOW:
                    System.out.println(SSL_HANDSHAKE_LOG + "[NEED_WRAP][BUFFER_UNDERFLOW][Unexpected buffer underflow during wrap]");
                    return HandshakingResult.ERROR;

                // 表示出站OutBound已经关闭
                case CLOSED:
                    // 如果出站已经关闭，还需要将加密后的数据发送出去，即发送close_notify通知对端
                    try {
                        myNetBuffer.flip();
                        while (myNetBuffer.hasRemaining()) {
                            socketChannel.write(myNetBuffer);
                        }
                        myNetBuffer.clear();
                        // close_notify通知对端后，需要等待对端会进行响应，将peerNetBuffer清空以确保接收到正确的响应
                        // 之后将进入NEED_UNWRAP流程，读取对端响应
                        peerNetBuffer.clear();
                        return HandshakingResult.DOING;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return HandshakingResult.ERROR;
                    }
                default:
                    System.out.println(SSL_HANDSHAKE_LOG + "[Invalid Wrap Status: " + result.getStatus() + "]");
                    return HandshakingResult.ERROR;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(SSL_HANDSHAKE_LOG + "[Wrap Exception]");
            return HandshakingResult.ERROR;
        }
    }

    private HandshakingResult handleNeedUnwrap(SocketChannel socketChannel) {
        try {
            // 如果上一次解密失败，上一次读取的数据需要继续保留在peerNetBuffer中，再次read只是补充后续数据或没有数据读到
            int bytesRead = socketChannel.read(peerNetBuffer);

            // 表示连接已经关闭
            if (bytesRead < 0) {
                // 如果入站出站都完成，即连接已经正常关闭，则整个握手流程完成了
                if (sslEngine.isInboundDone() && sslEngine.isOutboundDone()) {
                    return HandshakingResult.DONE;
                }
                // 未收到对方对于本方的关闭连接请求的响应，则强制关闭连接
                try {
                    sslEngine.closeInbound();
                } catch (SSLException e) {
                    System.out.println(SSL_HANDSHAKE_LOG + "[Force Close Inbound]");
                    //("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
                }
                sslEngine.closeOutbound();
                // After closeOutbound the engine will be set to WRAP state, in order to try to send a close message to the client.
                return HandshakingResult.DOING;
            }

            peerNetBuffer.flip();
            SSLEngineResult result = sslEngine.unwrap(peerNetBuffer, peerAppBuffer);

            switch (result.getStatus()) {
                // 表示解密成功，则继续进行握手
                case OK:
                    // 下一次循环还会向peerNetBuffer中读取数据，因此需要compact()
                    peerNetBuffer.compact();
                    return HandshakingResult.DOING;

                // 表示peerAppBuffer溢出，则需要扩容
                case BUFFER_OVERFLOW:
                    // 下一次循环还会向peerNetBuffer中读取数据，因此需要compact()
                    peerNetBuffer.compact();
                    enlargePeerAppBuffer();
                    // 扩容后需要继续进行解码，则交给下一次循环进行处理
                    return HandshakingResult.DOING;

                // 表示peerNetBuffer不足，即peerNetBuffer没有数据或数据不足无法解密
                // 如果是数据不足，则意味着没有读取到足够的数据，则需要扩容peerNetBuffer进行读取数据
                // 如果是没有数据，即意味着bytesRead = 0，则可能是还没读取到数据，需要继续读取数据进行解密
                case BUFFER_UNDERFLOW:
                    handleUnWrapPeerNetBufferUnderflow();
                    return HandshakingResult.DOING;

                // 表示读取到了对端的关闭连接请求close_notify，则进行连接关闭
                case CLOSED:
                    // 如果出站已完成，即表示本端已经给对方发送了close_notify，则整个握手流程完成了
                    if (sslEngine.isOutboundDone()) {
                        return HandshakingResult.DONE;
                    }
                    // 下一次循环还会向peerNetBuffer中读取数据，因此需要compact()
                    peerNetBuffer.compact();
                    // 如果出站未完整，即表示本端先收到了对端的close_notify，则正常响应对端
                    // 本端收到请关闭请求，通过closeOutBound()方法触发发送给对端close_notify
                    sslEngine.closeOutbound();

                    // 继续继续握手，后续将进入NEED_WRAP发送数据给对端
                    return HandshakingResult.DOING;
                default:
                    System.out.println(SSL_HANDSHAKE_LOG + "[Invalid Unwrap Status: " + result.getStatus() + "]");
                    return HandshakingResult.ERROR;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(SSL_HANDSHAKE_LOG + "[Unwrap Exception]");
            return HandshakingResult.ERROR;
        }
    }

    private HandshakingResult handleNeedTask() {
        Runnable task;
        while ((task = sslEngine.getDelegatedTask()) != null) {
            TASK_THREAD_POOL.execute(task);
        }
        return HandshakingResult.DOING;
    }

    public static ByteBuffer enlargeBuffer(ByteBuffer buffer, int sessionProposedCapacity) {
        // 确定目标容量
        int newCapacity = sessionProposedCapacity > buffer.capacity() ? sessionProposedCapacity : buffer.capacity() * 2;

        // 创建新缓冲区并复制原有数据
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);

        // 如果缓冲区有剩余数据，则复制剩余数据
        if (buffer.position() > 0) {
            // 切换为读模式，准备复制剩余数据
            buffer.flip();
            // 复制剩余数据
            newBuffer.put(buffer);
        }
        return newBuffer;
    }

    public ByteBuffer handleUnWrapPeerNetBufferUnderflow() {
        peerNetBuffer.compact();
        if (sslEngine.getSession().getPacketBufferSize() < peerNetBuffer.limit()) {
            return peerNetBuffer;
        } else {
            return enlargePeerNetBuffer();
        }
    }


    public WriteResult write(SocketChannel socketChannel, String msg) {
        try {
            // 将要发送的消息写入myAppBuffer中, 如果myAppBuffer容量不够则先扩容
            while (true) {
                int msgBytes = MsgEnDecoder.encodeMsg(msg + SslServer.DELIMITER, myAppBuffer);
                if (msgBytes > 0) {
                    enlargeMyAppBuffer(msgBytes);
                } else {
                    break;
                }
            }
            while (myAppBuffer.hasRemaining()) {
                // 加密数据
                SSLEngineResult wrapResult = sslEngine.wrap(myAppBuffer, myNetBuffer);
                switch (wrapResult.getStatus()) {
                    case OK:
                        myNetBuffer.flip();
                        while (myNetBuffer.hasRemaining()) {
                            int writeBytes = socketChannel.write(myNetBuffer);
                            if (writeBytes <= 0) {
                                return WriteResult.ERROR;
                            }
                        }
                        myNetBuffer.clear();
                        break;
                    case CLOSED:
                        return WriteResult.CLOSED;
                    case BUFFER_OVERFLOW:
                        // 加密数据超过Buffer容量，则扩容后再次进行加密
                        enlargeMyNetBuffer();
                        break;
                    default:
                        return WriteResult.ERROR;
                }
            }
            myAppBuffer.clear();
            return WriteResult.DONE;
        } catch (Exception e) {
            e.printStackTrace();
            return WriteResult.ERROR;
        }
    }
}
