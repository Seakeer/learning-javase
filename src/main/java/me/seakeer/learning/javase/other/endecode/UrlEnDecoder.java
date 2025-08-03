package me.seakeer.learning.javase.other.endecode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * UrlEnDecoder;
 *
 * @author Seakeer;
 * @date 2025/1/4;
 */
public class UrlEnDecoder {

    /**
     * 将字符串进行 URL 编码
     *
     * @param url 要编码的字符串
     * @return 编码后的字符串
     */
    public static String encode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 将字符串进行 URL 解码
     *
     * @param url 要解码的字符串
     * @return 解码后的字符串
     */
    public static String decode(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

}
