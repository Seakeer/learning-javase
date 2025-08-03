package me.seakeer.learning.javase.datastructure.linerlist;

/**
 * 链表
 * LinkedList;
 *
 * @author Seakeer;
 * @date 2024/12/1;
 */
public class MyLinkedList<E> {

    /**
     * 链表结点类
     */
    public static class Node<E> {

        /**
         * 数据域
         */
        private E data;

        /**
         * 链接器，指向下一个结点
         */
        private Node<E> next;

        public Node(E data) {
            this.data = data;
            this.next = null;
        }
    }

    /**
     * 表头结点
     */
    private Node<E> head;

    /**
     * 表大小
     */
    private int size;

    public MyLinkedList() {
        head = null;
        size = 0;
    }

    /**
     * 在指定索引位置插入元素
     *
     * @param idx  插入位置的索引
     * @param data 要插入的数据
     * @return 插入成功返回 true，否则返回 false
     */
    public boolean insert(int idx, E data) {
        if (idx < 0 || idx > size) {
            return false;
        }

        Node<E> newNode = new Node<>(data);
        if (idx == 0) {
            newNode.next = head;
            head = newNode;
        } else {
            Node<E> current = head;
            for (int i = 0; i < idx - 1; i++) {
                current = current.next;
            }
            newNode.next = current.next;
            current.next = newNode;
        }

        size++;
        return true;
    }

    /**
     * 删除指定索引位置的元素
     *
     * @param idx 要删除的元素的索引
     * @return 删除成功返回 true，否则返回 false
     */
    public boolean remove(int idx) {
        if (idx < 0 || idx >= size) {
            return false;
        }

        if (idx == 0) {
            head = head.next;
        } else {
            Node<E> current = head;
            for (int i = 0; i < idx - 1; i++) {
                current = current.next;
            }
            current.next = current.next.next;
        }

        size--;
        return true;
    }

    /**
     * 获取指定索引位置的元素
     *
     * @param idx 要获取的元素的索引
     * @return 返回指定索引位置的元素，如果索引无效返回 null
     */
    public E get(int idx) {
        if (idx < 0 || idx >= size) {
            return null;
        }

        Node<E> current = head;
        for (int i = 0; i < idx; i++) {
            current = current.next;
        }

        return current.data;
    }

    /**
     * 清空链表
     */
    public void clear() {
        head = null;
        size = 0;
    }

    /**
     * 获取链表的大小
     *
     * @return 链表的大小
     */
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Node node = this.head;
        while (node != null) {
            sb.append(node.data);
            node = node.next;
            if (node != null) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static void main(String[] args) {
        MyLinkedList<Integer> linkedList = new MyLinkedList<>();
        linkedList.insert(0, 1);
        linkedList.insert(1, 2);
        linkedList.insert(2, 3);
        System.out.println(linkedList);
        linkedList.remove(1);
        System.out.println(linkedList);
    }
}
