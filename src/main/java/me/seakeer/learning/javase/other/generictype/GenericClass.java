package me.seakeer.learning.javase.other.generictype;

/**
 * GenericClass;
 *
 * @author Seakeer;
 * @date 2024/8/14;
 */
public class GenericClass<K, D> implements GenericInterface<D> {

    private D data;
    private K key;

    @Override
    public D getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key + " = " + data;
    }
}
