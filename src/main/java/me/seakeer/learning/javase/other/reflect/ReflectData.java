package me.seakeer.learning.javase.other.reflect;

import java.util.List;

/**
 * ReflectData;
 *
 * @author Seakeer;
 * @date 2024/8/8;
 */
public class ReflectData<T> {

    private String stringField;

    private int intField;

    private List<String> stringListField;

    private List<? extends Number> numberListField;

    private T[] tArrField;

    private List<?>[] listArrField;

}


