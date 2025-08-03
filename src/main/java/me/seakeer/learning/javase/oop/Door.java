package me.seakeer.learning.javase.oop;

/**
 * 抽象类 Door;
 *
 * @author Seakeer;
 * @date 2024/8/17;
 */
public abstract class Door implements Turnable {

    public String material;

    public Door() {
        System.out.println("Door Constructor Executed");
    }

    @Override
    public void open() {
        System.out.println("Open the door");
    }

    @Override
    public void close() {
        System.out.println("Close then door");
    }

    public void goThrough() {
        System.out.println("Go Through");
    }

    /**
     * 抽象方法
     */
    public abstract void lock();

    /**
     * 抽象方法
     */
    public abstract void unlock();
}
