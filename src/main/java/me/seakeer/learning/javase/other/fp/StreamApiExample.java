package me.seakeer.learning.javase.other.fp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * StreamApiExample;
 *
 * @author Seakeer;
 * @date 2024/8/20;
 */
public class StreamApiExample {

    public static void main(String[] args) {

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        // 遍历打印 循环的方式
        for (Integer num : list) {
            System.out.print(num);
        }
        System.out.println();
        // 遍历打印 StreamAPI方式
        list.stream().forEach(System.out::print);
        System.out.println();

        Map<Integer, String> numSquareNum = list.stream()
                // 过滤 --- 保留符合条件的元素
                .filter(num -> num % 2 == 0)
                // 去重
                .distinct()
                // 映射
                .map(num -> num * num)
                // 收集器收集到Map
                .collect(Collectors.toMap(num -> num, String::valueOf));
        System.out.println(numSquareNum);

        // reduce 归约，将流中的元素组合成一个单一的结果，可用于求和、求积、求最值等。
        System.out.println("求积：" + list.stream().reduce((a, b) -> a * b).get());
        System.out.println("求和：" + list.stream().reduce(0, Integer::sum));
        System.out.println("求最大值: " + +list.stream().reduce(Integer::max).get());

        // 并行流
        list.parallelStream().forEach(System.out::print);
        System.out.println();
        list.stream().parallel().forEach(System.out::print);
    }
}
