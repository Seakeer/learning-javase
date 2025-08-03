package me.seakeer.learning.javase.multithread.classiccase.bank;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * BankSystem;
 *
 * @author Seakeer;
 * @date 2024/9/28;
 */
public class BankSystem {

    private final ConcurrentHashMap<Integer, AtomicLong> ACCOUNT_BALANCE_MAP = new ConcurrentHashMap<>();

    private final Lock LOCK = new ReentrantLock();

    public boolean exists(int account) {
        return ACCOUNT_BALANCE_MAP.containsKey(account);
    }

    public boolean createAccount(int account) {
        ACCOUNT_BALANCE_MAP.putIfAbsent(account, new AtomicLong(0));
        return true;
    }

    public boolean deleteAccount(int account) {
        ACCOUNT_BALANCE_MAP.remove(account);
        return true;
    }

    public boolean deposit(int account, long amount) {
        if (amount <= 0) {
            return false;
        }
        AtomicLong atomicLong = ACCOUNT_BALANCE_MAP.get(account);
        if (null == atomicLong) {
            return false;
        }
        atomicLong.addAndGet(amount);
        return true;
    }

    public boolean withdraw(int account, long amount) {
        if (amount <= 0) {
            return false;
        }
        AtomicLong atomicLong = ACCOUNT_BALANCE_MAP.get(account);
        if (null == atomicLong || atomicLong.get() < amount) {
            return false;
        }
        atomicLong.addAndGet(-amount);
        return true;
    }

    public boolean transfer(int fromAccount, int toAccount, long amount) {
        if (!exists(fromAccount) || !exists(toAccount)) {
            return false;
        }
        AtomicLong fromBalance = ACCOUNT_BALANCE_MAP.get(fromAccount);
        if (null == fromBalance || fromBalance.get() < amount) {
            return false;
        }

        AtomicLong toBalance = ACCOUNT_BALANCE_MAP.get(toAccount);
        if (toBalance == null) {
            return false;
        }
        // 保证对2个账户的修改是原子性的
        if (LOCK.tryLock()) {
            fromBalance.addAndGet(-amount);
            toBalance.addAndGet(amount);
            LOCK.unlock();
            return true;
        }
        return false;
    }

    private void doTransfer(int fromAccount, int toAccount, long amount) {
        ACCOUNT_BALANCE_MAP.get(fromAccount).addAndGet(-amount);
        ACCOUNT_BALANCE_MAP.get(toAccount).addAndGet(amount);
    }

    public long getBalance(int account) {
        return ACCOUNT_BALANCE_MAP.getOrDefault(account, new AtomicLong(0)).get();
    }

    public void viewAccounts() {
        System.out.println("Accounts: " + ACCOUNT_BALANCE_MAP);
    }

    public static void main(String[] args) {

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        BankSystem bankSystem = new BankSystem();
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            executorService.execute(() -> bankSystem.createAccount(random.nextInt(10)));
            executorService.execute(() -> bankSystem.deposit(random.nextInt(10), random.nextInt(100)));
            executorService.execute(() -> bankSystem.withdraw(random.nextInt(10), random.nextInt(100)));
            executorService.execute(() -> bankSystem.transfer(random.nextInt(10), random.nextInt(10), random.nextInt(100)));
            executorService.execute(bankSystem::viewAccounts);
        }

        executorService.shutdown();
    }
}
