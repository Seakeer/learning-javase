package me.seakeer.learning.javase.multithread.commu;

import java.util.Random;

/**
 * VolatileExample;
 *
 * @author Seakeer;
 * @date 2024/7/16;
 */
public class VolatileExample {

    /**
     * volatile修饰的共享变量
     * 用于标识是否开跑
     */
    private static volatile boolean go = false;

    public static void main(String[] args) {
        // 3位运动员
        new Thread(() -> run(), "Runner 1").start();
        new Thread(() -> run(), "Runner 2").start();
        new Thread(() -> run(), "Runner 3").start();
        // 发令员
        new Thread(() -> order(), "Starter").start();
    }

    private static void run() {
        System.out.println(Thread.currentThread().getName() + " is ready.");
        while (true) {
            // 发令员发令后开跑
            if (go) {
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
    }

    /**
     * 发令员发令
     */
    private static void order() {
        System.out.print(Thread.currentThread().getName() + " is ordering: ");
        for (int i = 3; i > 0; i--) {
            System.out.print(i + "...");
        }
        System.out.println("GO!");
        go = true;
    }
}

