package me.seakeer.learning.javase.iostream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * ByteCharIoStreamExample;
 * 字节流 字符流 示例
 *
 * @author Seakeer;
 * @date 2024/7/11;
 */
public class ByteCharIoStreamExample {

    public static void main(String[] args) throws IOException {
        byteStream();
        charStream();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String nextLine = scanner.nextLine();
            System.out.println(nextLine);
        }
    }

    private static void charStream() {
        try (Writer writer = new FileWriter("IoStream.txt");
             Reader reader = new FileReader("IoStream.txt")) {
            writer.write("Hello World Char Stream");
            writer.flush();

            char[] chars = new char[64];
            int numRead;
            while ((numRead = reader.read(chars)) != -1) {
                System.out.println(new String(chars, 0, numRead));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void byteStream() {
        try (OutputStream outputStream = new FileOutputStream("IoStream.txt");
             InputStream inputStream = new FileInputStream("IoStream.txt")) {

            outputStream.write("Hello World Byte Stream".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            byte[] bytes = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(bytes)) != -1) {
                System.out.println(new String(bytes, 0, bytesRead, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
