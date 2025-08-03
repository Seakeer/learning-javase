package me.seakeer.learning.javase.datastructure.tree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * BinaryHeap;
 *
 * @author Seakeer;
 * @date 2024/11/25;
 */
public class MyBinaryHeap<E extends Comparable<E>> {

    /**
     * 存储数据的列表，顺序存储使用ArrayList;
     * 可使用数组，但需要考虑扩容问题;
     */
    private final List<E> dataList;

    private final boolean minHeap;

    /**
     * 默认构造函数，创建一个最大堆
     */
    public MyBinaryHeap() {
        this(false);
    }

    public MyBinaryHeap(boolean minHeap) {
        this.dataList = new ArrayList<>();
        this.minHeap = minHeap;
    }

    /**
     * 插入元素
     *
     * @param data
     */
    public void insert(E data) {
        dataList.add(data);
        heapifyUp(dataList.size() - 1);
    }

    /**
     * 移除指定元素，返回移除的个数
     *
     * @param data
     * @return
     */
    public int remove(E data) {
        int count = 0;
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).equals(data)) {
                count++;
                removeAt(i);
                i--; // 调整索引
            }
        }
        return count;
    }

    /**
     * 删除指定位置的元素
     *
     * @param index
     */
    private void removeAt(int index) {
        if (index >= dataList.size()) {
            return;
        }
        E lastElement = dataList.remove(dataList.size() - 1);
        if (index < dataList.size()) {
            dataList.set(index, lastElement);
            heapifyDown(index);
            heapifyUp(index); // 确保删除后堆的性质仍然满足
        }
    }

    /**
     * 移除并返回根节点元素
     *
     * @return
     */
    public E removeRoot() {
        if (dataList.isEmpty()) {
            return null;
        }
        E root = dataList.get(0);
        E lastElement = dataList.remove(dataList.size() - 1);
        if (!dataList.isEmpty()) {
            dataList.set(0, lastElement);
            heapifyDown(0);
        }
        return root;
    }

    /**
     * 获取最小元素
     *
     * @return
     */
    public E getMin() {
        if (dataList.isEmpty()) {
            return null;
        }
        if (minHeap) {
            return dataList.get(0);
        } else {
            return findMinInMaxHeap();
        }
    }

    /**
     * 获取最大元素
     *
     * @return
     */
    public E getMax() {
        if (dataList.isEmpty()) {
            return null;
        }
        if (!minHeap) {
            return dataList.get(0);
        } else {
            return findMaxInMinHeap();
        }
    }

    /**
     * 在最大堆中查找最小元素
     *
     * @return
     */
    private E findMinInMaxHeap() {
        if (dataList.isEmpty()) {
            return null;
        }
        E min = dataList.get(0);
        for (E element : dataList) {
            if (element.compareTo(min) < 0) {
                min = element;
            }
        }
        return min;
    }

    /**
     * 在最小堆中查找最大元素
     *
     * @return
     */
    private E findMaxInMinHeap() {
        if (dataList.isEmpty()) {
            return null;
        }
        E max = dataList.get(0);
        for (E element : dataList) {
            if (element.compareTo(max) > 0) {
                max = element;
            }
        }
        return max;
    }

    // 获取堆的大小
    public int size() {
        return dataList.size();
    }

    // 判断堆是否为空
    public boolean isEmpty() {
        return dataList.isEmpty();
    }

    /**
     * 上滤操作
     *
     * @param index
     */
    private void heapifyUp(int index) {
        while (index > 0) {
            int parentIndex = parentIndex(index);
            if (minHeap) {
                if (dataList.get(index).compareTo(dataList.get(parentIndex)) < 0) {
                    swap(index, parentIndex);
                } else {
                    break;
                }
            } else {
                if (dataList.get(index).compareTo(dataList.get(parentIndex)) > 0) {
                    swap(index, parentIndex);
                } else {
                    break;
                }
            }
            index = parentIndex;
        }
    }

    // 下滤操作
    private void heapifyDown(int index) {
        int size = dataList.size();
        while (true) {
            int left = leftChildIndex(index);
            int right = rightChildIndex(index);
            int smallest = index;

            if (minHeap) {
                if (left < size && dataList.get(left).compareTo(dataList.get(smallest)) < 0) {
                    smallest = left;
                }
                if (right < size && dataList.get(right).compareTo(dataList.get(smallest)) < 0) {
                    smallest = right;
                }
            } else {
                if (left < size && dataList.get(left).compareTo(dataList.get(smallest)) > 0) {
                    smallest = left;
                }
                if (right < size && dataList.get(right).compareTo(dataList.get(smallest)) > 0) {
                    smallest = right;
                }
            }

            if (smallest != index) {
                swap(index, smallest);
                index = smallest;
            } else {
                break;
            }
        }
    }

    // 交换两个节点
    private void swap(int i, int j) {
        E temp = dataList.get(i);
        dataList.set(i, dataList.get(j));
        dataList.set(j, temp);
    }

    // 获取父节点的索引
    private int parentIndex(int index) {
        return (index - 1) / 2;
    }

    // 获取左子节点的索引
    private int leftChildIndex(int index) {
        return 2 * index + 1;
    }

    // 获取右子节点的索引
    private int rightChildIndex(int index) {
        return 2 * index + 2;
    }

    /**
     * 广度优先遍历
     *
     * @return
     */
    public List<E> bfs() {
        if (dataList.isEmpty()) {
            return new ArrayList<>();
        }
        List<E> result = new ArrayList<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(0);

        while (!queue.isEmpty()) {
            int index = queue.poll();
            result.add(dataList.get(index));

            int left = leftChildIndex(index);
            int right = rightChildIndex(index);

            if (left < dataList.size()) {
                queue.offer(left);
            }
            if (right < dataList.size()) {
                queue.offer(right);
            }
        }

        return result;
    }

    /**
     * 深度优先遍历 - 前序遍历
     *
     * @return
     */
    public List<E> dfsPre() {
        List<E> result = new ArrayList<>();
        preOrder(0, result);
        return result;
    }

    /**
     * 深度优先遍历 - 中序遍历
     *
     * @return
     */
    public List<E> dfsIn() {
        List<E> result = new ArrayList<>();
        inOrder(0, result);
        return result;
    }

    /**
     * 深度优先遍历 - 后序遍历
     *
     * @return
     */
    public List<E> dfsPost() {
        List<E> result = new ArrayList<>();
        postOrder(0, result);
        return result;
    }

    private void preOrder(int index, List<E> result) {
        if (index >= dataList.size()) {
            return;
        }
        result.add(dataList.get(index));
        preOrder(leftChildIndex(index), result);
        preOrder(rightChildIndex(index), result);
    }

    private void inOrder(int index, List<E> result) {
        if (index >= dataList.size()) {
            return;
        }
        inOrder(leftChildIndex(index), result);
        result.add(dataList.get(index));
        inOrder(rightChildIndex(index), result);
    }

    private void postOrder(int index, List<E> result) {
        if (index >= dataList.size()) {
            return;
        }
        postOrder(leftChildIndex(index), result);
        postOrder(rightChildIndex(index), result);
        result.add(dataList.get(index));
    }

    public static void main(String[] args) {
        MyBinaryHeap<Integer> heap = new MyBinaryHeap<>();
        for (int i = 1; i <= 10; i++) {
            heap.insert(i);
        }
        System.out.println(heap.bfs());
        System.out.println(heap.dfsPre());
        System.out.println(heap.dfsIn());
        System.out.println(heap.dfsPost());
    }
}
