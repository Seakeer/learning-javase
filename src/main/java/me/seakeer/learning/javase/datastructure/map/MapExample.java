package me.seakeer.learning.javase.datastructure.map;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * MapExample;
 *
 * @author Seakeer;
 * @date 2024/11/21;
 */
public class MapExample {
    public static void main(String[] args) {
        Map<Integer, Object> map = new LinkedHashMap<>(8);
        for (int i = 0; i < 14; i++) {
            map.put(i, i);
        }
        map.put(14, 14);


        Map<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "1");

        Map<Integer, String> skipListMap = new ConcurrentSkipListMap<>();

    }
}
