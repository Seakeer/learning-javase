package me.seakeer.learning.javase.nio.simpleiomodel.syncnonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SyncNonBlockingServer;
 *
 * @author Seakeer;
 * @date 2024/10/14;
 */
public class SyncNonBlockingServer {

    private final int port;

    private ServerSocketChannel serverSocketChannel;

    private Selector selector;

    private final Map<SocketChannel, String> SOCKET_CHANNEL_CLIENT_ID_MAP = new ConcurrentHashMap<>();
    private final Map<String, SocketChannel> CLIENT_ID_SOCKE_CHANNELT_MAP = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ConcurrentLinkedQueue<String>> SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP = new ConcurrentHashMap<>();


    public SyncNonBlockingServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        SyncNonBlockingServer server = new SyncNonBlockingServer(9090);
        mockSendMsg2Client(server);
        server.start();
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
        while (true) {
            try {
                int keysNum = selector.select();
                if (keysNum <= 0) {
                    continue;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    // 移除当前处理的 SelectionKey
                    iterator.remove();
                    if (selectionKey.isAcceptable()) {
                        handleOpAccept();
                    }
                    if (selectionKey.isReadable()) {
                        handleOpRead(selectionKey);
                    }
                    if (selectionKey.isWritable()) {
                        handleOpWrite(selectionKey);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleOpWrite(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ConcurrentLinkedQueue<String> msgQueue = SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.getOrDefault(socketChannel, new ConcurrentLinkedQueue<>());
        String msg;
        while ((msg = msgQueue.peek()) != null) {
            if (write(socketChannel, msg)) {
                msgQueue.poll();
                System.out.printf("[Server] [Send Msg] [To client: %s, msg: %s]\n", SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel), msg);
            } else {
                break;
            }
        }
    }

    private void handleOpRead(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        if (!handleClientMsg(socketChannel)) {
            handleDisconnect(socketChannel);
        }
    }

    public boolean handleClientMsg(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        StringBuilder readContent = new StringBuilder();
        int bytesRead;
        while ((bytesRead = socketChannel.read(byteBuffer)) > 0) {
            byteBuffer.flip();
            readContent.append(StandardCharsets.UTF_8.decode(byteBuffer));
        }
        if (bytesRead == -1) {
            return false;
        }
        String msg = readContent.toString();
        System.out.println("[Server] [Received msg: " + msg + "; From Client: " + SOCKET_CHANNEL_CLIENT_ID_MAP.get(socketChannel) + "]");
        if (msg.startsWith("FROM ")) {
            String clientId = msg.substring(5);
            SOCKET_CHANNEL_CLIENT_ID_MAP.put(socketChannel, clientId);
            CLIENT_ID_SOCKE_CHANNELT_MAP.put(clientId, socketChannel);
        }
        return true;
    }

    private void handleDisconnect(SocketChannel socketChannel) throws IOException {
        SelectionKey key = socketChannel.keyFor(selector);
        if (key != null) {
            key.cancel();
        }
        socketChannel.close();
        String clientId = SOCKET_CHANNEL_CLIENT_ID_MAP.remove(socketChannel);
        if (clientId != null) {
            CLIENT_ID_SOCKE_CHANNELT_MAP.remove(clientId);
        }
        SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.remove(socketChannel);
        System.out.println("[Server] [Client disconnected: " + clientId + "]");
    }

    private void handleOpAccept() {
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            System.out.println("[Server] [Accepted client: " + socketChannel.getRemoteAddress() + "]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        init();
        System.out.println("[Server] [Server is running]");
        run();
    }

    private static void mockSendMsg2Client(SyncNonBlockingServer server) {
        Thread thread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                try {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void sendToAllClient(String msg) {
        for (SocketChannel socketChannel : SOCKET_CHANNEL_CLIENT_ID_MAP.keySet()) {
            write(socketChannel, msg);
        }
    }

    public boolean sendMsg(String clientId, String msg) {
        SocketChannel socketChannel = CLIENT_ID_SOCKE_CHANNELT_MAP.get(clientId);
        if (null == socketChannel) {
            return false;
        }
        ConcurrentLinkedQueue<String> msgQueue = SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.getOrDefault(socketChannel, new ConcurrentLinkedQueue<>());
        msgQueue.offer(msg);
        SOCKET_CHANNEL_SENDING_MSG_QUEUE_MAP.put(socketChannel, msgQueue);
        return true;
    }

    private boolean write(SocketChannel socketChannel, String msg) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
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
