package me.seakeer.learning.javase.other.generictype;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GenericTypeExample;
 *
 * @author Seakeer;
 * @date 2024/8/10;
 */
public class GenericTypeExample {

    public static <In, Out> List<Out> convert(List<In> list, Function<In, Out> mapper) {
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        GenericClass<String, List<Integer>> gc = new GenericClass<>();
        gc.setKey("key1");
        gc.setData(Arrays.asList(1, 2, 3));
        System.out.println(gc);

        System.out.println(GenericTypeExample.convert(Arrays.asList(1, 2, 3), String::valueOf));
    }
}
