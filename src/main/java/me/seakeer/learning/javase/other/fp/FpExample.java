package me.seakeer.learning.javase.other.fp;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * FpExample;
 * 函数式编程
 *
 * @author Seakeer;
 * @date 2024/8/15;
 */
public class FpExample {

    public static void main(String[] args) {

        lambdaExpressionExample();

        methodReferenceExample();

        streamApiExample();
    }

    private static void lambdaExpressionExample() {
        // 匿名内部类方式
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("HelloWorld");
            }
        });

        // Lambda表达式
        new Thread(() -> System.out.println("HelloWorld"));
    }

    /**
     * 方法引用示例
     */
    private static void methodReferenceExample() {
        // Function<String, Integer> str2IntFunc = str -> Integer.parseInt(str);
        // 静态方法引用
        Function<String, Integer> str2IntFunc = Integer::parseInt;


        // Function<String, Integer> strLengthFunc = str -> str.length();
        // 类任意对象的方法引用
        Function<String, Integer> strLengthFunc = String::length;


        List<String> strList = Arrays.asList("6", "8", "9");
        // Function<String, Boolean> eleOfListFunc = str -> strList.contains(str);
        // 实例方法引用
        Function<String, Boolean> eleOfListFunc = strList::contains;

        // Function<String, Exception> excNewFunc = msg -> new Exception(msg);
        // 构造方法引用
        Function<String, Exception> excNewFunc = Exception::new;
    }

    private static void streamApiExample() {

        List<String> strList = Arrays.asList("6", "8", "9");

    }
}
