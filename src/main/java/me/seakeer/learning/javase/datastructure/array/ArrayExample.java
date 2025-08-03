package me.seakeer.learning.javase.datastructure.array;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * ArrayExample;
 *
 * @author Seakeer;
 * @date 2024/8/22;
 */
public class ArrayExample {

    public static void main(String[] args) {
        // 数组使用示例
        useArray();
        // 多维数组
        arr2d();

        // Arrays类
        arrays();
        // Array类
        array();
    }

    private static void array() {
        // 创建一个长度为3的字符串数组
        Object array = Array.newInstance(String.class, 3);
        // 赋值
        Array.set(array, 0, "I");
        Array.set(array, 1, "Love");
        Array.set(array, 2, "It");
        // 获取
        for (int i = 0; i < 3; i++) {
            System.out.println((String) Array.get(array, i));
        }
    }

    private static void arrays() {
        System.out.println(Arrays.toString(new String[]{"A", "B", "C"}));
        System.out.println(Arrays.toString(new String[]{"A", "B", "C"}));
        System.out.println(Arrays.toString(new String[][]{{"A", "B", "C"}, {"D", "E", "F"}}));
        System.out.println(Arrays.deepToString(new String[][]{{"A", "B", "C"}, {"D", "E", "F"}}));
        System.out.println(Arrays.binarySearch(new int[]{1, 2}, 10));
    }

    private static void arr2d() {
        int[][] arrInt2d = new int[2][3];
        arrInt2d[0][0] = 1;
        arrInt2d[1][2] = 2;

        Long[][] arrLong2d = {
                {11L, 12L, 13L},
                {21L, 22L, 23L}
        };
        for (Long[] arr : arrLong2d) {
            for (Long num : arr) {
                System.out.println(num);
            }
        }

        for (int i = 0; i < arrInt2d.length; i++) {
            for (int j = 0; j < arrInt2d[i].length; j++) {
                System.out.println(arrInt2d[i][j]);
            }
        }
    }

    private static void useArray() {
        // 声明创建数组 隐式初始化
        int[] arrInt = new int[3];
        arrInt[0] = 1;
        arrInt[1] = 2;
        arrInt[2] = 3;

        // 显式初始化
        Long[] arrLong = {10L, 20L, 30L};

        // 数组长度
        System.out.println(arrLong.length);

        // 遍历数组
        for (Long num : arrLong) {
            System.out.println(num);
        }

        // 访问数组元素
        System.out.println(arrInt[1]);

        System.out.println(Arrays.toString(arrInt));
    }
}
