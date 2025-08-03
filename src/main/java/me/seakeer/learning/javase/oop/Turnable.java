package me.seakeer.learning.javase.oop;

/**
 * 接口Turnable;
 * 抽象集成open close on off等方法;
 *
 * @author Seakeer;
 * @date 2024/8/17;
 */
interface Turnable {

    /**
     * 未实现的方法 抽象方法
     */
    void open();

    /**
     * 未实现的方法 抽象方法
     */
    void close();

    /**
     * 默认方法
     */
    default void on() {
        open();
        System.out.println("Turn On");
    }

    /**
     * 默认方法
     */
    default void off() {
        close();
        System.out.println("Turn Off");
    }
}
