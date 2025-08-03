package me.seakeer.learning.javase.oop;

import java.util.function.Supplier;

/**
 * OuterClass;
 *
 * @author Seakeer;
 * @date 2024/8/22;
 */
public class OuterClass {

    private static String hello = "Hello";

    private String outer;

    private Integer value;

    public OuterClass(String outer, Integer value) {
        this.outer = outer;
        this.value = value;
    }

    /**
     * 成员内部类
     */
    public class MemberInnerClass {

        private String memberInner;

        private Integer value;

        public MemberInnerClass(String memberInner) {
            this.memberInner = memberInner;
            this.value = OuterClass.this.value * 2;
        }

        public void display() {
            System.out.println(hello + "; " + outer + "; " + OuterClass.this.value + "; " + memberInner + "; " + value + ";");
        }
    }


    /**
     * 静态内部类
     */
    public static class StaticInnerClass {

        private String staticInner;

        private Integer value;

        public StaticInnerClass(String staticInner, Integer value) {
            this.staticInner = staticInner;
            this.value = value;
        }

        public void display() {
            System.out.println(hello + "; " + staticInner + "; " + value + ";");
        }
    }


    public void funcWitchLocalInner() {
        // 局部内部类 --- 方法内部类
        class LocalInnerClass {

            private String localInner;

            private Integer value;

            public LocalInnerClass(String localInner) {
                this.localInner = localInner;
                this.value = OuterClass.this.value * 2;
            }

            public void display() {
                System.out.println(hello + "; " + outer + "; " + OuterClass.this.value + "; " + localInner + "; " + value + ";");
            }
        }

        LocalInnerClass localInnerClass = new LocalInnerClass("LocalInner");
        localInnerClass.display();
    }


    public void funcWitchAnonymousInner() {
        // 匿名内部类
        Supplier<String> supplier = new Supplier<String>() {
            @Override
            public String get() {
                return hello + "; " + outer + "; " + value + ";";
            }
        };

        System.out.println(supplier.get());
    }

    public static void main(String[] args) {

        OuterClass outerClass = new OuterClass("Outer", 9);

        OuterClass.MemberInnerClass memberInnerClass = outerClass.new MemberInnerClass("MemberInner");
        memberInnerClass.display();

        StaticInnerClass staticInnerClass = new OuterClass.StaticInnerClass("StaticInner", 99);
        staticInnerClass.display();

        outerClass.funcWitchLocalInner();

        outerClass.funcWitchAnonymousInner();
    }
}