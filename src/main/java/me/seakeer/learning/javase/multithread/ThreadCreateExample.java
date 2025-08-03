package me.seakeer.learning.javase.multithread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * CreateThreadExample;
 * 线程创建示例
 *
 * @author Seakeer;
 * @date 2024/7/11;
 */
public class ThreadCreateExample {

    public static void main(String[] args) throws Exception {

        // 继承Thread类
        new MyThread("售票窗口1(MyThread)").start();

        // 实现Runnable接口
        new Thread(new MyRunnable(), "售票窗口2(MyRunnableThread)").start();

        // 使用FutureTask和Callable接口
        FutureTask<Integer> futureTask = new FutureTask<>(new MyCallable());
        Thread thread = new Thread(futureTask, "售票窗口3(MyCallableThread)");
        thread.start();
        try {
            Integer integer = futureTask.get();
            System.out.println(thread.getName() + "剩余票数：" + integer);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }
}

/**
 * 继承Thread类，重写run()方法
 */
class MyThread extends Thread {
    private int tickets = 6;

    public MyThread() {
        super();
    }

    public MyThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        while (--tickets > 0) {
            System.out.println(Thread.currentThread().getName() + "正在发售第" + tickets + "张票");
        }
    }
}

/**
 * 实现Runnable接口，重写run()方法
 */
class MyRunnable implements Runnable {
    private int tickets = 6;

    @Override
    public void run() {
        while (--tickets > 0) {
            System.out.println(Thread.currentThread().getName() + "正在发售第" + tickets + "张票");
        }
    }
}

/**
 * 实现Callable接口，重写call()方法
 */
class MyCallable implements Callable<Integer> {
    private int tickets = 6;

    @Override
    public Integer call() throws Exception {
        while (--tickets > 0) {
            System.out.println(Thread.currentThread().getName() + "正在发售第" + tickets + "张票");
        }
        return tickets;
    }
}
