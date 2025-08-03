package me.seakeer.learning.javase.multithread.commu;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * CyclicBarrierExample;
 *
 * @author Seakeer;
 * @date 2024/7/16;
 */
public class CyclicBarrierExample {

    /**
     * 循环屏障 3个运动员 + 1个发令员，共计4个参与者
     * 4个参与者都准备好后开跑，即3个运动员准备好起跑，发令员发令
     */
    private static final CyclicBarrier CYCLIC_BARRIER = new CyclicBarrier(4);

    public static void main(String[] args) {
        new Thread(CyclicBarrierExample::run, "Runner 1").start();
        new Thread(CyclicBarrierExample::run, "Runner 2").start();
        new Thread(CyclicBarrierExample::run, "Runner 3").start();
        new Thread(CyclicBarrierExample::order, "Starter").start();
    }

    private static void run() {
        System.out.println(Thread.currentThread().getName() + " is ready.");
        try {
            // 等待所有参与者到达屏障
            CYCLIC_BARRIER.await();
        } catch (InterruptedException | BrokenBarrierException e) {
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
        try {
            // 发令员也参与屏障
            CYCLIC_BARRIER.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
