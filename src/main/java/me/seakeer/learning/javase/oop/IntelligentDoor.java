package me.seakeer.learning.javase.oop;

/**
 * 具体类 IntelligentDoor;
 * 智能门
 *
 * @author Seakeer;
 * @date 2024/8/16;
 */
public class IntelligentDoor extends Door implements Alarm, AutoId {
    private volatile boolean isWorking;

    public IntelligentDoor() {
        System.out.println("IntelligentDoor Constructor Executed");
    }

    public void startup() {
        isWorking = true;
        while (isWorking) {
            if (!securityCheck()) {
                alert();
            }
            if (autoId()) {
                unlock();
                open();
                goThrough();
                close();
                lock();
            }
            shutdown();
        }
    }

    public void shutdown() {
        isWorking = false;
    }

    private boolean securityCheck() {
        System.out.println("Security Check Successfully");
        return true;
    }

    @Override
    public void alert() {
        System.out.println("Alert");
    }

    @Override
    public boolean autoId() {
        System.out.println("Identify Automatically");
        return true;
    }

    @Override
    public void lock() {
        System.out.println("Lock Automatically");
    }

    @Override
    public void unlock() {
        System.out.println("Unlock Automatically");
    }
}