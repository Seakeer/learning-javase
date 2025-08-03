package me.seakeer.learning.javase.multithread;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * MultiLockExample;
 * 锁示例
 *
 * @author Seakeer;
 * @date 2024/8/31;
 */
public class MultiLockExample {

    public static void main(String[] args) {

        reentrantLock();

        reentrantReadWriteLock();

        stampedLock();
    }

    private static void stampedLock() {
        Point point = new Point();
        new Thread(() -> {
            System.out.println(point.distanceFromOrigin());
            point.moveIfAtOrigin(1, 1);
            point.move(1, 1);
            System.out.println(point.distanceFromOrigin());
        }).start();
    }

    private static void reentrantLock() {
        Lock lock = new ReentrantLock(true);

        if (lock.tryLock()) {
            System.out.println("线程：" + Thread.currentThread().getName() + " 获取锁成功");
            new Thread(() -> {
                lock.lock();
                System.out.println("线程：" + Thread.currentThread().getName() + " 获取锁成功");
                System.out.println("线程：" + Thread.currentThread().getName() + " running");
            }, "FirstThread").start();


            System.out.println("线程：" + Thread.currentThread().getName() + " running");
            lock.unlock();
            System.out.println("线程：" + Thread.currentThread().getName() + " 释放锁");

        } else {
            System.out.println("线程：" + Thread.currentThread().getName() + " 获取锁失败");
        }


        System.out.println("线程：" + Thread.currentThread().getName() + " ending");
    }

    private static void reentrantReadWriteLock() {
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        Lock writeLock = readWriteLock.writeLock();
        Lock readLock = readWriteLock.readLock();

        // 获取写锁
        if (writeLock.tryLock()) {
            System.out.printf("线程：%s, 读锁次数：%d, 写锁次数：%d, 是否持有写锁：%s\n", Thread.currentThread().getName(), readWriteLock.getReadHoldCount(), readWriteLock.getWriteHoldCount(), readWriteLock.isWriteLocked());
            // 持有写锁可获取到读锁，即可进行锁降级
            if (readLock.tryLock()) {
                System.out.printf("线程：%s, 读锁次数：%d, 写锁次数：%d, 是否持有写锁：%s\n", Thread.currentThread().getName(), readWriteLock.getReadHoldCount(), readWriteLock.getWriteHoldCount(), readWriteLock.isWriteLocked());
                // 释放写锁，完成锁降级
                writeLock.unlock();
                System.out.printf("线程：%s, 读锁次数：%d, 写锁次数：%d, 是否持有写锁：%s\n", Thread.currentThread().getName(), readWriteLock.getReadHoldCount(), readWriteLock.getWriteHoldCount(), readWriteLock.isWriteLocked());
                // 此时线程持有读锁，无法获取到写锁，即不能进行锁升级
                if (writeLock.tryLock()) {
                    System.out.printf("线程：%s, 读锁次数：%d, 写锁次数：%d, 是否持有写锁：%s\n", Thread.currentThread().getName(), readWriteLock.getReadHoldCount(), readWriteLock.getWriteHoldCount(), readWriteLock.isWriteLocked());
                }
            }
        }
        readLock.unlock();
    }
}


class Point {

    private double x, y;

    private final StampedLock sl = new StampedLock();

    void move(double deltaX, double deltaY) {
        // 涉及对共享资源的修改，使用写锁
        long stamp = sl.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    /**
     * 使用乐观读锁访问共享资源
     * 注意：乐观读锁在保证数据一致性上需要拷贝一份要操作的变量到方法栈，并且在操作数据时候可能其他写线程已经修改了数据，
     * 而我们操作的是方法栈里面的数据，也就是一个快照，所以最多返回的不是最新的数据，但是一致性还是得到保障的。
     *
     * @return
     */
    double distanceFromOrigin() {
        long stamp = sl.tryOptimisticRead();    // 使用乐观读锁
        double currentX = x, currentY = y;      // 拷贝共享资源到本地方法栈中
        if (!sl.validate(stamp)) {              // 如果有写锁被占用，可能造成数据不一致，所以要切换到普通读锁模式
            stamp = sl.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                sl.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    void moveIfAtOrigin(double newX, double newY) { // upgrade
        // Could instead start with optimistic, not read mode
        long stamp = sl.readLock();
        try {
            while (x == 0.0 && y == 0.0) {
                long ws = sl.tryConvertToWriteLock(stamp);  //读锁转换为写锁
                if (ws != 0L) {
                    stamp = ws;
                    x = newX;
                    y = newY;
                    break;
                } else {
                    sl.unlockRead(stamp);
                    stamp = sl.writeLock();
                }
            }
        } finally {
            sl.unlock(stamp);
        }
    }
}