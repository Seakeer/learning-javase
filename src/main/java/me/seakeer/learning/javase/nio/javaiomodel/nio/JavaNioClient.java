package me.seakeer.learning.javase.nio.javaiomodel.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * JavaNioClient;
 *
 * @author Seakeer;
 * @date 2024/10/12;
 */
public class JavaNioClient {

    private final String serverHostname;

    private final int serverPort;

    private SocketChannel socketChannel;

    private Selector selector;

    private static final ConcurrentLinkedQueue<String> SENDING_MSG_QUEUE = new ConcurrentLinkedQueue<>();

    public JavaNioClient(String serverHostname, int serverPort) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) {
        JavaNioClient client = new JavaNioClient("127.0.0.1", 9090);
        mockSendMsg2Server(client);
        client.start();
    }

    private static void mockSendMsg2Server(JavaNioClient client) {
        Thread thread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                client.sendMsg(scanner.nextLine());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }


    public JavaNioClient connect() {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            boolean connected = socketChannel.connect(new InetSocketAddress(serverHostname, serverPort));
            Selector selector = Selector.open();
            if (connected) {
                System.out.printf("[Client] [Connected to server: %s:%d]\n", serverHostname, serverPort);
                socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            } else {
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
            this.selector = selector;
            this.socketChannel = socketChannel;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public void start() {
        connect();
        run();
    }

    private void run() {
        while (true) {
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
                            System.out.printf("[Client] [Connected to server: %s:%d]\n", serverHostname, serverPort);
                            sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        } else {
                            sc.register(selector, SelectionKey.OP_CONNECT);
                        }
                    }
                    if (selectionKey.isReadable()) {
                        receiveMsg();
                    }
                    if (selectionKey.isWritable()) {
                        doSendMsg();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void doSendMsg() {
        String msg;
        while ((msg = SENDING_MSG_QUEUE.peek()) != null) {
            if (write(msg)) {
                SENDING_MSG_QUEUE.poll();
            } else {
                break;
            }
        }
    }

    private void receiveMsg() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        try {
            int readBytes = socketChannel.read(byteBuffer);
            if (readBytes < 0) {
                System.out.println("[Client] [Server closed]");
                return;
            }
            if (readBytes > 0) {
                byteBuffer.flip();
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                String readContent = new String(bytes, StandardCharsets.UTF_8);
                System.out.println("[Client] [Received msg: " + readContent + "]");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean sendMsg(String msg) {
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
            System.out.println("[Client] [Send msg: " + msg + "]");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
