package me.seakeer.learning.javase.oop;

/**
 * 学生类
 * Student;
 *
 * @author Seakeer;
 * @date 2024/8/22;
 */
public class Student {

    // 公有静态变量，学校
    public static String school;

    // 静态代码块
    static {
        school = "XiDian University";
        System.out.println("static code block in Student executed");
    }

    // 私有变量姓名和性别
    private String name;
    private String sex;

    // 构造方法
    public Student(String name, String sex) {
        //调用无参的构造方法
        this();
        this.name = name;
        this.sex = sex;
    }

    public Student() {
        this.noting();
    }

    public void noting() {
    }

    //公有静态成员方法
    public static void visitSchool() {
        System.out.println("Welcome to visit " + school);
    }

    //公有成员方法
    public void setName(String name) {
        this.name = name;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getName() {
        return name;
    }

    public String getSex() {
        return this.sex;
    }

    public void study(String course) {
        System.out.println(name + " is studying " + course);
    }
}