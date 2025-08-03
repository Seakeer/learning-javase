package me.seakeer.learning.javase.basicsyntax;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * BasicSyntaxExample;
 * 基本语法示例
 *
 * @author Seakeer;
 * @date 2024/8/21;
 */
public class BasicSyntaxExample {

    public static void main(String[] args) {

        // 二进制
        binary();

        // 引用类型
        referenceType();
        // 引用类型使用示例
        referenceExample();

        // 值传递 & 引用传递
        valueRefPassing();
    }

    private static void valueRefPassing() {
        int num = 10;
        List<Integer> list = new ArrayList<>();
        System.out.println("Before: " + "num = " + num + ", list = " + list);

        modifyValue(num);
        modifyReference(list);
        modifyReferenceObj(list);

        System.out.println("After: " + "num = " + num + ", list = " + list);
    }

    private static void binary() {
        System.out.println(Integer.toBinaryString(1));
        System.out.println(Integer.toBinaryString(0));
        System.out.println(Integer.toBinaryString(-1));
    }

    private static void referenceType() {
        String strFroStrong = "StringForStrong";
        String strForSoft = new String("StringForSoft");
        String strForWeak = new String("StringForWeak");
        String strForPhantom = new String("StringForPhantom");
        SoftReference<String> softReference = new SoftReference<>(strForSoft);
        WeakReference<String> weakReference = new WeakReference<>(strForWeak);
        ReferenceQueue<String> referenceQueue = new ReferenceQueue<>();
        PhantomReference<String> phantomReference = new PhantomReference<>(strForPhantom, referenceQueue);

        System.out.println("Strong Reference: " + strFroStrong);
        System.out.println("Soft Reference: " + softReference.get());
        System.out.println("Weak Reference: " + weakReference.get());
        System.out.println("Phantom Reference: " + phantomReference.get());
        System.out.println("Reference Queue: " + referenceQueue.poll());

        // 去掉对象的强引用
        strFroStrong = null;
        strForSoft = null;
        strForWeak = null;
        strForPhantom = null;

        // 手动触发GC
        System.gc();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Strong Reference: " + strFroStrong);
        System.out.println("Soft Reference: " + softReference.get());
        System.out.println("Weak Reference: " + weakReference.get());
        System.out.println("Phantom Reference: " + phantomReference.get());
        System.out.println("Reference Queue: " + referenceQueue.poll());
    }

    private static void referenceExample() {
        ReferenceQueue<Byte[]> referenceQueue = new ReferenceQueue<>();
        Map<Object, Object> map = new HashMap<>();
        // 监控线程
        Thread thread = new Thread(() -> {
            try {
                int cnt = 0;
                WeakReference<Byte[]> k;
                // 被回收的弱引用对象会被加入到ReferenceQueue阻塞队列中，通过出队可进行监控
                while ((k = (WeakReference<Byte[]>) referenceQueue.remove(1000)) != null) {
                    cnt++;
                    // 可在此移除map中对应的弱引用KEY, WeakHashMap的处理机制
                    // map.remove(k);
                }
                System.out.println("回收字节数组个数: " + cnt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        for (int i = 0; i < 10000; i++) {
            // 创建1M 字节数组
            Byte[] bytes = new Byte[1024 * 1024];
            WeakReference<Byte[]> weakReference = new WeakReference<>(bytes, referenceQueue);
            map.put(weakReference, 1);
        }
        System.out.println("map.size: " + map.size());
    }

    public static void modifyValue(int value) {
        // 值传递，不影响原始值
        value = 100;
    }

    public static void modifyReference(List<Integer> reference) {
        // 引用传递，可修改引用所指向的目标，但不影响原始引用所指向的目标
        reference = new ArrayList<>();
        reference.add(1);
    }

    public static void modifyReferenceObj(List<Integer> reference) {
        // 引用传递，可修改引用所指向的对象的内容
        reference.add(2);
    }
}


