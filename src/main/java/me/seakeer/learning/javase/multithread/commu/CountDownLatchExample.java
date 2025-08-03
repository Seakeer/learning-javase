package me.seakeer.learning.javase.multithread.commu;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * CountDownLatchExample;
 *
 * @author Seakeer;
 * @date 2024/7/16;
 */
public class CountDownLatchExample {

    // 倒数器 从1倒数 对应一个发令员
    private static final CountDownLatch COUNT_DOWN_LATCH = new CountDownLatch(1);

    public static void main(String[] args) {
        new Thread(CountDownLatchExample::run, "Runner 1").start();
        new Thread(CountDownLatchExample::run, "Runner 2").start();
        new Thread(CountDownLatchExample::run, "Runner 3").start();
        new Thread(CountDownLatchExample::order, "Starter").start();
    }

    private static void run() {
        System.out.println(Thread.currentThread().getName() + " is ready.");
        try {
            // 等待计数器到达零
            COUNT_DOWN_LATCH.await();
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
        // 计数器减一
        COUNT_DOWN_LATCH.countDown();
    }
}
