package me.seakeer.learning.javase.multithread.classiccase.abc;

import java.util.concurrent.Semaphore;

/**
 * SemaphorePrintAbc;
 *
 * @author Seakeer;
 * @date 2024/9/28;
 */
public class SemaphorePrintAbc {

    private static final int N = 10;

    private static final Semaphore SEMAPHORE_A = new Semaphore(1);
    private static final Semaphore SEMAPHORE_B = new Semaphore(0);
    private static final Semaphore SEMAPHORE_C = new Semaphore(0);

    public static void main(String[] args) {
        new Thread(() -> print("A", SEMAPHORE_A, SEMAPHORE_B)).start();
        new Thread(() -> print("B", SEMAPHORE_B, SEMAPHORE_C)).start();
        new Thread(() -> print("C", SEMAPHORE_C, SEMAPHORE_A)).start();
    }

    private static void print(String letter, Semaphore curSemaphore, Semaphore nextSemaphore) {
        for (int i = 0; i < N; i++) {
            try {
                curSemaphore.acquire();
                System.out.print(letter);
                nextSemaphore.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
