package me.seakeer.learning.javase.other.reflect;

import java.lang.annotation.ElementType;
import java.lang.reflect.*;
import java.util.List;

/**
 * ReflectModelClassExample;
 * 反射模型类示例
 *
 * @author Seakeer;
 * @date 2024/8/12;
 */
public class ReflectModelClassExample {

    private String name;

    private static final Integer STATIC_FINAL_NUMBER = 1;

    public ReflectModelClassExample(String name) {
        this.name = name;
    }

    public ReflectModelClassExample() {
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static void main(String[] args) {
        classExample();
        fieldExample();
        methodExample();
        constructorExample();
        modifierExample();
    }

    private static void modifierExample() {
        System.out.println("----------------- [Modifier Example] ----------------");
        try {
            Field field = ReflectModelClassExample.class.getDeclaredField("STATIC_FINAL_NUMBER");
            System.out.println(Modifier.toString(field.getModifiers()));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    private static void constructorExample() {
        System.out.println("----------------- [Constructor Example] ----------------");

    }

    private static void methodExample() {
        System.out.println("----------------- [Method Example] ----------------");
        try {
            Method method = ReflectModelClassExample.class.getDeclaredMethod("setName", String.class);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    private static void fieldExample() {
        System.out.println("----------------- [Field Example] ----------------");
        try {
            Field field = ReflectModelClassExample.class.getDeclaredField("name");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static void classExample() {
        System.out.println("----------------- [Class Example] ----------------");
        // 创建对象
        crtInstance();

        // 获取类型
        getType();

        // 类型检查
        checkType();

        // 获取类成员
        getClassMember();

    }

    private static void checkType() {
        System.out.println("[IsPrimitive]: " + int.class.isPrimitive() + "; " + Integer.class.isPrimitive());
        System.out.println("[IsEnum]: " + ElementType.class.isEnum());
        System.out.println("[IsArray]: " + String[].class.isArray());
        System.out.println("[IsInterface]: " + List.class.isInterface());
        System.out.println("[IsLocalClass]: " + ReflectModelClassExample.class.isLocalClass());
    }

    private static void getClassMember() {
        Field[] declaredFields = ReflectModelClassExample.class.getFields();
        Method[] declaredMethods = ReflectModelClassExample.class.getDeclaredMethods();
        Constructor<?>[] declaredConstructors = ReflectModelClassExample.class.getDeclaredConstructors();
        showMembers(declaredFields, declaredMethods, declaredConstructors);
    }

    private static void showMembers(Member[]... members) {
        for (Member[] memberArr : members) {
            for (Member member : memberArr) {
                System.out.println(member);
            }
        }
    }

    private static void getType() {
        Class<?> clz = Integer.class;
        System.out.println("[TypeName]: " + clz.getTypeName());
        System.out.println("[SimpleName]: " + clz.getSimpleName());
        Class<?> superclass = clz.getSuperclass();
        System.out.println("[SuperClass]: " + superclass.getTypeName());
        Class<?>[] interfaces = clz.getInterfaces();
        for (Class<?> superInterface : interfaces) {
            System.out.println("[Interface]: " + superInterface.getTypeName());
        }
    }

    private static void crtInstance() {
        try {
            Class<?> clzObj = Class.forName("me.seakeer.learning.javase.other.reflect.ReflectModelClassExample");
            System.out.println("[Class]: " + clzObj.getTypeName());
            ReflectModelClassExample obj = (ReflectModelClassExample) clzObj.newInstance();
            System.out.println("[Instance]: " + obj);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
