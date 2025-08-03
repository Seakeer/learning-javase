package me.seakeer.learning.javase.other.endecode;

import java.nio.charset.StandardCharsets;

/**
 * EnDecodeExample;
 *
 * @author Seakeer;
 * @date 2025/1/4;
 */
public class EnDecodeExample {
    public static void main(String[] args) {
        String str = "https://www.vojivo.xyz/tool/jsonOne?jsonRecordId=1&no=.-_~";
        String base64Str = Base64EnDecoder.encode(str.getBytes(StandardCharsets.UTF_8));
        System.out.println("Base64: " + base64Str);
        String hexStr = HexEnDecoder.encode(str.getBytes(StandardCharsets.UTF_8));
        System.out.println("Hex: " + hexStr);
        String urlStr = UrlEnDecoder.encode(str);
        System.out.println("Url: " + urlStr);
    }
}
