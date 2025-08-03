package me.seakeer.learning.javase.datastructure.linerlist;

/**
 * 顺序队列
 * MySeqQueue;
 *
 * @author Seakeer;
 * @date 2024/12/1;
 */
public class MySeqQueue<E> {

    /**
     * 数据域
     */
    private E data[];

    /**
     * 队头标识
     */
    private int frontIdx;

    /**
     * 队尾标识
     */
    private int rearIdx;

    /**
     * 队列容量
     */
    private int capacity;

    public MySeqQueue(int capacity) {
        this.capacity = capacity;
        data = (E[]) new Object[capacity];
        frontIdx = 0;
        rearIdx = 0;
    }

    public boolean isEmpty() {
        return frontIdx == rearIdx;
    }

    public boolean isFull() {
        return (rearIdx + 1) % capacity == frontIdx;
    }

    /**
     * 入队
     */
    public boolean enqueue(E item) {
        if (isFull()) {
            return false;
        }
        data[rearIdx] = item;
        rearIdx = (rearIdx + 1) % capacity;
        return true;
    }

    /**
     * 出队
     */
    public E dequeue() {
        if (isEmpty()) {
            return null;
        }
        E item = data[frontIdx];
        frontIdx = (frontIdx + 1) % capacity;
        return item;
    }

    /**
     * 获取队头元素
     *
     * @return
     */
    public E peek() {
        return data[frontIdx];
    }

    public int size() {
        return (rearIdx - frontIdx + capacity) % capacity;
    }

    public void clear() {
        frontIdx = rearIdx = 0;
        data = (E[]) new Object[capacity];
    }

    public static void main(String[] args) {
        MySeqQueue<Integer> queue = new MySeqQueue<>(5);
        for (int i = 0; i < 3; i++) {
            queue.enqueue(i);
        }
        System.out.println(queue.peek());
        System.out.println(queue.dequeue());
        System.out.println(queue.peek());
    }
}
