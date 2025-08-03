package me.seakeer.learning.javase.multithread;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ThreadSyncExample;
 * 线程同步示例
 *
 * @author Seakeer;
 * @date 2024/7/15;
 */
public class ThreadSyncExample {
    public static void main(String[] args) {
        TicketService ticketService = new TicketService(9);
        new Thread(ticketService, "TicketWindow1").start();
        new Thread(ticketService, "TicketWindow2").start();
        new Thread(ticketService, "TicketWindow3").start();
        new Thread(ticketService, "TicketWindow4").start();
    }
}

/**
 * 票务;
 */
class TicketService implements Runnable {

    public TicketService(int tickets) {
        this.tickets = tickets;
    }

    private static final Lock LOCK = new ReentrantLock();

    /**
     * 共享资源
     */
    private int tickets;

    @Override
    public void run() {
        while (true) {
            boolean isSelloutSyncMethod = saleTicketSyncMethod();
            boolean isSelloutSyncCodeBlock = saleTicketSyncCodeBlock();
            boolean isSelloutLock = saleTicketLock();
            if (isSelloutSyncMethod || isSelloutSyncCodeBlock || isSelloutLock) {
                break;
            }
        }
    }

    /**
     * 同步代码块
     */
    private boolean saleTicketSyncCodeBlock() {
        // 同步代码块
        synchronized (this) {
            if (tickets > 0) {
                System.out.println(Thread.currentThread().getName() + " 出售第 " + tickets-- + " 张票" + " --- 同步代码块");
                try {
                    Thread.sleep(100);
                    Thread.yield();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * 同步方法
     */
    private synchronized boolean saleTicketSyncMethod() {
        if (tickets > 0) {
            System.out.println(Thread.currentThread().getName() + " 出售第 " + tickets-- + " 张票" + " --- 同步方法");
            try {
                Thread.sleep(100);
                Thread.yield();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        } else {
            return true;
        }
    }


    /**
     * 显式锁Lock
     */
    private boolean saleTicketLock() {
        if (LOCK.tryLock()) {
            if (tickets > 0) {
                System.out.println(Thread.currentThread().getName() + " 出售第 " + tickets-- + " 张票" + " --- Lock");
                try {
                    Thread.sleep(100);
                    Thread.yield();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LOCK.unlock();
                return false;
            } else {
                LOCK.unlock();
                return true;
            }
        }
        return false;
    }

}