package me.seakeer.learning.javase.datastructure.linerlist;

import java.util.Random;

/**
 * 跳表
 * MySkipList;
 *
 * @author Seakeer;
 * @date 2024/12/1;
 */

public class MySkipList<E extends Comparable<E>> {

    private static final int DEFAULT_MAX_LEVEL = 32;

    private static final double DEFAULT_P = 0.25;

    private static class Node<E> {

        /**
         * 数据域
         */
        private E data;

        /**
         * 链接器数组，指向当前结点的下一层结点
         */
        private Node<E>[] forward;

        public Node(E data, int level) {
            this.data = data;
            this.forward = new Node[level];
        }

    }

    /**
     * 头结点
     */
    private Node<E> head;

    /**
     * 当前层数
     */
    private int level;

    /**
     * 最大层数
     */
    private int maxLevel;

    /**
     * 升层概率
     */
    private double p;

    private final Random RANDOM = new Random();

    public MySkipList() {
        this(DEFAULT_MAX_LEVEL, DEFAULT_P);
    }

    public MySkipList(int maxLevel, double p) {
        if (maxLevel <= 0) {
            this.maxLevel = DEFAULT_MAX_LEVEL;
        } else {
            this.maxLevel = maxLevel;
        }
        if (p <= 0 || p >= 1) {
            this.p = DEFAULT_P;
        } else {
            this.p = p;
        }
        head = new Node<>(null, maxLevel);
        level = 1;
    }

    /**
     * 生成随机层数，指定结点所在的最大层级数
     */
    private int randomLevel(double p, int maxLevel) {
        int lvl = 1;
        while (RANDOM.nextDouble() < p && lvl < maxLevel) {
            lvl++;
        }
        return lvl;
    }


    public void insert(E data) {
        Node<E>[] update = new Node[this.maxLevel];
        Node<E> p = head;

        // 找到每一层的插入位置
        for (int i = level - 1; i >= 0; i--) {
            while (p.forward[i] != null && p.forward[i].data.compareTo(data) < 0) {
                p = p.forward[i];
            }
            update[i] = p;
        }

        // 插入新节点
        int newLevel = randomLevel(this.p, this.maxLevel);
        if (newLevel > level) {
            for (int i = level; i < newLevel; i++) {
                update[i] = head;
            }
            level = newLevel;
        }

        Node<E> newNode = new Node<>(data, newLevel);
        for (int i = 0; i < newLevel; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }
    }

    public boolean contains(E data) {
        Node<E> p = head;
        for (int i = level - 1; i >= 0; i--) {
            while (p.forward[i] != null && p.forward[i].data.compareTo(data) < 0) {
                p = p.forward[i];
            }
            if (p.forward[i] != null && p.forward[i].data.equals(data)) {
                return true;
            }
        }
        return false;
    }


    public void delete(E data) {
        Node<E>[] update = new Node[this.maxLevel];
        Node<E> p = head;

        // 找到每一层的删除位置
        for (int i = level - 1; i >= 0; i--) {
            while (p.forward[i] != null && p.forward[i].data.compareTo(data) < 0) {
                p = p.forward[i];
            }
            update[i] = p;
        }

        // 删除节点
        if (p.forward[0] != null && p.forward[0].data.equals(data)) {
            for (int i = 0; i < level; i++) {
                if (update[i].forward[i] != null && update[i].forward[i].data.equals(data)) {
                    update[i].forward[i] = update[i].forward[i].forward[i];
                }
            }

            // 更新当前层数
            while (level > 1 && head.forward[level - 1] == null) {
                level--;
            }
        }
    }

    public static void main(String[] args) {
        MySkipList<Integer> skipList = new MySkipList<>();
        skipList.insert(3);
        skipList.insert(6);
        skipList.insert(7);
        skipList.insert(9);
        skipList.insert(12);
        skipList.insert(19);
        skipList.insert(17);
        skipList.insert(26);
        skipList.insert(21);
        skipList.insert(25);

        System.out.println(skipList.contains(19)); // 输出: true
        System.out.println(skipList.contains(15)); // 输出: false

        skipList.delete(19);
        System.out.println(skipList.contains(19)); // 输出: false
    }
}

