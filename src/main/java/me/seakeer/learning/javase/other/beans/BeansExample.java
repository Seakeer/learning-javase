package me.seakeer.learning.javase.other.beans;

import java.beans.*;

/**
 * BeansExample;
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
public class BeansExample {

    public static void main(String[] args) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(MyBean.class);
            BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor();
            System.out.printf("[BeanDescriptor][name: %s; shortDescription: %s]\n",
                    beanDescriptor.getName(),
                    beanDescriptor.getShortDescription());
            System.out.println("----------------------------------------------------");

            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                System.out.printf("[PropertyDescriptor][name: %s; typeName: %s;]\n",
                        propertyDescriptor.getName(),
                        propertyDescriptor.getPropertyType().getName());
            }
            System.out.println("----------------------------------------------------");

            MethodDescriptor[] methodDescriptors = beanInfo.getMethodDescriptors();
            for (MethodDescriptor methodDescriptor : methodDescriptors) {
                System.out.printf("[MethodDescriptor][name: %s]\n", methodDescriptor.getName());
            }
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
    }
}
