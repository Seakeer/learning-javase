package me.seakeer.learning.javase.oop;

/**
 * AbstractClassInterfaceExample;
 * 抽象类和接口示例;
 *
 * @author Seakeer;
 * @date 2024/8/17;
 */
public class AbstractClassInterfaceExample {

    public static void main(String[] args) {

        Window window = new Window();
        window.open();
        window.close();

        Door cd = new CommonDoor();
        cd.unlock();
        cd.open();
        cd.goThrough();
        cd.close();
        cd.lock();

        IntelligentDoor id = new IntelligentDoor();
        id.startup();
    }
}
