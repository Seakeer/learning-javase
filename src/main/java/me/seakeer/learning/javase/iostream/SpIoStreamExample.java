package me.seakeer.learning.javase.iostream;

import java.io.*;

/**
 * SpIoStreamExample;
 *
 * @author Seakeer;
 * @date 2024/10/14;
 */
public class SpIoStreamExample {

    public static void main(String[] args) {

        System.out.println("------------------ 数组流 ------------------");
        byteArrayStream();

        System.out.println("------------------ 缓冲流 ------------------");
        bufferedStream();

        System.out.println("------------------ 管道流 ------------------");
        pipedStream();

        System.out.println("------------------ 回退流 ------------------");
        pushbackStream();

        System.out.println("------------------ 数据流 ------------------");
        dataStream();

        System.out.println("------------------ 顺序流 ------------------");
        sequenceStream();

        System.out.println("------------------ 打印流 ------------------");
        printStream();

        System.out.println("------------------ 字符串流 ------------------");
        stringReader();

        System.out.println("------------------ 行号流 ------------------");
        lineNumberReader();

    }

    private static ByteArrayInputStream genByteArrayInputStream() {
        String text = "Hello World! \n" +
                "Here are examples of Java IO Stream.\n";
        return new ByteArrayInputStream(text.getBytes());
    }

    private static void byteArrayStream() {
        try (ByteArrayInputStream bis = genByteArrayInputStream()) {
            int data;
            while ((data = bis.read()) != -1) {
                System.out.print((char) data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void bufferedStream() {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(genByteArrayInputStream())) {
            int data;
            while ((data = bufferedInputStream.read()) != -1) {
                System.out.print((char) data);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void pipedStream() {
        try (PipedInputStream pipedInputStream = new PipedInputStream();
             PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream)) {

            Thread outThread = new Thread(() -> {
                try {
                    String text = "Hello World! \n" +
                            "Here are examples of Java IO Stream.\n";
                    pipedOutputStream.write(text.getBytes());
                    pipedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Thread inThread = new Thread(() -> {
                try {
                    int data;
                    while ((data = pipedInputStream.read()) != -1) {
                        System.out.print((char) data);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            outThread.start();
            inThread.start();
            outThread.join();
            inThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void pushbackStream() {
        try (PushbackInputStream pushbackInputStream = new PushbackInputStream(genByteArrayInputStream())) {
            int data;
            while ((data = pushbackInputStream.read()) != -1) {
                System.out.print((char) data);
                if (data == 'o') {
                    // 将 'o' 推回
                    pushbackInputStream.unread(data);
                    break;
                }
            }
            while ((data = pushbackInputStream.read()) != -1) {
                System.out.print((char) data);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void dataStream() {
        try (DataInputStream dis = new DataInputStream(genByteArrayInputStream())) {
            int readInt = dis.readInt();
            boolean readBoolean = dis.readBoolean();
            System.out.println("int value: " + readInt);
            System.out.println("boolean value: " + readBoolean);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sequenceStream() {
        try (SequenceInputStream sis = new SequenceInputStream(genByteArrayInputStream(),
                genByteArrayInputStream())) {
            int data;
            while ((data = sis.read()) != -1) {
                System.out.print((char) data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printStream() {
        //不使用try-with-source, 否则会关闭System.out，导致后续打印失败
        PrintStream printStream = System.out;
        PrintWriter pw = new PrintWriter(printStream);
        printStream.println("Hello World!");
        pw.println("Here are examples of Java IO Stream.");
        pw.flush();
    }

    private static void stringReader() {
        String text = "Hello World! \n" +
                "Here are examples of Java IO Stream.\n";
        try (StringReader sr = new StringReader(text)) {
            int data;
            while ((data = sr.read()) != -1) {
                System.out.print((char) data);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void lineNumberReader() {
        String text = "Hello World! \n" +
                "Here are examples of Java IO Stream.\n";
        try (LineNumberReader lnr = new LineNumberReader(new StringReader(text))) {
            String line;
            while ((line = lnr.readLine()) != null) {
                System.out.println("Line " + lnr.getLineNumber() + ": " + line);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
