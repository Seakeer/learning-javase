package me.seakeer.learning.javase.datastructure.collections;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CollectionsExample;
 *
 * @author Seakeer;
 * @date 2024/12/15;
 */
public class CollectionsExample {

    public static void main(String[] args) {
        List<Integer> list = IntStream.range(1, 10).boxed().collect(Collectors.toList());

        Collections.rotate(list, 4);
        System.out.println("Collections.rotate:  " + list);

        Collections.sort(list);
        System.out.println("Collections.sort:    " + list);
        System.out.println("Collections.binarySearch 1: " + Collections.binarySearch(list, 1));


        Collections.reverse(list);
        System.out.println("Collections.reverse: " + list);

        Collections.swap(list, 0, 8);
        System.out.println("Collections.swap:    " + list);

        Collections.shuffle(list);
        System.out.println("Collections.shuffle: " + list);

        Map<Integer, Integer> map = new HashMap<Integer, Integer>() {
            {
                put(1, 1);
            }

            {
                put(2, 2);
            }

            {
                put(3, 3);
            }
        };

        Collections.synchronizedMap(map);
        Collections.unmodifiableMap(map);
    }
}
