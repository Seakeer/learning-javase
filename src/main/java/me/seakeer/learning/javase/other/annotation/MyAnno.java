package me.seakeer.learning.javase.other.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MyAnno;
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface MyAnno {

    String value();

    int data() default 0;

    String[] names() default {};

}
