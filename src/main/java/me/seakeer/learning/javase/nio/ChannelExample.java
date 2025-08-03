package me.seakeer.learning.javase.nio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ChannelExample;
 *
 * @author Seakeer;
 * @date 2024/10/15;
 */
public class ChannelExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("----------------文件通道示例--------------");
        fileChannelExample();

        System.out.println("----------------TCP套接字通道示例--------------");
        tcpSocketChannel();

        TimeUnit.SECONDS.sleep(2);
        System.out.println("----------------UDP套接字通道示例--------------");
        udpSocketChannel();

        TimeUnit.SECONDS.sleep(2);
        System.out.println("----------------Pipe管道SinkChannel SourceChannel示例--------------");
        pipeSinkSourceChannelExample();

    }

    private static void fileChannelExample() {
        System.out.println("[FileChannel][ZeroCopy]");
        try (RandomAccessFile randomAccessFile = new RandomAccessFile("SrcFile.txt", "rw")) {
            FileChannel fileChannel = randomAccessFile.getChannel();
            byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
            MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, bytes.length);
            map.put(bytes);
            map.force();
            System.out.println("[FileChannel][map <--> mmap]");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileChannel srcFile = FileChannel.open(Paths.get("SrcFile.txt"), StandardOpenOption.READ);
             FileChannel destFile = FileChannel.open(Paths.get("DestFile.txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            srcFile.transferTo(0, srcFile.size(), destFile);
            System.out.println("[FileChannel][transferTo <---> sendfile]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void tcpSocketChannel() {
        new Thread(() -> tcpServer("127.0.0.1", 9999)).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> tcpClient("127.0.0.1", 9999)).start();
    }

    private static void tcpServer(String host, int port) {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(host, port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("[Server] [Started]");
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
                        if (selectionKey.isAcceptable()) {
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            socketChannel.configureBlocking(false);
                            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            System.out.println("[Server] [Accepted client: " + socketChannel.getRemoteAddress() + "]");
                        } else if (selectionKey.isReadable()) {
                            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                            StringBuilder readContent = new StringBuilder();
                            int bytesRead;
                            while ((bytesRead = socketChannel.read(buffer)) > 0) {
                                buffer.flip();
                                readContent.append(StandardCharsets.UTF_8.decode(buffer));
                            }
                            if (bytesRead == -1) {
                                System.out.println("[Server] [Client Disconnected]");
                                break;
                            }
                            System.out.println("[Server] [Received msg: " + readContent + "; From Client: " + socketChannel.getRemoteAddress() + "]");
                            socketChannel.register(selector, SelectionKey.OP_READ);
                        } else if (selectionKey.isWritable()) {
                            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                            socketChannel.write(StandardCharsets.UTF_8.encode("Hello Client"));
                        }
                        iterator.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void tcpClient(String serverHost, int serverPort) {
        try (SocketChannel socketChannel = SocketChannel.open();
             Selector selector = Selector.open()) {
            socketChannel.configureBlocking(false);
            boolean connected = socketChannel.connect(new InetSocketAddress(serverHost, serverPort));
            if (connected) {
                socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            } else {
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
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
                        if (selectionKey.isConnectable()) {
                            socketChannel.finishConnect();
                            System.out.println("[Client] [Connected to server: " + socketChannel.getRemoteAddress() + "]");
                            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        } else if (selectionKey.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                            StringBuilder readContent = new StringBuilder();
                            int bytesRead;
                            while ((bytesRead = socketChannel.read(buffer)) > 0) {
                                buffer.flip();
                                readContent.append(StandardCharsets.UTF_8.decode(buffer));
                            }
                            if (bytesRead == -1) {
                                System.out.println("[Client] [Server Closed]");
                                break;
                            }
                            System.out.println("[Client] [Received msg: " + readContent + "]");
                            socketChannel.register(selector, SelectionKey.OP_READ);
                        } else if (selectionKey.isWritable()) {
                            socketChannel.write(ByteBuffer.wrap("Hello Server".getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void udpSocketChannel() {
        new Thread(() -> udpSender("127.0.0.1", 9998)).start();
        new Thread(() -> udpReceiver("127.0.0.1", 9998)).start();
    }

    private static void udpSender(String destHost, int destPort) {
        try (DatagramChannel datagramChannel = DatagramChannel.open();
             Selector selector = Selector.open()) {
            datagramChannel.configureBlocking(false);
            datagramChannel.register(selector, SelectionKey.OP_WRITE);
            InetSocketAddress destAddr = new InetSocketAddress(destHost, destPort);
            datagramChannel.connect(destAddr);
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            int sendNum = 1;
            while (sendNum <= 5) {
                int keysNum = selector.select();
                if (keysNum <= 0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                if (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    if (selectionKey.isWritable() && sendNum <= 5) {
                        String datagram = sendNum + ". HELLO UDP";
                        buffer.put(datagram.getBytes(StandardCharsets.UTF_8));
                        buffer.flip();
                        datagramChannel.send(buffer, destAddr);
                        System.out.println("[UDP Sender][Send Datagram: " + datagram + "]");
                        buffer.clear();
                        sendNum++;
                    }
                    iterator.remove();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void udpReceiver(String host, int port) {
        try (DatagramChannel datagramChannel = DatagramChannel.open();
             Selector selector = Selector.open()) {
            datagramChannel.configureBlocking(false);
            datagramChannel.bind(new InetSocketAddress(host, port));
            datagramChannel.register(selector, SelectionKey.OP_READ);
            while (true) {
                try {
                    int keysNum = selector.select();
                    if (keysNum <= 0) {
                        continue;
                    }
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    if (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                            while (null != datagramChannel.receive(buffer)) {
                                buffer.flip();
                                System.out.println("[UDP Receiver][Received Datagram: " + StandardCharsets.UTF_8.decode(buffer) + "]");
                                buffer.clear();
                            }
                        }
                        iterator.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void pipeSinkSourceChannelExample() {
        try {
            // 创建 Pipe
            Pipe pipe = Pipe.open();
            new Thread(() -> sinkChannel(pipe)).start();
            new Thread(() -> sourceChannel(pipe)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sinkChannel(Pipe pipe) {
        try (Pipe.SinkChannel sinkChannel = pipe.sink();
             Selector selector = Selector.open()) {
            sinkChannel.configureBlocking(false);
            sinkChannel.register(selector, SelectionKey.OP_WRITE);
            boolean sinkDone = false;
            while (!sinkDone) {
                int readyChannels = selector.select();
                if (readyChannels <= 0) {
                    continue;
                }
                Set<SelectionKey> keySet = selector.selectedKeys();
                Iterator<SelectionKey> keyItr = keySet.iterator();
                while (keyItr.hasNext()) {
                    SelectionKey key = keyItr.next();
                    if (key.isWritable() && !sinkDone) {
                        String data = "Hello Pipe";
                        // 不需要buffer.flip() 因为该方法返回的buffer已经是读模式
                        ByteBuffer buffer = StandardCharsets.UTF_8.encode(data);
                        sinkChannel.write(buffer);
                        sinkDone = true;
                        System.out.println("[Pipe.SinkChannel][Write Data: " + data + "]");
                    }
                    keyItr.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sourceChannel(Pipe pipe) {
        try (Pipe.SourceChannel sourceChannel = pipe.source();
             Selector selector = Selector.open()) {
            sourceChannel.configureBlocking(false);
            sourceChannel.register(selector, SelectionKey.OP_READ);
            while (true) {
                int readyChannels = selector.select();
                if (readyChannels <= 0) {
                    continue;
                }
                Set<SelectionKey> keySet = selector.selectedKeys();
                Iterator<SelectionKey> keyItr = keySet.iterator();
                while (keyItr.hasNext()) {
                    SelectionKey key = keyItr.next();
                    if (key.isReadable()) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate(64);
                        int read = sourceChannel.read(byteBuffer);
                        if (read != -1) {
                            byteBuffer.flip();
                            System.out.println("[Pipe.SourceChannel][Read Data: " + StandardCharsets.UTF_8.decode(byteBuffer) + "]");
                        }
                    }
                    keyItr.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
