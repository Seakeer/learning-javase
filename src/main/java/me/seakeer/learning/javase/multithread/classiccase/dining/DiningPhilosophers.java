package me.seakeer.learning.javase.multithread.classiccase.dining;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DiningPhilosophers;
 * 哲学家进餐问题
 * 限制最多4个哲学家同时持有叉子
 *
 * @author Seakeer;
 * @date 2024/9/29;
 */
public class DiningPhilosophers {

    // 5个叉子对应5把锁ReentrantLock，或者使用Semaphore也可
    private final ReentrantLock[] lockList = {
            new ReentrantLock(),
            new ReentrantLock(),
            new ReentrantLock(),
            new ReentrantLock(),
            new ReentrantLock()
    };

    // 限制最多4个哲学家同时持有叉子，避免5个哲学家各持有一个叉子的情况，即死锁
    private final Semaphore withForkPhilosopherLimit = new Semaphore(4);

    public DiningPhilosophers() {

    }

    // call the run() method of any runnable to execute its code
    public void wantsToEat(int philosopher,
                           Runnable pickLeftFork,
                           Runnable pickRightFork,
                           Runnable eat,
                           Runnable putLeftFork,
                           Runnable putRightFork) throws InterruptedException {

        // 计算叉子编号
        int leftFork = (philosopher + 1) % 5;
        int rightFork = philosopher;

        // 允许持有叉子的哲学家数量 -1
        withForkPhilosopherLimit.acquire();

        // 给左右的叉子加锁
        lockList[leftFork].lock();
        lockList[rightFork].lock();

        // 加锁成功则意味着可拿起叉子，进行吃面
        pickLeftFork.run();
        pickRightFork.run();

        eat.run();

        // 吃完后放下叉子
        putLeftFork.run();
        putRightFork.run();

        // 释放锁
        lockList[leftFork].unlock();
        lockList[rightFork].unlock();

        // 允许持有叉子的哲学家数量 +1
        withForkPhilosopherLimit.release();
    }
}