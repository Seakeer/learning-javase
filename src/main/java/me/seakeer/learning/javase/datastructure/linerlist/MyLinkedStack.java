package me.seakeer.learning.javase.datastructure.linerlist;

/**
 * 链栈
 * MyLinkedStack;
 *
 * @author Seakeer;
 * @date 2024/12/1;
 */
public class MyLinkedStack<E> {

    public static class Node<E> {

        private E data;

        private Node<E> next;

        public Node(E data, Node<E> next) {
            this.data = data;
            this.next = next;
        }
    }

    /**
     * 栈顶
     */
    private Node<E> top;

    /**
     * 栈长度
     */
    private int size;

    public MyLinkedStack() {
        top = null;
        size = 0;
    }

    public boolean push(E data) {
        top = new Node<>(data, top);
        size++;
        return true;
    }

    public E pop() {
        if (top == null) return null;
        E data = top.data;
        top = top.next;
        size--;
        return data;
    }

    public E peek() {
        return top == null ? null : top.data;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        top = null;
        size = 0;
    }

    public static void main(String[] args) {
        MyLinkedStack<Integer> stack = new MyLinkedStack<>();
        for (int i = 0; i < 3; i++) {
            stack.push(i);
        }
        System.out.println(stack.peek());
        System.out.println(stack.pop());
        System.out.println(stack.peek());
    }

}
