package me.seakeer.learning.javase.other.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RegexExample;
 *
 * @author Seakeer;
 * @date 2025/8/3;
 */
public class RegexExample {


    private static final String EMAIL_REGEX = "([a-zA-Z0-9]([a-zA-Z0-9._-])*[a-zA-Z0-9])@([a-zA-Z0-9]([a-zA-Z0-9-])*[a-zA-Z0-9])\\.(([a-zA-Z]{2,4})+)";

    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public static void main(String[] args) {
        String text = "%787237788*Seakeer@163.com902392309fun@js.cn";
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        System.out.println("matches: " + matcher.matches());
        matcher.reset();
        while (matcher.find()) {
            System.out.println("Found value: " + matcher.group() + "---" + matcher.start() + "  " + matcher.end());
            for (int i = 0; i < matcher.groupCount(); i++) {
                System.out.println("Group " + i + ": " + matcher.group(i));
            }
        }
    }
}
