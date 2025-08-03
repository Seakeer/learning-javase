package me.seakeer.learning.javase.other.reflect;

import java.lang.reflect.*;

/**
 * JavaReflectExample;
 *
 * @author Seakeer;
 * @date 2024/8/8;
 */
public class JavaReflectExample {

    public static void main(String[] args) throws NoSuchFieldException {
        //Class.forName("me.seakeer.learning.javalang.reflect.JavaReflectExample")

        ReflectData<String> reflectData = new ReflectData<>();
        TypeVariable<? extends Class<? extends ReflectData>>[] typeParameters = reflectData.getClass().getTypeParameters();
        for (TypeVariable<? extends Class<? extends ReflectData>> typeParameter : typeParameters) {
            Class<? extends ReflectData> genericDeclaration = typeParameter.getGenericDeclaration();
            System.out.println("GenericDeclaration: " + genericDeclaration);
            Type[] bounds = typeParameter.getBounds();
            for (Type bound : bounds) {
                System.out.println("Bound: " + bound);
            }
            System.out.println(typeParameter.getName());
            AnnotatedType[] annotatedBounds = typeParameter.getAnnotatedBounds();
            for (AnnotatedType annotatedBound : annotatedBounds) {
                System.out.println("AnnotatedBound: " + annotatedBound);
            }

        }

        Field[] fields = reflectData.getClass().getDeclaredFields();
        for (Field field : fields) {
            printType(field);
        }
    }

    private static void printType(Field field) {
        String fieldName = field.getName();
        Type genericType = field.getGenericType();
        printType(genericType, fieldName);
    }

    private static void printType(Type genericType, String fieldName) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            System.out.println("------------" + "[" + "ParameterizedType: " + fieldName + "]" + "-------------");
            // 获取实际类型参数
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (Type type : actualTypeArguments) {
                System.out.println("ActualType: " + type);
                printType(type, fieldName);
            }

            // 获取原始类型
            Type rawType = parameterizedType.getRawType();
            System.out.println("RawType: " + rawType);

            // 获取所有者类型
            Type ownerType = parameterizedType.getOwnerType();
            if (ownerType != null) {
                System.out.println("OwnerType: " + ownerType);
            }
        }
        if (genericType instanceof WildcardType) {
            System.out.println("------------" + "[" + "WildcardType: " + fieldName + "]" + "-------------");
            WildcardType wildcardType = (WildcardType) genericType;
            Type[] lowerBounds = wildcardType.getLowerBounds();
            for (Type type : lowerBounds) {
                System.out.println("LowerBounds: " + type);
            }
            Type[] upperBounds = wildcardType.getUpperBounds();
            for (Type type : upperBounds) {
                System.out.println("UpperBounds: " + type);
            }
        }
        if (genericType instanceof GenericArrayType) {
            System.out.println("------------" + "[" + "GenericArrayType: " + fieldName + "]" + "-------------");
            GenericArrayType genericArrayType = (GenericArrayType) genericType;
            Type genericComponentType = genericArrayType.getGenericComponentType();
            System.out.println("GenericComponentType: " + genericComponentType);
        }
        if (genericType instanceof TypeVariable) {
            System.out.println("------------" + "[" + "TypeVariable: " + fieldName + "]" + "-------------");
            TypeVariable typeVariable = (TypeVariable) genericType;
            System.out.println("TypeVariable: " + typeVariable);
        }
    }
}
