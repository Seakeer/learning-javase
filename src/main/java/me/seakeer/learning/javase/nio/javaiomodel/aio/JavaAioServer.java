package me.seakeer.learning.javase.nio.javaiomodel.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;


/**
 * JavaAioServer;
 *
 * @author Seakeer;
 * @date 2024/10/12;
 */
public class JavaAioServer {

    private final int port;

    private AsynchronousServerSocketChannel asyncServerSocketChannel;

    private final Map<AsynchronousSocketChannel, String> ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP = new ConcurrentHashMap<>();
    private final Map<String, AsynchronousSocketChannel> CLIENT_ID_ASYNC_SOCKET_CHANNEL_MAP = new ConcurrentHashMap<>();


    public JavaAioServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        JavaAioServer server = new JavaAioServer(9090);
        mockSendMsg2Client(server);
        server.start();
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
        System.out.println("[Server] [Server is running]");
        acceptAndRead();
    }

    private void acceptAndRead() {
        asyncServerSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(AsynchronousSocketChannel asc, Object attachment) {
                handleAccept(asc);
                handleClientMsg(asc);
                acceptAndRead();
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                acceptAndRead();
            }
        });
    }


    private void handleAccept(AsynchronousSocketChannel asc) {
        try {
            System.out.println("[Server][Accepted client: " + asc.getRemoteAddress() + "]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientMsg(AsynchronousSocketChannel asc) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        asc.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) {
                    handleClientDisconnect(asc);
                }
                if (result == 0) {
                    return;
                }
                attachment.flip();
                byte[] data = new byte[attachment.remaining()];
                attachment.get(data);
                String msg = new String(data, StandardCharsets.UTF_8);
                System.out.println("[Server] [Received msg: " + msg + "; From Client: " + ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP.get(asc) + "]");
                if (msg.startsWith("FROM ")) {
                    String clientId = msg.substring(5);
                    ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP.put(asc, clientId);
                    CLIENT_ID_ASYNC_SOCKET_CHANNEL_MAP.put(clientId, asc);
                }
                handleClientMsg(asc);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                exc.printStackTrace();
                handleClientDisconnect(asc);
            }
        });
    }

    public void start() {
        init();
        run();
    }

    private static void mockSendMsg2Client(JavaAioServer server) {
        new Thread(() -> {
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
        }).start();
    }

    private void sendToAllClient(String msg) {
        for (AsynchronousSocketChannel socket : ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP.keySet()) {
            write(socket, msg);
        }
    }

    public boolean sendMsg(String clientId, String msg) {
        AsynchronousSocketChannel asc = CLIENT_ID_ASYNC_SOCKET_CHANNEL_MAP.get(clientId);
        if (null == asc) {
            return false;
        }
        write(asc, msg);
        System.out.printf("[Server] [Send Msg][To client: %s; msg: %s]\n", clientId, msg);
        return true;
    }

    private void write(AsynchronousSocketChannel asc, String msg) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        asc.write(byteBuffer, null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer result, Object attachment) {
                if (result < 0) {
                    handleClientDisconnect(asc);
                } else {
                    System.out.println("[Server] [Send Msg][ClientId: " + ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP.get(asc) + "]" + " [Msg: " + msg + "]");
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
                handleClientDisconnect(asc);
            }
        });
    }


    private void handleClientDisconnect(AsynchronousSocketChannel asc) {
        try {
            String clientId = ASYNC_SOCKET_CHANNEL_CLIENT_ID_MAP.remove(asc);
            if (clientId != null) {
                CLIENT_ID_ASYNC_SOCKET_CHANNEL_MAP.remove(clientId);
                System.out.println("[Server] [Client disconnected: " + clientId + "]");
            }
            asc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
