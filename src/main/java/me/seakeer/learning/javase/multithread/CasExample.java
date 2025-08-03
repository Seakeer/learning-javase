package me.seakeer.learning.javase.multithread;

import java.util.Arrays;
import java.util.concurrent.atomic.*;

/**
 * CasExample;
 * CAS原子操作示例
 *
 * @author Seakeer;
 * @date 2024/9/2;
 */
public class CasExample {

    public static void main(String[] args) throws InterruptedException {
        // CAS 原子类示例
        casAtomicClassExample();

        Thread.sleep(2000);
        System.out.println("----------------------------");

        // CAS 单一共享变量一致性示例（无锁同步）
        casSingleVarLockFreeExample();

        Thread.sleep(2000);
        System.out.println("----------------------------");


        // CAS 乐观锁示例
        casOptimisticLockExample();
    }

    private static void casAtomicClassExample() {
        AtomicLong atomicLong = new AtomicLong(9L);
        atomicLong.compareAndSet(9L, 99L);
        System.out.println(atomicLong.get());

        AtomicReference<String> atomicReference = new AtomicReference<>("Seakeer");
        atomicReference.compareAndSet("Seakeer", "Seakeer1");
        System.out.println(atomicReference.get());

        AtomicStampedReference<String> atomicStampedReference = new AtomicStampedReference<>("Seakeer", 1);
        atomicStampedReference.compareAndSet("Seakeer", "Seakeer1", atomicStampedReference.getStamp(), atomicStampedReference.getStamp() + 1);
        System.out.println(atomicStampedReference.getReference());

        Data dataForAmr = new Data("Seakeer", 9L);
        AtomicMarkableReference<Data> atomicMarkableReference = new AtomicMarkableReference<>(dataForAmr, false);
        atomicMarkableReference.compareAndSet(dataForAmr, new Data("Seakeer1", 99L), atomicMarkableReference.isMarked(), !atomicMarkableReference.isMarked());
        System.out.println(atomicMarkableReference.getReference());

        // value字段必须由volatile修饰且对于当前的Updater所在区域是可访问的。
        AtomicLongFieldUpdater<Data> value = AtomicLongFieldUpdater.newUpdater(Data.class, "value");
        Data data = new Data("Seakeer", 9L);
        value.compareAndSet(data, 9L, 99L);
        System.out.println(value.get(data));
        System.out.println(data.getValue());

        String[] strArr = new String[]{"Seakeer", "Seakeer2"};
        AtomicReferenceArray<String> atomicReferenceArray = new AtomicReferenceArray<>(strArr);
        atomicReferenceArray.compareAndSet(0, "Seakeer", "Seakeer1");
        System.out.println(atomicReferenceArray.get(0));
        // 原数组不会更新
        System.out.println(Arrays.toString(strArr));

    }

    public static class Data {

        private final String key;

        /**
         * 原子更新类的字段需要满足以下4个条件：
         * 被操作的字段不能是static类型；
         * 被操纵的字段不能是final类型；
         * 被操作的字段必须被volatile修饰；
         * 属性必须对于当前的Updater所在区域是可见的。
         */
        protected volatile long value;

        public Data(String key, long value) {
            this.key = key;
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        @Override
        public String toString() {
            return key + " = " + value;
        }
    }

    private static void casSingleVarLockFreeExample() {
        TicketService ticketService = new TicketService(9);
        new Thread(ticketService, "TicketWindow1").start();
        new Thread(ticketService, "TicketWindow2").start();
        new Thread(ticketService, "TicketWindow3").start();
        new Thread(ticketService, "TicketWindow4").start();
    }

    private static void casOptimisticLockExample() {

        DataService dataService = new DataService(new Data("Seakeer", 9L));
        new Thread(dataService, "DS1").start();
        new Thread(dataService, "DS2").start();
        new Thread(dataService, "DS3").start();
        new Thread(dataService, "DS4").start();
    }

    public static class DataService implements Runnable {
        private AtomicStampedReference<Data> atomicData;

        public DataService(Data data) {
            this.atomicData = new AtomicStampedReference<>(data, 1);
        }

        @Override
        public void run() {
            while (true) {
                if (done()) {
                    break;
                }
            }
            System.out.println("Result:" + atomicData.getReference());
        }

        public boolean done() {
            while (true) {
                int stamp = atomicData.getStamp();
                Data curData = atomicData.getReference();
                if (curData.getValue() <= 0) {
                    return true;
                }
                Data newData = new Data(Thread.currentThread().getName(), curData.getValue() - 1);
                boolean result = this.atomicData.compareAndSet(curData, newData, stamp, stamp + 1);
                if (result) {
                    System.out.println(atomicData.getReference());
                    return newData.getValue() <= 0;
                }
            }

        }
    }

    /**
     * 票务;
     */
    public static class TicketService implements Runnable {

        public TicketService(int tickets) {
            this.tickets = new AtomicInteger(tickets);
        }

        /**
         * 共享资源
         */
        private final AtomicInteger tickets;

        @Override
        public void run() {
            while (true) {
                boolean isSelloutCas = saleTicketCas();
                if (isSelloutCas) {
                    break;
                }
            }
        }

        private boolean saleTicketCas() {
            int ticket = tickets.getAndDecrement();
            if (ticket > 0) {
                System.out.println(Thread.currentThread().getName() + " 出售第 " + ticket + " 张票" + " --- CAS");
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

}



