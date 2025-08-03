package me.seakeer.learning.javase.multithread;

import java.util.concurrent.*;

/**
 * AsyncExample;
 * 多线程异步示例
 *
 * @author Seakeer;
 * @date 2024/9/23;
 */
public class AsyncExample {

    public static void main(String[] args) {

        System.out.println("--------------------futureTaskExample------------------------");
        futureTaskExample();

        System.out.println("-------------------completableFutureExample------------------------");
        completableFutureExample();

        System.out.println("-------------------composeTaskExample------------------------");
        composeTaskExample();

        System.out.println("-------------------cfPrincipleExample------------------------");
        cfPrincipleExample();
    }

    private static void completableFutureExample() {
        cfAsyncExecuteTaskExample();
        System.out.println("--------------------------------------------");
        cfCombineAsyncTaskExample();
        System.out.println("--------------------------------------------");
        cfChainCallExample();
    }

    private static void cfCombineAsyncTaskExample() {
        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "CF1";
        });
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> "CF2");

        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(cf1, cf2);
        CompletableFuture<Void> allOfCf = CompletableFuture.allOf(cf1, cf2);

        System.out.println("[anyOf] isDone: " + anyOf.isDone() + " result: " + anyOf.join());
        System.out.println("[allOf] isDone: " + allOfCf.isDone() + " result: " + allOfCf.join());
    }


    /**
     * CompletableFuture异步执行任务示例
     */
    private static void cfAsyncExecuteTaskExample() {

        // 异步执行无返回值的任务；使用默认的线程池ForkJoinPool
        CompletableFuture<Void> runAsyncCf = CompletableFuture.runAsync(() ->
                System.out.printf("CompletableFuture.runAsync() ThreadName: %s \n", Thread.currentThread().getName()));

        // 异步执行有返回值的任务；使用默认的线程池ForkJoinPool
        CompletableFuture<String> supplyAsyncCf = CompletableFuture.supplyAsync(() ->
                "CompletableFuture.supplyAsync() ThreadName: " + Thread.currentThread().getName());

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // 异步执行无返回值的任务；使用自定义的线程池
        CompletableFuture<Void> runAsyncPoolCf = CompletableFuture.runAsync(() ->
                        System.out.printf("CompletableFuture.runAsync() ThreadName: %s \n", Thread.currentThread().getName()),
                executorService);
        // 异步执有返回值的任务；使用自定义的线程池
        CompletableFuture<String> supplyAsyncPoolCf = CompletableFuture.supplyAsync(() ->
                        "CompletableFuture.supplyAsync() ThreadName: " + Thread.currentThread().getName(),
                executorService);


        System.out.println("[runAsyncCf] result:  " + runAsyncCf.join());
        System.out.println("[supplyAsyncCf] result:  " + supplyAsyncCf.join());
        try {
            System.out.println("[runAsyncPoolCf] Result: " + runAsyncPoolCf.get());
            System.out.println("[supplyAsyncCf] Result: " + supplyAsyncPoolCf.get());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    private static void cfChainCallExample() {

        CompletableFuture<String> cfChain1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("CF_CHAIN_1, ThreadName: " + Thread.currentThread().getName());
            return "CF_CHAIN_1_RESULT";
        });
        CompletableFuture<String> cfChain2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("CF_CHAIN_2, ThreadName: " + Thread.currentThread().getName());
            return "CF_CHAIN_2_RESULT";
        });

        // 链式调用
        cfChain1.thenCompose(cfChain1Result -> cfChain2)
                .handle((cfChain2Result, throwable) -> {
                    System.out.println("[handle] Result: " + cfChain2Result + " Throwable: " + throwable);
                    throw new RuntimeException("HandleFailed");
                })
                .whenComplete((cfResult, throwable) -> {
                    System.out.println("[whenComplete] Result: " + cfResult + "; Throwable: " + throwable.getMessage());
                });

        // 依赖2个CF的回调任务
        cfChain1.runAfterBoth(cfChain2, () -> System.out.println("runAfterBoth"));
        cfChain1.runAfterEither(cfChain2, () -> System.out.println("runAfterEither"));

        cfChain1.thenAcceptBoth(cfChain2, (cfChain1Result, cfChain2Result) ->
                System.out.println("thenAcceptBoth: " + cfChain1Result + " " + cfChain2Result));
        cfChain1.acceptEither(cfChain2, cfChainResult ->
                System.out.println("acceptEither: " + cfChainResult));

        cfChain1.thenCombine(cfChain2, (cfChain1Result, cfChain2Result) -> {
            System.out.println("thenCombine: " + cfChain1Result + " " + cfChain2Result);
            return cfChain1Result + " " + cfChain2Result;
        });
    }


    private static void composeTaskExample() {

        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> "Task1");
        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> "Task2");

        // 并发执行
        CompletableFuture<String> resultCf = CompletableFuture.allOf(task1, task2)
                //顺序执行
                .thenApply(result -> true)
                // 条件执行
                .thenApply(v -> {
                    if (v) {
                        return "YES";
                    }
                    return "NO";
                })
                // 组合结果
                .thenCombine(task2, (conditionResult, task2Result) -> conditionResult + " " + task2Result)
                // 异常处理
                .exceptionally(ex -> "Error handled: " + ex.getMessage());
        System.out.println(resultCf.join());
    }

    private static void cfPrincipleExample() {

        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("CF1: CF1_DEP_0; ThreadName: " + Thread.currentThread().getName());
            return "CF1";
        });


        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("CF2")
                // CF2 stack 入栈元素
                .whenComplete((cf2Result, cf2Exception) -> {
                    System.out.println("CF2: CF2_DEP_0; ThreadName: " + Thread.currentThread().getName());
                });

        // CF1 stack 入栈元素
        CompletableFuture<String> cf3 = cf1.thenApply(cf1Result -> {
            System.out.println("CF3: " + cf1Result + " --> " + "CF3_DEP_CF1; ThreadName: " + Thread.currentThread().getName());
            return "CF3";
        });

        // CF2 stack入栈新元素，stack.next执行该新元素
        CompletableFuture<Void> cf5 = cf2.thenAccept(cf2Result ->
                System.out.println("CF5: " + cf2Result + " --> " + "CF5_DEP_CF2; ThreadName: " + Thread.currentThread().getName()));

        CompletableFuture<String> cf4 = cf1.thenCombine(cf2,
                (cf1Result, cf2Result) -> {
                    System.out.println("CF4: " + cf1Result + "," + cf2Result + " --> " + "CF4_DEP_CF1,CF2; ThreadName: " + Thread.currentThread().getName());
                    return "CF4";
                });

        CompletableFuture<Void> cf6 = CompletableFuture.allOf(cf3, cf4, cf5)
                .whenComplete((cf6Result, cf6Exception) -> {
                    System.out.println("CF6: CF3,CF4,CF5 --> CF6_DEP_CF3,CF4,CF5; ThreadName: " + Thread.currentThread().getName());
                });

        try {
            System.out.println("CF1 Result: " + cf1.get());
            System.out.println("CF2 Result: " + cf2.get());
            System.out.println("CF3 Result: " + cf3.get());
            System.out.println("CF4 Result: " + cf4.get());
            System.out.println("CF5 Result: " + cf5.get());
            System.out.println("CF6 Result: " + cf6.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void futureTaskExample() {

        FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "FutureTask";
            }
        });

        new Thread(futureTask).start();
        try {
            System.out.println("FutureTask Result: " + futureTask.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
