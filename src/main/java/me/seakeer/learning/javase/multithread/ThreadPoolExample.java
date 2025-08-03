package me.seakeer.learning.javase.multithread;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * ThreadPoolExample;
 * 线程池示例
 *
 * @author Seakeer;
 * @date 2024/9/18;
 */
public class ThreadPoolExample {

    public static void main(String[] args) throws InterruptedException {

        threadPoolExecutorExample();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("----------------------------------------------------------------------");


        scheduledThreadPoolExecutorExample();
        System.out.println("----------------------------------------------------------------------");

        forkJoinPoolExecutorExample();
        System.out.println("----------------------------------------------------------------------");

        executorsExample();
    }

    private static void threadPoolExecutorExample() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                8,
                16,
                1L, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(32),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        threadPoolExecutor.execute(() -> System.out.println("[ThreadPoolExecutor] [ExecuteRunnableTask]  ThreadName: " + Thread.currentThread().getName()));
        Future<?> submitRunnableTask = threadPoolExecutor.submit(() -> System.out.println("[ThreadPoolExecutor] [SubmitRunnableTask] ThreadName: " + Thread.currentThread().getName()));
        Future<String> submitCallableTask = threadPoolExecutor.submit(() -> "[ThreadPoolExecutor] [SubmitCallableTask] ThreadName: " + Thread.currentThread().getName());
        try {
            System.out.println(submitCallableTask.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Callable<String>> callableList = Arrays.asList(
                () -> "[ThreadPoolExecutor] [TaskBatch] [CallableTask1] ThreadName: " + Thread.currentThread().getName(),
                () -> "[ThreadPoolExecutor] [TaskBatch] [CallableTask2] ThreadName: " + Thread.currentThread().getName(),
                () -> "[ThreadPoolExecutor] [TaskBatch] [CallableTask3] ThreadName: " + Thread.currentThread().getName()
        );
        try {
            List<Future<String>> futures = threadPoolExecutor.invokeAll(callableList);
            for (Future<String> future : futures) {
                System.out.println(future.get());
            }
            String result = threadPoolExecutor.invokeAny(callableList);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        threadPoolExecutor.shutdown();
    }

    private static void scheduledThreadPoolExecutorExample() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(8);

        long start = System.currentTimeMillis();
        // 延迟100ms执行
        scheduledThreadPoolExecutor.schedule(() -> System.out.println("[ScheduledThreadPoolExecutor] [DelayTask] ThreadName: "
                        + Thread.currentThread().getName()
                        + " Time: " + (System.currentTimeMillis() - start)),
                100, TimeUnit.MILLISECONDS);

        // 初始延迟200ms, 之后在上一个任务执行完毕后 固定延迟500ms执行
        scheduledThreadPoolExecutor.scheduleWithFixedDelay(() -> System.out.println("[ScheduledThreadPoolExecutor] [FixedDelayTask] ThreadName: " + Thread.currentThread().getName()
                        + " Time: " + (System.currentTimeMillis() - start)),
                200, 500, TimeUnit.MILLISECONDS);


        // 初始延迟500ms, 之后每隔1000ms执行一次，如果上一个任务执行完成后超过了1000ms, 则立即执行当前任务
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> System.out.println("[ScheduledThreadPoolExecutor] [FixedRateTask] ThreadName: " + Thread.currentThread().getName()
                        + " Time: " + (System.currentTimeMillis() - start)),
                500, 1000, TimeUnit.MILLISECONDS);

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        scheduledThreadPoolExecutor.shutdown();
    }

    private static void forkJoinPoolExecutorExample() {


        class SumTask extends RecursiveTask<Integer> {
            private final int[] numbers;
            private final int beginIdx;
            private final int endIdx;

            public SumTask(int[] numbers, int startIdx, int endIdx) {
                this.numbers = numbers;
                this.beginIdx = startIdx;
                this.endIdx = endIdx;
            }

            @Override
            protected Integer compute() {
                // 将大任务拆分为四份小任务
                if (endIdx - beginIdx <= numbers.length / 4) {
                    int sum = 0;
                    for (int i = beginIdx; i <= endIdx; i++) {
                        sum += numbers[i];
                    }
                    return sum;
                } else {
                    // 取中间
                    int midIdx = beginIdx + (endIdx - beginIdx) / 2;
                    SumTask leftTask = new SumTask(numbers, beginIdx, midIdx);
                    SumTask rightTask = new SumTask(numbers, midIdx + 1, endIdx);
                    // 子任务加入任务队列进行执行
                    leftTask.fork();
                    rightTask.fork();
                    // 执行子任务
                    // invokeAll(leftTask, rightTask);
                    // 合并结果
                    return leftTask.join() + rightTask.join();
                }
            }

        }


        // 随机生成四百万个[0,9]的整数
        int[] numbers = new int[4000000];
        Random random = new Random();
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = random.nextInt(9);
        }
        ForkJoinTask<Integer> task = new SumTask(numbers, 0, numbers.length - 1);

        // 创建并行度为2的ForkJoinPool
        ForkJoinPool forkJoinPool = new ForkJoinPool(2);
        Integer result = forkJoinPool.invoke(task);

        System.out.printf("[ForkJoinPool] Parallelism: %s; PoolSize: %s \n", forkJoinPool.getParallelism(), forkJoinPool.getPoolSize());
        System.out.printf("[ForkJoinPool] SumTaskResult: %s; StealCount: %s \n", result, forkJoinPool.getStealCount());

        /*try {
            Field workQueues = ForkJoinPool.class.getDeclaredField("workQueues");
            workQueues.setAccessible(true);
            Object[] workQueueArray = (Object[]) workQueues.get(forkJoinPool);
            System.out.println(workQueueArray.length);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        // 关闭线程池
        forkJoinPool.shutdown();
    }

    private static void executorsExample() {

        // 单线程的ThreadPoolExecutor
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        // 8个核心线程数的ThreadPoolExecutor
        ExecutorService fixedThreadPoolExecutor = Executors.newFixedThreadPool(8);


        // 单线程 ScheduledThreadPoolExecutor
        ScheduledExecutorService singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        // 8个核心线程的 ScheduledThreadPoolExecutor
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(8);

        // 创建一个ForkJoinPool, 并发度为当前系统的CPU核心数
        ExecutorService forkJoinPool = Executors.newWorkStealingPool();

        /*
         * 核心线程数0，最大线程数Integer.MAX_VALUE，非核心线程的保活时间60秒，使用SynchronousQueue
         * 支持动态创建线程，即当有新任务提交时，如果没有可用的线程，线程池会创建新的线程来执行任务，否则会重用这些线程。
         */
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

        // 默认线程工厂
        ThreadFactory threadFactory = Executors.defaultThreadFactory();

    }

    private static void prdExample() {

    }

}
