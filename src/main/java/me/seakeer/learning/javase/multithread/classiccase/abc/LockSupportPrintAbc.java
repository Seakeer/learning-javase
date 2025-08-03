package me.seakeer.learning.javase.multithread.classiccase.abc;

import java.util.concurrent.locks.LockSupport;

/**
 * LockSupportPrintAbc;
 * 使用LockSupport 实现3线程交替打印ABC
 *
 * @author Seakeer;
 * @date 2024/9/28;
 */
public class LockSupportPrintAbc {

    private static Thread threadA, threadB, threadC;

    private static final int N = 10;

    public static void main(String[] args) {

        threadA = new Thread(() -> {
            for (int i = 0; i < N; i++) {
                System.out.print("A");
                LockSupport.unpark(threadB);
                LockSupport.park();
            }
        });
        threadB = new Thread(() -> {
            for (int i = 0; i < N; i++) {
                // 先挂起 等待被唤醒再执行
                LockSupport.park();
                System.out.print("B");
                // 唤醒下一个线程
                LockSupport.unpark(threadC);
            }
        });
        threadC = new Thread(() -> {
            for (int i = 0; i < N; i++) {
                LockSupport.park();
                System.out.print("C");
                LockSupport.unpark(threadA);
            }
        });

        threadA.start();
        threadB.start();
        threadC.start();
    }
}
