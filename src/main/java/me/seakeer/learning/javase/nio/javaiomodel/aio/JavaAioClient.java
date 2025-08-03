package me.seakeer.learning.javase.nio.javaiomodel.aio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * JavaAioClient;
 *
 * @author Seakeer;
 * @date 2024/10/12;
 */
public class JavaAioClient {

    private String serverHostname;

    private int serverPort;

    private AsynchronousSocketChannel asyncSocketChannel;

    private final CountDownLatch COUNT_DOWN_LATCH = new CountDownLatch(1);

    public static void main(String[] args) {
        JavaAioClient client = new JavaAioClient("127.0.0.1", 9090);
        mockSendMsg2Server(client);
        client.start();
    }

    private static void mockSendMsg2Server(JavaAioClient client) {
        Thread thread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                client.sendMsg(scanner.nextLine());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public JavaAioClient(String serverHostname, int serverPort) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    public JavaAioClient connect() {
        try {
            AsynchronousSocketChannel asc = AsynchronousSocketChannel.open();
            asc.connect(new InetSocketAddress(serverHostname, serverPort), null, new CompletionHandler<Void, Object>() {
                @Override
                public void completed(Void result, Object attachment) {
                    asyncSocketChannel = asc;
                    System.out.printf("[Client] [Connected to server: %s:%d][ThreadName: %s]\n", serverHostname, serverPort, Thread.currentThread().getName());
                    receiveMsg();
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    exc.printStackTrace();
                }
            });
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
        try {
            COUNT_DOWN_LATCH.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void receiveMsg() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        asyncSocketChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) {
                    System.out.println("[Client] [Server closed]");
                    return;
                }
                if (result > 0) {
                    attachment.flip();
                    byte[] data = new byte[attachment.remaining()];
                    attachment.get(data);
                    String msg = new String(data, StandardCharsets.UTF_8);
                    System.out.println("[Client] [Received msg: " + msg + "]" + "[ThreadName: " + Thread.currentThread().getName() + "]");
                }
                receiveMsg();
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                exc.printStackTrace();
            }
        });
    }

    public boolean sendMsg(String msg) {
        if (asyncSocketChannel == null) {
            return false;
        }
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        asyncSocketChannel.write(byteBuffer, null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer result, Object attachment) {
                if (result == -1) {
                    return;
                }
                System.out.println("[Client] [Send msg: " + msg + "]");
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
            }
        });
        return true;
    }
}
