package me.seakeer.learning.javase.other.bignumber;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * BigNumberExample;
 *
 * @author Seakeer;
 * @date 2024/9/15;
 */
public class BigNumberExample {

    public static void main(String[] args) {

        bigIntegerExample();

        System.out.println("--------------------------------------------");

        bigDecimalExample();
    }

    private static void bigDecimalExample() {
        BigDecimal bigDecimal1 = new BigDecimal("10.00");
        BigDecimal bigDecimal2 = new BigDecimal("-3.00");
        System.out.println(bigDecimal1.add(bigDecimal2));
        System.out.println(bigDecimal1.subtract(bigDecimal2));
        System.out.println(bigDecimal1.multiply(bigDecimal2));
        System.out.println(bigDecimal1.remainder(bigDecimal2));
        System.out.println(bigDecimal1.divide(bigDecimal2, 2, RoundingMode.HALF_UP));
        System.out.println(bigDecimal1.divide(bigDecimal2, RoundingMode.CEILING));
        System.out.println(bigDecimal2.toPlainString());
        for (RoundingMode roundingMode : RoundingMode.values()) {
            try {
                System.out.println(roundingMode.name() + ": " + bigDecimal1.divide(bigDecimal2, 2, roundingMode));
            } catch (ArithmeticException e) {
                e.printStackTrace();
            }
        }

        try {
            Field intVal = BigDecimal.class.getDeclaredField("intVal");
            Field scale = BigDecimal.class.getDeclaredField("scale");
            Field intCompact = BigDecimal.class.getDeclaredField("intCompact");

            intVal.setAccessible(true);
            intCompact.setAccessible(true);
            scale.setAccessible(true);


            BigDecimal bigDecimal = new BigDecimal(Long.MAX_VALUE);

            System.out.printf("intVal: %s, intCompact: %s, scale: %s\n", intVal.get(bigDecimal), intCompact.get(bigDecimal), scale.get(bigDecimal));
            bigDecimal = new BigDecimal(Long.MAX_VALUE).add(new BigDecimal("0.99"));
            System.out.printf("intVal: %s, intCompact: %s, scale: %s\n", intVal.get(bigDecimal), intCompact.get(bigDecimal), scale.get(bigDecimal));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void bigIntegerExample() {
        BigInteger bigInteger1 = new BigInteger("-100");
        BigInteger bigInteger2 = new BigInteger("3");
        System.out.println(bigInteger1.add(bigInteger2));
        System.out.println(bigInteger1.subtract(bigInteger2));
        System.out.println(bigInteger1.multiply(bigInteger2));
        System.out.println(bigInteger1.divide(bigInteger2));
        //   -100 % 3  = -100 - (-100 / 3) * 3 = -1
        System.out.println(bigInteger1.remainder(bigInteger2));
        // -100 mod 3 = -100 % 3 >= 0 ? -100 % 3 : -100 % 3 + 3
        System.out.println(bigInteger1.mod(bigInteger2));


        try {
            Field mag = BigInteger.class.getDeclaredField("mag");
            mag.setAccessible(true);
            BigInteger pow = new BigInteger("0").subtract(new BigInteger("2").pow(33));
            System.out.println(pow.toString(2));
            int[] arr = (int[]) mag.get(pow);
            System.out.println(Arrays.toString(arr));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
