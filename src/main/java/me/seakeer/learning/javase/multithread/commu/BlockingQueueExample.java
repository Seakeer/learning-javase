package me.seakeer.learning.javase.multithread.commu;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * BlockingQueueExample;
 *
 * @author Seakeer;
 * @date 2024/7/18;
 */
public class BlockingQueueExample {

    // 初始化阻塞队列，容量为3，对应3个运动员
    private static final BlockingQueue<Integer> BLOCKING_QUEUE = new LinkedBlockingQueue<>(3);

    public static void main(String[] args) {
        new Thread(BlockingQueueExample::run, "Runner 1").start();
        new Thread(BlockingQueueExample::run, "Runner 2").start();
        new Thread(BlockingQueueExample::run, "Runner 3").start();
        new Thread(BlockingQueueExample::order, "Starter").start();
    }

    private static void run() {
        System.out.println(Thread.currentThread().getName() + " is ready.");
        try {
            // 从阻塞队列中取元素，如果没有元素则等待直到有元素可取
            BLOCKING_QUEUE.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long cur = System.currentTimeMillis();
        System.out.println(Thread.currentThread().getName() + " is running.");
        try {
            Thread.sleep(1000 + new Random().nextInt(1001));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(Thread.currentThread().getName() + " costs " + (System.currentTimeMillis() - cur) + "ms.");
    }

    private static void order() {
        System.out.print(Thread.currentThread().getName() + " is ordering: ");
        for (int i = 3; i > 0; i--) {
            System.out.print(i + "...");
        }
        System.out.println("GO!");
        // 发令员发令：向阻塞队列中添加3个元素，对应的3个运动员获取到元素就开跑
        BLOCKING_QUEUE.addAll(Arrays.asList(1, 2, 3));
    }
}
