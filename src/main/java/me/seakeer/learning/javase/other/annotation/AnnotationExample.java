package me.seakeer.learning.javase.other.annotation;

/**
 * AnnotationExample;
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
@MyAnno(value = "TestAnno", data = 1, names = {"Anno", "Vino"})
public class AnnotationExample {
    public static void main(String[] args) {
        MyAnno myAnno = AnnotationExample.class.getAnnotation(MyAnno.class);
        System.out.println(myAnno.value());
        System.out.println(myAnno.data());
        for (String name : myAnno.names()) {
            System.out.println(name);
        }
    }
}
