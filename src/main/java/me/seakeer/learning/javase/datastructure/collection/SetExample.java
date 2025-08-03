package me.seakeer.learning.javase.datastructure.collection;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * SetExample;
 *
 * @author Seakeer;
 * @date 2024/11/23;
 */
public class SetExample {

    public static void main(String[] args) {
        Set<Integer> treeSet = new TreeSet<>();
        treeSet.add(2);
        treeSet.add(1);
        treeSet.add(3);
        treeSet.add(1);
        System.out.println(treeSet);

        Set<Integer> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add(2);
        linkedHashSet.add(1);
        linkedHashSet.add(3);
        linkedHashSet.add(1);
        System.out.println(linkedHashSet);

        Set<Integer> hashSet = new HashSet<>();
        hashSet.add(2);
        hashSet.add(1);
        hashSet.add(3);
        hashSet.add(1);
        System.out.println(hashSet);
    }
}
