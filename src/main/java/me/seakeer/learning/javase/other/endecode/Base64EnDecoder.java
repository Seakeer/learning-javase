package me.seakeer.learning.javase.other.endecode;

import java.util.Base64;

/**
 * Base64EnDecoder;
 *
 * @author Seakeer;
 * @date 2025/1/4;
 */
public class Base64EnDecoder {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    public static String encode(byte[] bytes) {
        return ENCODER.encodeToString(bytes);
    }

    public static byte[] decode(String str) {
        return DECODER.decode(str);
    }
}
