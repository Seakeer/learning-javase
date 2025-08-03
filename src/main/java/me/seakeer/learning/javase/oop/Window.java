package me.seakeer.learning.javase.oop;

/**
 * 具体类 Window;
 *
 * @author Seakeer;
 * @date 2024/8/17;
 */
public class Window implements Turnable {

    public Window() {
        System.out.println("Window Constructor Executed");
    }

    public void open() {
        System.out.println("Open the window");
    }

    public void close() {
        System.out.println("Close the window");
    }
}
