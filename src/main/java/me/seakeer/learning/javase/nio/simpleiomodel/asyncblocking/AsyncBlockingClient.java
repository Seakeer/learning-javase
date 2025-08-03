package me.seakeer.learning.javase.nio.simpleiomodel.asyncblocking;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Future;

/**
 * JavaNioClient;
 *
 * @author Seakeer;
 * @date 2024/10/12;
 */
public class AsyncBlockingClient {

    private String serverHostname;

    private int serverPort;

    private AsynchronousSocketChannel asyncSocketChannel;

    public AsyncBlockingClient(String serverHostname, int serverPort) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) {
        AsyncBlockingClient client = new AsyncBlockingClient("127.0.0.1", 9090);
        mockSendMsg2Server(client);
        client.start();
    }

    private static void mockSendMsg2Server(AsyncBlockingClient client) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                client.sendMsg(scanner.nextLine());
            }
        }).start();
    }

    public AsyncBlockingClient connect() {
        try {
            AsynchronousSocketChannel asc = AsynchronousSocketChannel.open();
            Future<Void> connectFuture = asc.connect(new InetSocketAddress(serverHostname, serverPort));
            connectFuture.get();
            asyncSocketChannel = asc;
            System.out.printf("[Client] [Connected to server: %s:%d]\n", serverHostname, serverPort);
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
        receivingMsg();
    }

    private void receivingMsg() {
        while (true) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            Future<Integer> readFuture = asyncSocketChannel.read(buffer);
            try {
                Integer result = readFuture.get();
                if (result > 0) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String msg = new String(data, StandardCharsets.UTF_8);
                    System.out.println("[Client] [Received msg: " + msg + "]");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean sendMsg(String msg) {
        if (asyncSocketChannel == null) {
            return false;
        }
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        try {
            asyncSocketChannel.write(byteBuffer).get();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[Client] [Send Msg Failed] [Msg: " + msg + "]");
            return false;
        }
        System.out.println("[Client] [Send msg: " + msg + "]");
        return true;
    }
}
