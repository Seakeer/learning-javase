package me.seakeer.learning.javase.oop;

/**
 * 具体类 CommonDoor;
 * 普通门
 *
 * @author Seakeer;
 * @date 2024/8/16;
 */
public class CommonDoor extends Door {
    public CommonDoor() {
        System.out.println("Common Constructor Executed");
    }

    @Override
    public void lock() {
        System.out.println("Lock Manually");
    }

    @Override
    public void unlock() {
        System.out.println("Unlock Manually");
    }
}
