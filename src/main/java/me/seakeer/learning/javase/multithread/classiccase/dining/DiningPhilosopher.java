package me.seakeer.learning.javase.multithread.classiccase.dining;

import java.util.concurrent.Semaphore;

/**
 * DiningPhilosopher;
 * 限制同一时间只能有 1 个哲学家进餐
 *
 * @author Seakeer;
 * @date 2024/9/29;
 */
public class DiningPhilosopher {

    // 限制同一时间只能有 1 个哲学家进餐
    private final Semaphore eatingPhilosopher = new Semaphore(1);

    public DiningPhilosopher() {

    }

    // call the run() method of any runnable to execute its code
    public void wantsToEat(int philosopher,
                           Runnable pickLeftFork,
                           Runnable pickRightFork,
                           Runnable eat,
                           Runnable putLeftFork,
                           Runnable putRightFork) throws InterruptedException {
        // 开始就餐
        eatingPhilosopher.acquire();

        pickLeftFork.run();
        pickRightFork.run();

        eat.run();

        putLeftFork.run();
        putRightFork.run();


        // 就餐完成，释放锁，让其他哲学家进餐
        eatingPhilosopher.release();
    }
}
