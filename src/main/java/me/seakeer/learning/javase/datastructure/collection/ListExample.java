package me.seakeer.learning.javase.datastructure.collection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * ListExample;
 *
 * @author Seakeer;
 * @date 2024/8/26;
 */
public class ListExample {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {

        // ArrayList 扩容机制 1.5倍
        arrayListGrowth();
    }

    private static void arrayListGrowth() {
        List<Integer> arrayList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            arrayList.add(i);
        }
        System.out.println(getCapacity(arrayList));
        arrayList.add(11);
        System.out.println(getCapacity(arrayList));
        for (int i = 12; i <= 21; i++) {
            arrayList.add(i);
        }
        System.out.println(getCapacity(arrayList));
    }

    private static int getCapacity(List<?> arrayListOrVector) {
        try {
            Field elementData = arrayListOrVector.getClass().getDeclaredField("elementData");
            elementData.setAccessible(true);
            Object[] o = (Object[]) elementData.get(arrayListOrVector);
            return o.length;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

}
