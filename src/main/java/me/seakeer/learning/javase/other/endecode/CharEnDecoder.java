package me.seakeer.learning.javase.other.endecode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * CharEnDecoder;
 *
 * @author Seakeer;
 * @date 2025/1/4;
 */
public class CharEnDecoder {
    public static String encode(byte[] bytes, Charset charset) {
        return new String(bytes, charset);
    }

    public static byte[] decode(String str, Charset charset) {
        return str.getBytes(charset);
    }

    public static void main(String[] args) {
        String str = "Z";
        byte[] bytes = decode(str, StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            System.out.println(Integer.toBinaryString(bytes[i]));
        }

        System.out.println(HexEnDecoder.encode(bytes));
    }
}
