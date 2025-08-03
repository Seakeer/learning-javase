package me.seakeer.learning.javase.multithread.commu;

import java.util.Random;
import java.util.concurrent.locks.LockSupport;

/**
 * LockSupportExample;
 *
 * @author Seakeer;
 * @date 2024/7/16;
 */
public class LockSupportExample {

    public static void main(String[] args) {
        Thread runner1 = new Thread(() -> run(), "Runner 1");
        Thread runner2 = new Thread(() -> run(), "Runner 2");
        Thread runner3 = new Thread(() -> run(), "Runner 3");

        runner1.start();
        runner2.start();
        runner3.start();

        new Thread(() -> order(runner1, runner2, runner3), "Starter").start();
    }

    private static void run() {
        System.out.println(Thread.currentThread().getName() + " is ready.");
        // 线程挂起，等待许可
        LockSupport.park();
        long cur = System.currentTimeMillis();
        System.out.println(Thread.currentThread().getName() + " is running.");
        try {
            Thread.sleep(1000 + new Random().nextInt(1001));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(Thread.currentThread().getName() + " costs " + (System.currentTimeMillis() - cur) + "ms.");
    }

    private static void order(Thread... runners) {
        System.out.print(Thread.currentThread().getName() + " is ordering: ");
        for (int i = 3; i > 0; i--) {
            System.out.print(i + "...");
        }
        System.out.println("GO!");
        for (Thread runner : runners) {
            // 唤醒指定线程
            LockSupport.unpark(runner);
        }
    }
}
