package me.seakeer.learning.javase.multithread;


import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ThreadLocalExample;
 *
 * @author Seakeer;
 * @date 2024/12/3;
 */
public class ThreadLocalExample {

    // 通常定义为静态常量
    public static final ThreadLocal<Integer> THREAD_LOCAL = new ThreadLocal<>();
    public static final InheritableThreadLocal<Integer> INHERITABLE_THREAD_LOCAL = new InheritableThreadLocal<>();
    public static final TransmittableThreadLocal<Integer> TRANSMITTABLE_THREAD_LOCAL = new TransmittableThreadLocal<>();

    // 展示两种线程池下TransmittableThreadLocal的差异
    public static final ExecutorService EXECUTOR_SERVICE_1 = Executors.newSingleThreadExecutor();
    public static final ExecutorService EXECUTOR_SERVICE_2 = TtlExecutors.getTtlExecutorService(Executors.newSingleThreadExecutor());

    public static void main(String[] args) throws InterruptedException {
        // 主线程设置值为1
        set(1);
        print();
        // 普通线程池的子线程获取值
        EXECUTOR_SERVICE_1.execute(ThreadLocalExample::print);
        TimeUnit.SECONDS.sleep(1);

        // TTL线程池的子线程获取值
        EXECUTOR_SERVICE_2.execute(ThreadLocalExample::print);
        TimeUnit.SECONDS.sleep(1);

        // 主线程更新值为2
        set(2);

        // 普通线程池的子线程获取更新值情况：3种都获取不到更新后的值
        EXECUTOR_SERVICE_1.execute(ThreadLocalExample::print);
        TimeUnit.SECONDS.sleep(1);

        // TTL线程池的子线程获取更新后值的情况：TransmittableThreadLocal可获取到更新后的值
        EXECUTOR_SERVICE_2.execute(() -> {
            print();
            // 子线程更新
            set(3);
        });
        TimeUnit.SECONDS.sleep(1);

        // 子线程更新值，主线程获取不到
        print();

        // 及时清理，避免内存泄漏
        THREAD_LOCAL.remove();
        INHERITABLE_THREAD_LOCAL.remove();
        TRANSMITTABLE_THREAD_LOCAL.remove();

        // 关闭线程池
        EXECUTOR_SERVICE_1.shutdown();
        EXECUTOR_SERVICE_2.shutdown();

    }

    private static void print() {
        System.out.println("--------------------------------------------------------");
        System.out.printf("Thread: %s, ThreadLocal: %d \n", Thread.currentThread().getName(), THREAD_LOCAL.get());
        System.out.printf("Thread: %s, InheritableThreadLocal: %d \n", Thread.currentThread().getName(), INHERITABLE_THREAD_LOCAL.get());
        System.out.printf("Thread: %s, TransmittableThreadLocal: %d \n", Thread.currentThread().getName(), TRANSMITTABLE_THREAD_LOCAL.get());
    }

    private static void set(Integer value) {
        THREAD_LOCAL.set(value);
        INHERITABLE_THREAD_LOCAL.set(value);
        TRANSMITTABLE_THREAD_LOCAL.set(value);
    }
}
