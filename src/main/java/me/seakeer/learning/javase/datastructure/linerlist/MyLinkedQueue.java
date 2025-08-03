package me.seakeer.learning.javase.datastructure.linerlist;

/**
 * 链式队列
 * MyLinkedQueue;
 *
 * @author Seakeer;
 * @date 2024/12/1;
 */
public class MyLinkedQueue<E> {

    public static class Node<E> {

        private E data;

        private Node next;

        public Node(E data) {
            this.data = data;
        }
    }

    /**
     * 队头
     */
    private Node<E> head;

    /**
     * 队尾
     */
    private Node<E> tail;

    /**
     * 队列长度
     */
    private int size;


    public MyLinkedQueue() {
        head = null;
        tail = null;
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 入队
     *
     * @param data 要入队的元素
     * @return 入队成功返回 true
     */
    public boolean enqueue(E data) {
        Node<E> newNode = new Node<>(data);
        if (tail == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
        return true;
    }

    /**
     * 出队
     *
     * @return 返回出队的元素，如果队列为空返回 null
     */
    public E dequeue() {
        if (head == null) {
            return null;
        }
        E data = head.data;
        head = head.next;

        if (head == null) {
            tail = null;
        }
        size--;
        return data;
    }

    /**
     * 获取队头元素
     *
     * @return
     */
    public E peek() {
        if (null == head) {
            return null;
        }
        return head.data;
    }

    public int size() {
        return size;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    public static void main(String[] args) {
        MyLinkedQueue<Integer> queue = new MyLinkedQueue<>();
        for (int i = 0; i < 3; i++) {
            queue.enqueue(i);
        }
        System.out.println(queue.peek());
        System.out.println(queue.dequeue());
        System.out.println(queue.peek());
    }

}
