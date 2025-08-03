package me.seakeer.learning.javase.oop;

/**
 * ObjectLifecycleExample;
 * 对象生命周期示例
 *
 * @author Seakeer;
 * @date 2024/8/22;
 */
public class ObjectLifecycleExample {
    public static void main(String[] args) {

        // 创建对象 & 初始化对象
        Student student = new Student("Seakeer", "MALE");

        // 使用对象
        student.study("Java");

        // 对象不可达
        student = null;

        // 主动触发垃圾回收，回收销毁对象
        System.gc();
    }
}