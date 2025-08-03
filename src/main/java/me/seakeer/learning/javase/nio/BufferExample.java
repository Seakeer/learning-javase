package me.seakeer.learning.javase.nio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * BufferExample;
 *
 * @author Seakeer;
 * @date 2024/10/15;
 */
public class BufferExample {

    private static final int SIZE_1_M = 1024 * 1024;

    public static void main(String[] args) {

        System.out.println("----------Buffer基本使用示例-----------");
        simpleBufferExample();

        System.out.println("----------ByteBuffer示例-----------");
        byteBufferExample();

        System.out.println("----------写入1M数据到文件3种ByteBuffer的耗时情况-----------");
        heapBufferFileExample(1);
        directByteBufferFileExample(1);
        mappedByteBufferFileExample(1);

        System.out.println("----------写入10M数据到文件3种ByteBuffer的耗时情况-----------");

        heapBufferFileExample(10);
        directByteBufferFileExample(10);
        mappedByteBufferFileExample(10);
    }

    private static void byteBufferExample() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.putChar('A');
        byteBuffer.put((byte) 'B');

        CharBuffer charBuffer = byteBuffer.asCharBuffer();
        charBuffer.put('C');
        charBuffer.put('D');

        byteBuffer.flip();
        System.out.println(byteBuffer.getChar());
        System.out.println((char) byteBuffer.get());
        System.out.println(byteBuffer.hasRemaining());

        charBuffer.flip();
        while (charBuffer.hasRemaining()) {
            System.out.println(charBuffer.get());
        }

    }

    private static void heapBufferFileExample(int mb) {
        long start = System.currentTimeMillis();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile("HeapByteBufferExample.txt", "rw")) {
            FileChannel fileChannel = randomAccessFile.getChannel();
            ByteBuffer heapByteBuffer = ByteBuffer.allocate(SIZE_1_M * mb);
            for (int i = 0; i < SIZE_1_M * mb; i++) {
                heapByteBuffer.put((byte) 'A');
            }
            heapByteBuffer.flip();
            fileChannel.write(heapByteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long cost = System.currentTimeMillis() - start;
        System.out.println("HeapByteBuffer cost: " + cost + " ms");
    }

    private static void directByteBufferFileExample(int mb) {
        long start = System.currentTimeMillis();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile("DirectByteBufferExample.txt", "rw")) {
            FileChannel fileChannel = randomAccessFile.getChannel();
            ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(SIZE_1_M * mb);
            for (int i = 0; i < SIZE_1_M * mb; i++) {
                directByteBuffer.put((byte) 'A');
            }
            directByteBuffer.flip();
            fileChannel.write(directByteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long cost = System.currentTimeMillis() - start;
        System.out.println("DirectByteBuffer cost: " + cost + " ms");
    }

    private static void mappedByteBufferFileExample(int mb) {
        long start = System.currentTimeMillis();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile("MappedByteBufferExample.txt", "rw")) {
            FileChannel fileChannel = randomAccessFile.getChannel();
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, (long) SIZE_1_M * mb);
            for (int i = 0; i < SIZE_1_M * mb; i++) {
                mappedByteBuffer.put((byte) 'A');
            }
            mappedByteBuffer.force();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long cost = System.currentTimeMillis() - start;
        System.out.println("MappedByteBuffer cost: " + cost + " ms");
    }

    private static void simpleBufferExample() {
        // 创建一个ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        // 写入数据
        byteBuffer.put("Hello Buffer\n".getBytes());

        // 翻转Buffer
        byteBuffer.flip();

        // 读取数据
        while (byteBuffer.hasRemaining()) {
            System.out.print((char) byteBuffer.get());
        }

        // 清空Buffer
        byteBuffer.clear();
    }
}
