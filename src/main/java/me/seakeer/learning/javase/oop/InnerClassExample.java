package me.seakeer.learning.javase.oop;

/**
 * InnerClassExample;
 * 内部类示例
 *
 * @author Seakeer;
 * @date 2024/8/19;
 */
public class InnerClassExample {

    public static void main(String[] args) {

        OuterClass outerClass = new OuterClass("Outer", 9);

        OuterClass.MemberInnerClass memberInnerClass = outerClass.new MemberInnerClass("MemberInner");
        memberInnerClass.display();

        OuterClass.StaticInnerClass staticInnerClass = new OuterClass.StaticInnerClass("StaticInner", 99);
        staticInnerClass.display();

        outerClass.funcWitchLocalInner();

        outerClass.funcWitchAnonymousInner();
    }
}
