package me.seakeer.learning.javase.multithread.classiccase.abc;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LockConditionPrintAbc;
 *
 * @author Seakeer;
 * @date 2024/9/28;
 */
public class LockConditionPrintAbc {

    private static final int N = 10;

    private static final Lock LOCK = new ReentrantLock();
    private static final Condition CONDITION_A = LOCK.newCondition();
    private static final Condition CONDITION_B = LOCK.newCondition();
    private static final Condition CONDITION_C = LOCK.newCondition();

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
        LOCK.lock();
        try {
            while (!"A".equals(curLetter)) {
                CONDITION_A.await();
            }
            System.out.print("A");
            curLetter = "B";
            CONDITION_B.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            LOCK.unlock();
        }
    }

    private static void printB() {
        LOCK.lock();
        try {
            while (!"B".equals(curLetter)) {
                CONDITION_B.await();
            }
            System.out.print("B");
            curLetter = "C";
            CONDITION_C.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            LOCK.unlock();
        }
    }

    private static void printC() {
        LOCK.lock();
        try {
            while (!"C".equals(curLetter)) {
                CONDITION_C.await();
            }
            System.out.print("C");
            curLetter = "A";
            CONDITION_A.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            LOCK.unlock();
        }
    }
}
