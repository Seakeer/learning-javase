package me.seakeer.learning.javase.multithread.classiccase.dining;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DiningPhilosopherTest;
 *
 * @author Seakeer;
 * @date 2024/9/29;
 */
public class DiningPhilosopherTest {

    public static void main(String[] args) {

        // 5个线程对应5个哲学家
        ExecutorService executor = Executors.newFixedThreadPool(5);

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            // 输入每个哲学家进餐的次数
            int n = scanner.nextInt();
            if (n == 0) {
                executor.shutdown();
                return;
            }
            // 获取进餐方案
            List<List<Integer>> solution = diningSolution(n, executor);
            // 输出结果
            System.out.println(solution);
//            dining(n, executor);
        }
    }

    private static List<List<Integer>> diningSolution(int n, ExecutorService executor) {
        List<List<Integer>> solution = new CopyOnWriteArrayList<>();
        CompletableFuture[] philosopherTaskArr = new CompletableFuture[5];

        // 解决方案
        DiningPhilosophers diningPhilosophers = new DiningPhilosophers();

        // 创建5个哲学家对应的就餐任务
        for (int i = 0; i < 5; i++) {
            int philosopher = i;
            philosopherTaskArr[i] = CompletableFuture.runAsync(() -> {
                try {
                    for (int j = 0; j < n; j++) {
                        diningPhilosophers.wantsToEat(
                                philosopher,
                                () -> solution.add(Arrays.asList(philosopher, 1, 1)),
                                () -> solution.add(Arrays.asList(philosopher, 2, 1)),
                                () -> solution.add(Arrays.asList(philosopher, 0, 3)),
                                () -> solution.add(Arrays.asList(philosopher, 1, 2)),
                                () -> solution.add(Arrays.asList(philosopher, 2, 2))
                        );
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, executor);
        }

        CompletableFuture.allOf(philosopherTaskArr).join();
        return solution;
    }


    private static void dining(int n, ExecutorService executor) {


        CompletableFuture[] philosopherTaskArr = new CompletableFuture[5];

        // 解决方案
        DiningPhilosophers diningPhilosophers = new DiningPhilosophers();

        // 记录输出
        StringBuffer output = new StringBuffer("[");

        // 创建5个哲学家对应的就餐任务
        for (int i = 0; i < 5; i++) {
            int philosopher = i;
            philosopherTaskArr[i] = CompletableFuture.runAsync(() -> {
                try {
                    for (int j = 0; j < n; j++) {
                        diningPhilosophers.wantsToEat(
                                philosopher,
                                () -> output.append(String.format("[%d,1,1],", philosopher)),
                                () -> output.append(String.format("[%d,2,1],", philosopher)),
                                () -> output.append(String.format("[%d,0,3],", philosopher)),
                                () -> output.append(String.format("[%d,1,2],", philosopher)),
                                () -> output.append(String.format("[%d,2,2],", philosopher))
                        );
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, executor);
        }

        CompletableFuture.allOf(philosopherTaskArr).join();

        output.setCharAt(output.length() - 1, ']');

        // 输出结果
        System.out.println(output);
    }

}
