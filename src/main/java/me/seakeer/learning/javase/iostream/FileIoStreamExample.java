package me.seakeer.learning.javase.iostream;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * FileIoStreamExample;
 * 文件流
 *
 * @author Seakeer;
 * @date 2024/7/24;
 */
public class FileIoStreamExample {

    public static void main(String[] args) {
        File file = new File("IoStream.txt");
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            randomAccessFile.seek(randomAccessFile.length());
            randomAccessFile.writeBytes("\nHello World");
            randomAccessFile.seek(0);
            int read;
            while ((read = randomAccessFile.read()) != -1) {
                System.out.print((char) read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
