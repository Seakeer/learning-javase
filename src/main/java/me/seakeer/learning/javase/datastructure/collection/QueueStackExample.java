package me.seakeer.learning.javase.datastructure.collection;

import java.util.*;
import java.util.concurrent.*;

/**
 * QueueStackExample;
 *
 * @author Seakeer;
 * @date 2024/8/28;
 */
public class QueueStackExample {

    public static void main(String[] args) throws InterruptedException {

        crtQueueStackExample();

        queueStackCoreMethodExample();

    }

    private static void queueStackCoreMethodExample() throws InterruptedException {
        Queue<Integer> queue = new ArrayDeque<>();
        boolean offer = queue.offer(1);
        Integer peek = queue.peek();
        Integer poll = queue.poll();
        System.out.printf("[Queue][offer: %s, peek: %s, poll: %s]\n", offer, peek, poll);

        BlockingQueue<Integer> blockingQueue = new ArrayBlockingQueue<>(10);
        blockingQueue.put(1);
        boolean blockingQueueOffer = blockingQueue.offer(2, 1, TimeUnit.SECONDS);
        Integer blockingQueuePeek = blockingQueue.peek();
        Integer blockingQueueTake = blockingQueue.take();
        System.out.printf("[BlockingQueue][put: %s, offer: %s, peek: %s, take: %s]\n", "void", blockingQueueOffer, blockingQueuePeek, blockingQueueTake);


        Deque<Integer> stack = new ArrayDeque<>();

        boolean offerFirst = stack.offerFirst(1);
        Integer peekFirst = stack.peekFirst();
        Integer pollFirst = stack.pollFirst();
        System.out.printf("[Stack][offerFirst: %s, peekFirst: %s, pollFirst: %s]\n", offerFirst, peekFirst, pollFirst);

        stack.push(2);
        Integer stackPeek = stack.peek();
        Integer stackPop = stack.pop();
        System.out.printf("[Stack][push: %s, peek: %s, pop: %s]", "void", stackPeek, stackPop);

    }

    private static void crtQueueStackExample() {
        // 单线程顺序队列
        Queue<Integer> seqQueue = new ArrayDeque<>();
        // 单线程链式队列
        Queue<Integer> linkedQueue = new LinkedList<>();
        // 单线程优先队列
        Queue<Integer> priorityQueue = new PriorityQueue<>();

        // 多线程链式队列
        Queue<Integer> safeLinkedQueue = new ConcurrentLinkedQueue<>();

        // 顺序阻塞队列
        BlockingQueue<Integer> seqBlockingQueue = new ArrayBlockingQueue<>(10);
        // 链式阻塞队列
        BlockingQueue<Integer> linkedBlockingQueue = new LinkedBlockingQueue<>();
        // 优先阻塞队列
        BlockingQueue<Integer> priorityBlockingQueue = new PriorityBlockingQueue<>();


        // 单线程顺序栈
        Deque<Integer> seqStack = new ArrayDeque<>();
        // 单线程链栈
        Deque<Integer> linkedStack = new LinkedList<>();

        // 多线程顺序栈
        Stack<Integer> safeSeqStack = new Stack<>();
        // 多线程链栈
        Deque<Integer> safeLinkedStack = new ConcurrentLinkedDeque<>();
        // 链式阻塞栈
        BlockingDeque<Integer> linkedBlockingStack = new LinkedBlockingDeque<>();
    }
}
