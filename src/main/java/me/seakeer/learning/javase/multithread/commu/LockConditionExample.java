package me.seakeer.learning.javase.multithread.commu;

import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LockConditionExample;
 *
 * @author Seakeer;
 * @date 2024/7/16;
 */
public class LockConditionExample {

    // LOCK加锁
    private static final Lock LOCK = new ReentrantLock();
    // 条件变量
    private static final Condition CONDITION = LOCK.newCondition();

    public static void main(String[] args) {
        new Thread(LockConditionExample::run, "Runner 1").start();
        new Thread(LockConditionExample::run, "Runner 2").start();
        new Thread(LockConditionExample::run, "Runner 3").start();
        new Thread(LockConditionExample::order, "Starter").start();
    }

    private static void run() {
        System.out.println(Thread.currentThread().getName() + " is ready.");
        LOCK.lock();
        try {
            // 在该条件变量上等待
            CONDITION.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            LOCK.unlock();
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
        LOCK.lock();
        try {
            // 唤醒所有在该条件变量上等待的线程
            CONDITION.signalAll();
        } finally {
            LOCK.unlock();
        }
    }
}
