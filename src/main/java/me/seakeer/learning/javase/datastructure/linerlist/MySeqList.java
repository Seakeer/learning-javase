package me.seakeer.learning.javase.datastructure.linerlist;

/**
 * 顺便表
 * MySeqList;
 *
 * @author Seakeer;
 * @date 2024/12/1;
 */
public class MySeqList<E> {

    /**
     * 数据域
     */
    private E[] data;

    /**
     * 容量
     */
    private int capacity;

    /**
     * 表尾标识
     */
    private int lastIdx;

    public MySeqList(int capacity) {
        this.capacity = capacity;
        data = (E[]) new Object[capacity];
        lastIdx = -1;
    }

    public boolean insert(int idx, E data) {
        if (idx < 0 || idx > lastIdx + 1) {
            return false;
        }
        for (int i = lastIdx; i >= idx; i--) {
            this.data[i + 1] = this.data[i];
        }
        this.data[idx] = data;
        lastIdx++;
        return true;
    }

    public boolean remove(int idx) {
        if (idx < 0 || idx > lastIdx) {
            return false;
        }
        for (int i = idx; i < lastIdx; i++) {
            data[i] = data[i + 1];
        }
        lastIdx--;
        data[lastIdx + 1] = null;
        return true;
    }

    public E get(int idx) {
        if (idx >= 0 && idx <= lastIdx) {
            return data[idx];
        }
        return null;
    }

    public int size() {
        return lastIdx + 1;
    }

    public boolean isEmpty() {
        return lastIdx == -1;
    }

    public void clear() {
        data = (E[]) new Object[capacity];
        lastIdx = -1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i <= lastIdx; i++) {
            sb.append(data[i]);
            if (i != lastIdx) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static void main(String[] args) {
        MySeqList<Integer> list = new MySeqList<>(10);
        list.insert(0, 1);
        list.insert(1, 2);
        list.insert(2, 3);
        System.out.println(list);
        list.remove(1);
        System.out.println(list);
    }
}