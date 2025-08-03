package me.seakeer.learning.javase.network.ssltls;

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
 * SslHandler;
 *
 * @author Seakeer;
 * @date 2025/6/3;
 */
public class SslHandler {

    public static final String SSL_HANDSHAKE_LOG = "[SslHandshake]";

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

    // 握手过程NEED_TASK的任务执行线程池
    protected ExecutorService executor = new ThreadPoolExecutor(2, 8,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(24),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // 缓冲区复用，避免频繁创建
    protected ByteBuffer myAppBuffer;
    protected ByteBuffer myNetBuffer;
    protected ByteBuffer peerAppBuffer;
    protected ByteBuffer peerNetBuffer;

    // 握手超时时间（毫秒）
    private static final long HANDSHAKE_TIMEOUT = 5_000;

    public static SSLContext crtSslContext(String keystorePath, String keystorePassword,
                                           String truststorePath, String truststorePassword) throws Exception {
        // 加载密钥库
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(SslHandler.class.getClassLoader().getResourceAsStream(keystorePath), keystorePassword.toCharArray());

        // 初始化 KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        // 加载信任库（可信任服务端证书的 CA）
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(SslHandler.class.getClassLoader().getResourceAsStream(truststorePath), truststorePassword.toCharArray());

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
     * @param engine
     * @return true表示连接建立成功，false表示失败
     */
    public boolean openHandshake(SocketChannel socketChannel, SSLEngine engine) {
        try {
            engine.beginHandshake();
            handshaking(socketChannel, engine);
            return engine.getSession().isValid();
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
            handshaking(socketChannel, engine);
            return engine.isInboundDone() && engine.isOutboundDone();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void handshaking(SocketChannel socketChannel, SSLEngine engine) {

        // 记录开始时间，用于超时判定
        long startTimeMillis = System.currentTimeMillis();

        // 初始化握手需要的缓冲区
        initBuffer(engine);

        // 循环执行握手流程，根据结果决定是否退出循环
        while (true) {
            HandshakingResult result = doHandshake(socketChannel, engine, startTimeMillis);
            // 耗时统计
            long costMillis = System.currentTimeMillis() - startTimeMillis;
            switch (result) {
                case DOING:
                    continue;

                case DONE:
                case TIMEOUT:
                case ERROR:
                    System.out.printf(SSL_HANDSHAKE_LOG + "[%s] [Cost: %dms]\n", result.name(), costMillis);
                    return;
                default:
                    System.out.printf(SSL_HANDSHAKE_LOG + "[Invalid Handshaking Result] [Cost: %d ms]\n", costMillis);
                    return;
            }
        }
    }

    private HandshakingResult doHandshake(SocketChannel socketChannel, SSLEngine engine, long startTimeMillis) {
        try {
            // 超时则中断握手
//            if (System.currentTimeMillis() - startTimeMillis > HANDSHAKE_TIMEOUT) {
//                return HandshakingResult.TIMEOUT;
//            }

            HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
            switch (handshakeStatus) {
                // 需加密数据进行发送
                case NEED_WRAP:
                    return handleNeedWrap(socketChannel, engine);

                // 需接收数据进行解密
                case NEED_UNWRAP:
                    return handleNeedUnwrap(socketChannel, engine);

                // 需要执行任务
                case NEED_TASK:
                    return handleNeedTask(engine);

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

    private HandshakingResult handleNeedWrap(SocketChannel socketChannel, SSLEngine engine) {
        try {
            myNetBuffer.clear();
            // 握手阶段，myAppBuffer不需要数据，SSLEngine会自动填充加密后的myNetBuffer
            SSLEngineResult result = engine.wrap(myAppBuffer, myNetBuffer);
            switch (result.getStatus()) {
                // 表示数据加密完成
                case OK:
                    // 发送加密后的数据
                    myNetBuffer.flip();
                    while (myNetBuffer.hasRemaining()) {
                        socketChannel.write(myNetBuffer);
                    }
                    // 发送数据完成后，需要等待对方响应，所以继续进行握手
                    return HandshakingResult.DOING;

                // 表示myNetBuffer溢出，即无法容纳加密后的数据，需要扩容
                case BUFFER_OVERFLOW:
                    // 溢出的情况下myNetData还没有数据，可以直接扩容为新的Buffer
                    myNetBuffer = enlargePacketBuffer(engine, myNetBuffer);

                    // 库容后继续进行握手，下一次将继续加密数据进行发送
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

    private HandshakingResult handleNeedUnwrap(SocketChannel socketChannel, SSLEngine engine) {
        try {
            // 如果上一次解密失败，上一次读取的数据需要继续保留在peerNetBuffer中，再次read只是补充后续数据或没有数据读到
            int bytesRead = socketChannel.read(peerNetBuffer);

            // 表示连接已经关闭
            if (bytesRead < 0) {
                // 如果入站出站都完成，即连接已经正常关闭，则整个握手流程完成了
                if (engine.isInboundDone() && engine.isOutboundDone()) {
                    return HandshakingResult.DONE;
                }
                // 未收到对方对于本方的关闭连接请求的响应，则强制关闭连接
                try {
                    engine.closeInbound();
                } catch (SSLException e) {
                    System.out.println(SSL_HANDSHAKE_LOG + "[Force Close Inbound]");
                    //("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
                }
                engine.closeOutbound();
                // After closeOutbound the engine will be set to WRAP state, in order to try to send a close message to the client.
                return HandshakingResult.DOING;
            }

            peerNetBuffer.flip();
            SSLEngineResult result = engine.unwrap(peerNetBuffer, peerAppBuffer);
            peerNetBuffer.compact();

            switch (result.getStatus()) {
                // 表示解密成功，则继续进行握手
                case OK:
                    return HandshakingResult.DOING;

                // 表示peerAppBuffer溢出，则需要扩容
                case BUFFER_OVERFLOW:
                    peerAppBuffer = enlargeApplicationBuffer(engine, peerAppBuffer);
                    // 扩容后需要继续进行解码，则交给下一次循环进行处理
                    return HandshakingResult.DOING;

                // 表示peerNetBuffer不足，即peerNetBuffer没有数据或数据不足无法解密
                // 如果是数据不足，则意味着没有读取到足够的数据，则需要扩容peerNetBuffer进行读取数据
                // 如果是没有数据，即意味着bytesRead = 0，则可能是还没读取到数据，需要继续读取数据进行解密
                case BUFFER_UNDERFLOW:
                    peerNetBuffer = handleUnWrapPeerNetBufferUnderflow(engine, peerNetBuffer);
                    return HandshakingResult.DOING;

                // 表示读取到了对端的关闭连接请求close_notify，则进行连接关闭
                case CLOSED:
                    // 如果出站已完成，即表示本端已经给对方发送了close_notify，则整个握手流程完成了
                    if (engine.isOutboundDone()) {
                        return HandshakingResult.DONE;
                    }
                    // 如果出站未完整，即表示本端先收到了对端的close_notify，则正常响应对端
                    // 本端收到请关闭请求，通过closeOutBound()方法触发发送给对端close_notify
                    engine.closeOutbound();

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

    private HandshakingResult handleNeedTask(SSLEngine engine) {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null) {
            executor.execute(task);
        }
        return HandshakingResult.DOING;
    }

    public static ByteBuffer enlargePacketBuffer(SSLEngine engine, ByteBuffer buffer) {
        return enlargeBuffer(buffer, engine.getSession().getPacketBufferSize());
    }

    public static ByteBuffer enlargeApplicationBuffer(SSLEngine engine, ByteBuffer buffer) {
        return enlargeBuffer(buffer, engine.getSession().getApplicationBufferSize());
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

    public static ByteBuffer handleUnWrapPeerNetBufferUnderflow(SSLEngine engine, ByteBuffer peerNetBuffer) {
        // peerNetBuffer容量足够，则说明没有读取到数据，继续读取数据
        if (engine.getSession().getPacketBufferSize() < peerNetBuffer.limit()) {
            return peerNetBuffer;
        } else {
            // peerNetBuffer容量不足，则扩容
            return enlargePacketBuffer(engine, peerNetBuffer);
        }
    }
}
