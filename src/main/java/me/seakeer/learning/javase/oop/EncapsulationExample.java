package me.seakeer.learning.javase.oop;

/**
 * ClassEncapsulationExample;
 * 封装示例
 *
 * @author Seakeer;
 * @date 2024/8/22;
 * @see Student
 */
public class EncapsulationExample {
    public static void main(String[] args) {
        Student stu1 = new Student("Stu1", "Male");
        stu1.visitSchool();

        Student stu2 = new Student();
        stu2.setName("Stu2");
        stu2.setSex("Female");
        stu2.study("Java");
        Student.visitSchool();
    }
}