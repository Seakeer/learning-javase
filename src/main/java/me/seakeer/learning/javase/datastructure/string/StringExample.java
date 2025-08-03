package me.seakeer.learning.javase.datastructure.string;

/**
 * StringExample;
 *
 * @author Seakeer;
 * @date 2024/8/23;
 */
public class StringExample {

    public static void main(String[] args) {
        // 字符串对象 & StringPool
        String str1 = new String("s");
        String str2 = "s";
        String str3 = new String("s");
        String str4 = str1.intern();
        System.out.println(str1 == str3);
        System.out.println(str2 == str3);
        System.out.println(str2 == str4);

        // 多线程安全的字符串StringBuffer
        StringBuffer sb = new StringBuffer("s");
        sb.append(1);
        System.out.println(sb);

        // 可变长字符串类StringBuilder
        StringBuilder stringBuilder = new StringBuilder("3456789");
        stringBuilder.insert(0, "012");
        stringBuilder.delete(5, 6);

        stringBuilder.replace(0, 1, "999");
        System.out.println(stringBuilder);
    }
}
