package me.seakeer.learning.javase.oop;

/**
 * InheritanceExample;
 * 继承示例
 *
 * @author Seakeer;
 * @date 2024/8/22;
 */
public class InheritanceExample {
    public static void main(String[] args) {

        // 无参构造创建
        Person per1 = new Person();
        // 有参构造创建
        Person per2 = new Person("Bera");

        Teacher teacher1 = new Teacher();
        Teacher teacher2 = new Teacher("XiDian", "Professor");

        //子类向上转型
        Person per3 = new Teacher();
        Person per4 = new Teacher("XiJunDian", "Professor");

        System.out.println(per1.name);
        System.out.println(per2.name);
        System.out.println(teacher1.name);
        System.out.println(teacher2.name);
        System.out.println(per3.name);
        System.out.println(per4.name);

        per1.introduceSelf();
        per3.introduceSelf();
        teacher2.introduceSelf();
    }
}


/**
 * 父类
 */
class Person {

    public String name;

    public Person() {
        System.out.println("Person Constructor executed");
    }

    public Person(String name) {
        this.name = name;
        System.out.println("Person Param-Constructor executed");
    }

    public void introduceSelf() {
        System.out.println("I am a person");
    }
}

/**
 * 子类
 */
class Teacher extends Person {

    /**
     * 学校
     */
    private String school;

    /**
     * 职称
     */
    private String title;

    // 构造方法
    public Teacher(String school, String title) {
        super("Jade");
        this.school = school;
        this.title = title;
        System.out.println("Teacher Para-Constructor executed");
    }

    public Teacher() {
        System.out.println("Teacher Constructor executed");
    }

    @Override
    public void introduceSelf() {
        super.introduceSelf();
        System.out.println("I am a teacher");
    }

    public void teach(String course) {
        System.out.println("I am teaching " + course);
    }

    public void teach(String course, int years) {
        System.out.println("I am teaching " + course + " for " + years + " years.");
    }
}