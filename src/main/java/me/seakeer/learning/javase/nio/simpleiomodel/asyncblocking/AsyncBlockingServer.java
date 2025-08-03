package me.seakeer.learning.javase.nio.simpleiomodel.asyncblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * JavaNioServer;
 *
 * @author Seakeer;
 * @date 2024/10/12;
 */
public class AsyncBlockingServer {

    private final int port;

    private AsynchronousServerSocketChannel asyncServerSocketChannel;


    private final Map<AsynchronousSocketChannel, String> ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP = new ConcurrentHashMap<>();
    private final Map<String, AsynchronousSocketChannel> CLIENT_ID_ASYNC_SOCKET_CHANNEL_MAP = new ConcurrentHashMap<>();


    public AsyncBlockingServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        AsyncBlockingServer server = new AsyncBlockingServer(9090);
        mockSendMsg2Client(server);
        server.start();
    }

    private static void mockSendMsg2Client(AsyncBlockingServer server) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String msg = scanner.nextLine();
                if (!msg.startsWith("TO ")) {
                    server.sendToAllClient(msg);
                    continue;
                }
                String[] msgParts = msg.split(" ");
                server.sendMsg(msgParts[1], msgParts[2]);
            }
        }).start();
    }

    private void sendToAllClient(String msg) {
        for (AsynchronousSocketChannel socket : ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP.keySet()) {
            write(socket, msg);
        }
    }

    private void init() {
        try {
            AsynchronousServerSocketChannel asyncSsc = AsynchronousServerSocketChannel.open(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()))
                    .bind(new InetSocketAddress(port));
            asyncServerSocketChannel = asyncSsc;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run() {
        while (true) {
            try {
                Future<AsynchronousSocketChannel> acceptFuture = asyncServerSocketChannel.accept();
                AsynchronousSocketChannel asc = acceptFuture.get();
                System.out.println("[Server][Accepted client: " + asc.getRemoteAddress() + "]");
                handleClientMsg(asc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void handleClientMsg(AsynchronousSocketChannel asc) {
        new Thread(() -> doHandleClientMsg(asc)).start();
    }

    private void doHandleClientMsg(AsynchronousSocketChannel asc) {
        receivingMsg(asc);
    }

    private void receivingMsg(AsynchronousSocketChannel asc) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (true) {
            Future<Integer> readFuture = asc.read(buffer);
            try {
                Integer result = readFuture.get();
                if (result > 0) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String msg = new String(data, StandardCharsets.UTF_8);
                    System.out.println("[Server] [Received msg: " + msg + "; From Client: " + ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP.get(asc) + "]");
                    if (msg.startsWith("FROM ")) {
                        String clientId = msg.substring(5);
                        ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP.put(asc, clientId);
                        CLIENT_ID_ASYNC_SOCKET_CHANNEL_MAP.put(clientId, asc);
                    }
                }
                buffer.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        init();
        System.out.println("[Server] [Server is running]");
        run();
    }

    public boolean sendMsg(String clientId, String msg) {
        AsynchronousSocketChannel asc = CLIENT_ID_ASYNC_SOCKET_CHANNEL_MAP.get(clientId);
        if (null == asc) {
            return false;
        }
        return write(asc, msg);
    }

    private boolean write(AsynchronousSocketChannel asc, String msg) {
        try {
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            asc.write(byteBuffer).get();
            System.out.printf("[Server] [Send Msg][To client: %s; msg: %s]\n", ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP.get(asc), msg);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
