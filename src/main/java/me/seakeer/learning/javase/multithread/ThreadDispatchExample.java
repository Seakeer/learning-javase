package me.seakeer.learning.javase.multithread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * ThreadDispatchExample;
 * 线程调度示例
 *
 * @author Seakeer;
 * @date 2024/7/15;
 */
public class ThreadDispatchExample {
    public static void main(String[] args) {

        Thread th1 = new Thread(new MyTask());
        Thread th2 = new Thread(new MyTask());

        //设置优先级
        th2.setPriority(Thread.MAX_PRIORITY);
        th1.setPriority(Thread.MIN_PRIORITY);

        th1.start();
        th2.start();

        for (int i = 9; i > 0; i--) {
            if (2 == i) {
                try {
                    System.out.println(Thread.currentThread().getName() + " is running ");
                    System.out.println(th1.getName() + " will join");
                    // 线程插队。
                    th1.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            System.out.println(Thread.currentThread().getName() + " is outputting " + i);
        }


        System.out.println("main will sleep for 3s");
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("main will park for 1s");
        LockSupport.parkNanos(1000 * 1000);

        System.out.println("main will interrupt");

        // 逻辑中断，后续代码还会执行
        Thread.currentThread().interrupt();

        System.out.println("main is interrupted: " + Thread.currentThread().isInterrupted());
        // 返回线程是否中断，并清除掉中断标记
        System.out.println("main is interrupted: " + Thread.interrupted());
        System.out.println("main is interrupted: " + Thread.currentThread().isInterrupted());


        // Thread.currentThread().suspend();
        // Thread.currentThread().resume();
        // Thread.currentThread().stop();
    }

    static class MyTask implements Runnable {
        @Override
        public void run() {
            for (int i = 9; i > 0; i--) {
                if (6 == i) {
                    try {
                        System.out.println(Thread.currentThread().getName() + " is sleeping");
                        // 线程休眠100ms
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (4 == i) {
                    System.out.println(Thread.currentThread().getName() + " is yielding");
                    // 线程让步
                    Thread.yield();
                }
                System.out.println(Thread.currentThread().getName() + " is outputting " + i);
            }

        }
    }
}

