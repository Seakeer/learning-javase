package me.seakeer.learning.javase.multithread.classiccase.abc;

/**
 * WaitNotifyPrintAbc;
 *
 * @author Seakeer;
 * @date 2024/9/28;
 */
public class WaitNotifyPrintAbc {

    private static final int N = 10;

    private static final Object MUTEX = new Object();

    private static String curLetter = "A";

    public static void main(String[] args) {
        Thread threadA = new Thread(() -> {
            for (int i = 0; i < N; i++) {
                printA();
            }
        });

        Thread threadB = new Thread(() -> {
            for (int i = 0; i < N; i++) {
                printB();
            }
        });

        Thread threadC = new Thread(() -> {
            for (int i = 0; i < N; i++) {
                printC();
            }
        });

        threadA.start();
        threadB.start();
        threadC.start();
    }

    private static void printA() {
        synchronized (MUTEX) {
            while (!"A".equals(curLetter)) {
                try {
                    MUTEX.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.print("A");
            curLetter = "B";
            MUTEX.notifyAll();
        }
    }

    private static void printB() {
        synchronized (MUTEX) {
            while (!"B".equals(curLetter)) {
                try {
                    MUTEX.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.print("B");
            curLetter = "C";
            MUTEX.notifyAll();
        }
    }

    private static void printC() {
        synchronized (MUTEX) {
            while (!"C".equals(curLetter)) {
                try {
                    MUTEX.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.print("C");
            curLetter = "A";
            MUTEX.notifyAll();
        }
    }
}
