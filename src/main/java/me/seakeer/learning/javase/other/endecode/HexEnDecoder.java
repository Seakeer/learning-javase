package me.seakeer.learning.javase.other.endecode;

/**
 * HexEnDecoder;
 *
 * @author Seakeer;
 * @date 2025/1/4;
 */
public class HexEnDecoder {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * 将字节数组编码为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String encode(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * 将十六进制字符串解码为字节数组
     *
     * @param hex 十六进制字符串
     * @return 字节数组
     */
    public static byte[] decode(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even number of characters");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
