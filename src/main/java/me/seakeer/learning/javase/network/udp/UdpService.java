package me.seakeer.learning.javase.network.udp;

import me.seakeer.learning.javase.network.MsgEnDecoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * UdpService;
 * UDP服务，支持发送和接收数据报
 *
 * @author Seakeer;
 * @date 2024/10/19;
 */
public class UdpService {

    public static final String UDP_SERVICE_LOG = "[UdpService] ";

    public static final String CMD_LIST = "[CmdList: START, START $PORT, RESTART, STOP, SHUTDOWN, TO $HOSTNAME:$PORT $DATA]";

    private volatile DatagramChannel datagramChannel;

    private volatile Selector selector;

    private int port;

    private static final int DEFAULT_PORT = 8888;

    private volatile boolean running = false;

    private static final ConcurrentLinkedQueue<Datagram> SENDING_DATAGRAM_QUEUE = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        UdpService udpService = new UdpService();
        cmd(udpService);
    }

    private static void cmd(UdpService udpService) {
        System.out.println(UDP_SERVICE_LOG + "[UdpService Ready] [Please Input Cmd] " + CMD_LIST);
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                String cmd = scanner.nextLine();
                switch (cmd) {
                    case "START":
                        new Thread(udpService::start).start();
                        break;
                    case "STOP":
                        udpService.stop();
                        break;
                    case "RESTART":
                        new Thread(udpService::restart).start();
                        break;
                    case "SHUTDOWN":
                        udpService.stop();
                        return;
                    default:
                        if (cmd.startsWith("START ")) {
                            String[] cmdParts = cmd.split(" ", 2);
                            new Thread(() -> udpService.start(Integer.parseInt(cmdParts[1]))).start();
                        } else if (cmd.startsWith("TO ")) {
                            String[] cmdParts = cmd.split(" ", 3);
                            if (cmdParts.length < 3) {
                                System.out.printf(UDP_SERVICE_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                                continue;
                            }
                            String[] hostnameAndPort = cmdParts[1].split(":");
                            if (hostnameAndPort.length != 2) {
                                System.out.printf(UDP_SERVICE_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                            } else {
                                udpService.send(hostnameAndPort[0], Integer.parseInt(hostnameAndPort[1]), cmdParts[2]);
                            }
                        } else {
                            System.out.printf(UDP_SERVICE_LOG + "[InvalidCmd] [Cmd: %s] " + CMD_LIST + "\n", cmd);
                        }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        start(DEFAULT_PORT);
    }

    public void start(int port) {
        if (running) {
            System.out.println(UDP_SERVICE_LOG + "[AlreadyRunning] [Port: " + this.port + "]");
            return;
        }
        System.out.println(UDP_SERVICE_LOG + "[Starting]");
        init(port);
        run();
    }

    public void stop() {
        System.out.println(UDP_SERVICE_LOG + "[Stopping]");
        running = false;
        SENDING_DATAGRAM_QUEUE.clear();
        try {
            if (null != datagramChannel) {
                datagramChannel.close();
                datagramChannel = null;
            }
            if (null != selector) {
                selector.close();
                selector = null;
            }
            System.out.println(UDP_SERVICE_LOG + "[Stopped]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restart() {
        stop();
        start(this.port == 0 ? DEFAULT_PORT : this.port);
    }

    private void init(int port) {
        try {
            this.selector = Selector.open();
            this.port = port;
            this.datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.bind(new InetSocketAddress(this.port));
            datagramChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run() {
        running = true;
        System.out.println(UDP_SERVICE_LOG + "[Running] [Port: " + this.port + "]");
        while (running) {
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
                    if (selectionKey.isValid() && selectionKey.isReadable()) {
                        handleOpRead();
                    }
                    if (selectionKey.isValid() && selectionKey.isWritable()) {
                        handleOpWrite();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleOpRead() {
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            InetSocketAddress addr;
            while ((addr = (InetSocketAddress) datagramChannel.receive(buffer)) != null) {
                buffer.flip();
                String data = MsgEnDecoder.decodeMsg(buffer);
                receive(new Datagram(addr.getHostName(), addr.getPort(), data));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleOpWrite() {
        Datagram datagram;
        while ((datagram = SENDING_DATAGRAM_QUEUE.peek()) != null) {
            if (send(datagram)) {
                SENDING_DATAGRAM_QUEUE.poll();
            } else {
                break;
            }
        }
    }

    private void receive(Datagram datagram) {
        System.out.println(UDP_SERVICE_LOG + "[Received Datagram] " + datagram);
    }

    public boolean send(String hostname, int port, String data) {
        return send(new Datagram(hostname, port, data));
    }

    public boolean send(Datagram datagram) {
        if (write(datagram)) {
            return true;
        }
        SENDING_DATAGRAM_QUEUE.offer(datagram);
        return true;
    }

    private boolean write(Datagram datagram) {
        ByteBuffer byteBuffer = MsgEnDecoder.encodeMsg(datagram.getData());
        SocketAddress socketAddress = new InetSocketAddress(datagram.getHostname(), datagram.getPort());
        try {
            while (byteBuffer.hasRemaining()) {
                int writeBytes = datagramChannel.send(byteBuffer, socketAddress);
                if (writeBytes <= 0) {
                    return false;
                }
            }
            System.out.println(UDP_SERVICE_LOG + "[Send Datagram] " + datagram);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class Datagram {
        private String hostname;
        private int port;
        private String data;

        public Datagram(String hostname, int port, String data) {
            this.hostname = hostname;
            this.port = port;
            this.data = data;
        }

        public String getHostname() {
            return hostname;
        }

        public Datagram setHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public int getPort() {
            return port;
        }

        public Datagram setPort(int port) {
            this.port = port;
            return this;
        }

        public String getData() {
            return data;
        }

        public Datagram setData(String data) {
            this.data = data;
            return this;
        }

        @Override
        public String toString() {
            return String.format("[Hostname: %s, Port: %d, Data: %s]", hostname, port, data);
        }
    }
}
