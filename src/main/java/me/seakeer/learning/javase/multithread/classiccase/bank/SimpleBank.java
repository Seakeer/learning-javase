package me.seakeer.learning.javase.multithread.classiccase.bank;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SimpleBank;
 *
 * @author Seakeer;
 * @date 2024/9/28;
 */
public class SimpleBank {

    private static final Lock LOCK = new ReentrantLock();

    private static final Map<Integer, Long> ACCOUNT_BALANCE_MAP = new ConcurrentHashMap<>();

    public boolean exists(int account) {
        return ACCOUNT_BALANCE_MAP.containsKey(account);
    }

    public boolean createAccount(int account) {
        if (LOCK.tryLock()) {
            ACCOUNT_BALANCE_MAP.putIfAbsent(account, 0L);
            LOCK.unlock();
            return true;
        }
        return false;
    }

    public boolean deleteAccount(int account) {
        if (LOCK.tryLock()) {
            ACCOUNT_BALANCE_MAP.remove(account);
            LOCK.unlock();
            return true;
        }
        return false;
    }

    public boolean deposit(int account, long amount) {
        if (amount <= 0 || !exists(account)) {
            return false;
        }
        if (LOCK.tryLock()) {
            Long balance = ACCOUNT_BALANCE_MAP.get(account);
            if (null == balance) {
                return false;
            }
            ACCOUNT_BALANCE_MAP.put(account, balance + amount);
            LOCK.unlock();
            return true;
        }
        return false;
    }

    public boolean withdraw(int account, long amount) {
        if (amount <= 0 || !exists(account)) {
            return false;
        }
        if (LOCK.tryLock()) {
            Long balance = ACCOUNT_BALANCE_MAP.get(account);
            if (null == balance || balance < amount) {
                return false;
            }
            ACCOUNT_BALANCE_MAP.put(account, balance - amount);
            LOCK.unlock();
            return true;
        }
        return false;
    }

    public boolean transfer(int fromAccount, int toAccount, long amount) {
        if (!exists(fromAccount) || !exists(toAccount)) {
            return false;
        }
        long balance = getBalance(fromAccount);
        if (balance < amount) {
            return false;
        }
        return doTransfer(fromAccount, toAccount, amount);
    }

    private boolean doTransfer(int fromAccount, int toAccount, long amount) {
        if (LOCK.tryLock()) {
            Long fromBalance = ACCOUNT_BALANCE_MAP.get(fromAccount);
            Long toBalance = ACCOUNT_BALANCE_MAP.get(toAccount);
            if (null == fromBalance || null == toBalance || fromBalance < amount) {
                return false;
            } else {
                ACCOUNT_BALANCE_MAP.put(fromAccount, fromBalance - amount);
                ACCOUNT_BALANCE_MAP.put(toAccount, toBalance + amount);
            }
            LOCK.unlock();
            return true;
        }
        return false;
    }

    public long getBalance(int account) {
        return ACCOUNT_BALANCE_MAP.getOrDefault(account, 0L);
    }

    public void viewAccounts() {
        System.out.println("Accounts: " + ACCOUNT_BALANCE_MAP);
    }

    public static void main(String[] args) {

        SimpleBank simpleBank = new SimpleBank();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            executorService.execute(() -> simpleBank.createAccount(random.nextInt(10)));
            executorService.execute(() -> simpleBank.deposit(random.nextInt(10), random.nextInt(100)));
            executorService.execute(() -> simpleBank.withdraw(random.nextInt(10), random.nextInt(100)));
            executorService.execute(() -> simpleBank.transfer(random.nextInt(10), random.nextInt(10), random.nextInt(100)));
            executorService.execute(simpleBank::viewAccounts);
        }

        executorService.shutdown();
    }
}
