package me.seakeer.learning.javase.multithread.commu;

import java.util.Random;

/**
 * WaitNotifyExample;
 *
 * @author Seakeer;
 * @date 2024/7/16;
 */
public class WaitNotifyExample {

    /**
     * 对象锁；在该对象锁上进行等待和通知
     */
    public static final Object MUTEX = new Object();

    public static void main(String[] args) {
        new Thread(() -> run(), "Runner 1").start();
        new Thread(() -> run(), "Runner 2").start();
        new Thread(() -> run(), "Runner 3").start();
        new Thread(() -> order(), "Starter").start();
    }

    private static void run() {
        System.out.println(Thread.currentThread().getName() + " is ready.");
        while (true) {
            // 上锁 等待和通知
            synchronized (MUTEX) {
                try {
                    MUTEX.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long cur = System.currentTimeMillis();
            System.out.println(Thread.currentThread().getName() + " is running.");
            try {
                Thread.sleep(1000 + new Random().nextInt(1001));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + " costs " + (System.currentTimeMillis() - cur) + "ms.");
            break;
        }
    }

    private static void order() {
        System.out.print(Thread.currentThread().getName() + " is ordering: ");
        for (int i = 3; i > 0; i--) {
            System.out.print(i + "...");
        }
        System.out.println("GO!");
        synchronized (MUTEX) {
            // 唤醒所有在MUTEX上等待的线程
            MUTEX.notifyAll();
        }
    }
}
