package me.seakeer.learning.javase.datastructure.linerlist;

/**
 * 顺序栈
 * MyStack;
 *
 * @author Seakeer;
 * @date 2024/12/1;
 */
public class MySeqStack<E> {

    /**
     * 数据域
     */
    private E data[];

    /**
     * 栈顶标识
     */
    private int topIdx;

    /**
     * 栈容量
     */
    private int capacity;

    public MySeqStack(int capacity) {
        this.capacity = capacity;
        data = (E[]) new Object[capacity];
        topIdx = -1;
    }

    public boolean isEmpty() {
        return topIdx == -1;
    }

    public boolean isFull() {
        return topIdx == capacity - 1;
    }

    /**
     * 入栈
     */
    public boolean push(E item) {
        if (isFull()) {
            return false;
        }
        data[++topIdx] = item;
        return true;
    }

    /**
     * 出栈
     */
    public E pop() {
        return data[topIdx--];
    }

    /**
     * 获取栈顶元素
     */
    public E peek() {
        return data[topIdx];
    }

    public int size() {
        return topIdx + 1;
    }

    public void clear() {
        topIdx = -1;
        data = (E[]) new Object[capacity];
    }

    public static void main(String[] args) {
        MySeqStack<Integer> stack = new MySeqStack<>(3);
        for (int i = 0; i < 3; i++) {
            stack.push(i);
        }
        System.out.println(stack.peek());
        System.out.println(stack.pop());
        System.out.println(stack.peek());
    }
}
