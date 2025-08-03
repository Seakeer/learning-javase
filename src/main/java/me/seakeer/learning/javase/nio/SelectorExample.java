package me.seakeer.learning.javase.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * SelectorExample;
 *
 * @author Seakeer;
 * @date 2024/10/15;
 */
public class SelectorExample {

    public static void main(String[] args) {
        new Thread(() -> server("127.0.0.1", 9999)).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> client("127.0.0.1", 9999)).start();
    }

    private static void server(String host, int port) {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(host, port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("[Server] [Started]");
            while (true) {
                try {
                    int readyOpsNum = selector.select();
                    System.out.println("[ServerSelector] [select ready ops num: " + readyOpsNum + "]");
                    if (readyOpsNum <= 0) {
                        continue;
                    }
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    printServerSelectionKeys(selectionKeys);
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();
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
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void client(String serverHost, int serverPort) {
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
                    int readyOpsNum = selector.select();
                    System.out.println("[ClientSelector] [select ready ops num: " + readyOpsNum + "]");
                    if (readyOpsNum <= 0) {
                        continue;
                    }
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    printClientSelectionKeys(selectionKeys);
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

    private static void printServerSelectionKeys(Set<SelectionKey> selectionKeys) {
        System.out.println("[ServerSelector] [selection keys num: " + selectionKeys.size() + "]");
        SelectionKey[] keyArr = selectionKeys.toArray(new SelectionKey[0]);
        for (int i = 0; i < keyArr.length; i++) {
            System.out.printf("[ServerSelector] [selectionKey[%d]][ready ops: %d, isValid: %b, isAcceptable: %s, isReadable: %s, isWriteable: %s]\n",
                    i, keyArr[i].readyOps(), keyArr[i].isValid(), keyArr[i].isAcceptable(), keyArr[i].isReadable(), keyArr[i].isWritable());
        }
    }

    private static void printClientSelectionKeys(Set<SelectionKey> selectionKeys) {
        System.out.println("[ClientSelector] [selection keys num: " + selectionKeys.size() + "]");
        SelectionKey[] keyArr = selectionKeys.toArray(new SelectionKey[0]);
        for (int i = 0; i < keyArr.length; i++) {
            System.out.printf("[ClientSelector] [selectionKey[%d]][ready ops: %d, isValid: %b, isConnectable: %s, isReadable: %s, isWriteable: %s]\n",
                    i, keyArr[i].readyOps(), keyArr[i].isValid(), keyArr[i].isConnectable(), keyArr[i].isReadable(), keyArr[i].isWritable());
        }
    }
}
