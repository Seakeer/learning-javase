package me.seakeer.learning.javase.multithread.commu;

import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * SemaphoreExample;
 *
 * @author Seakeer;
 * @date 2024/9/27;
 */
public class SemaphoreExample {

    /**
     * 初始信号量许可为0，即没有可用信号量
     * 发令员发令即释放3个信号量给3个运动员
     */
    private static final Semaphore SEMAPHORE = new Semaphore(0);

    public static void main(String[] args) {
        new Thread(SemaphoreExample::run, "Runner 1").start();
        new Thread(SemaphoreExample::run, "Runner 2").start();
        new Thread(SemaphoreExample::run, "Runner 3").start();
        new Thread(SemaphoreExample::order, "Starter").start();
    }

    private static void run() {
        System.out.println(Thread.currentThread().getName() + " is ready.");
        try {
            // 等待信号量被释放
            SEMAPHORE.acquire();
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
        // 释放信号量 因为有3个线程在获取信号量，所以释放3个
        SEMAPHORE.release(3);
    }
}