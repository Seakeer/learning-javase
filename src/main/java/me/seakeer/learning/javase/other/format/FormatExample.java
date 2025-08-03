package me.seakeer.learning.javase.other.format;

import java.text.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

/**
 * FormatExample;
 *
 * @author Seakeer;
 * @date 2024/11/5;
 */
public class FormatExample {

    public static void main(String[] args) {

        System.out.println("----------------NumberFormat------------------");
        numberFormatExample(Locale.CHINA);

        System.out.println("----------------DecimalFormat------------------");
        decimalFormatExample(Locale.CHINA);

        System.out.println("----------------ChoiceFormat------------------");
        choiceFormatExample();

        System.out.println("----------------DateTimeFormat------------------");
        dateTimeFormatExample(Locale.CHINA);

        System.out.println("----------------MessageFormat------------------");
        messageFormatExample(Locale.CHINA);

        System.out.println("----------------Formatter------------------");
        formatterExample(Locale.CHINA);

    }

    private static void numberFormatExample(Locale locale) {
        // 普通数字格式化
        NumberFormat instance = NumberFormat.getInstance(locale);
        System.out.println("[NumberFormat][getInstance]: " + instance.format(1234567.89));

        // 普通数字格式化
        NumberFormat numberInstance = NumberFormat.getNumberInstance(locale);
        System.out.println("[NumberFormat][getNumberInstance]: " + numberInstance.format(1234567.89));

        // 百分比数字格式化
        NumberFormat percentInstance = NumberFormat.getPercentInstance(locale);
        System.out.println("[NumberFormat][getPercentInstance]: " + percentInstance.format(0.5));

        // 货币数字格式化
        NumberFormat currencyInstance = NumberFormat.getCurrencyInstance(locale);
        System.out.println("[NumberFormat][getCurrencyInstance]: " + currencyInstance.format(1234567.89));

        // 整数数字格式化
        NumberFormat integerInstance = NumberFormat.getIntegerInstance(locale);
        System.out.println("[NumberFormat][getIntegerInstance]: " + integerInstance.format(123456789));
    }

    private static void decimalFormatExample(Locale locale) {
        try {
            DecimalFormat decimalFormat = new DecimalFormat("#,###.00", DecimalFormatSymbols.getInstance(locale));
            System.out.printf("[DecimalFormat][FormatResult: %s]\n", decimalFormat.format(1234567.8));
            System.out.printf("[DecimalFormat][ParseResult: %s]\n", decimalFormat.parse("1234567.80"));

            // 科学计数法: 1.235E06
            DecimalFormat sciFormat = new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(locale));
            System.out.printf("[DecimalFormat][SciFormatResult: %s]\n", sciFormat.format(1234567.89));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void choiceFormatExample() {
        double[] limits = {10, 20, 30};
        String[] formats = {"S=[-∞, 20)", "S=[20, 30)", "S=[30, +∞)"};
        ChoiceFormat choiceFormat = new ChoiceFormat(limits, formats);
        for (int i = 0; i < 5; i++) {
            System.out.printf("[ChoiceFormat][choice: %s, result: %s]\n", i * 10, choiceFormat.format(i * 10));
        }
        // 效果与上面的构造方法一样
        ChoiceFormat patternChoiceFormat = new ChoiceFormat("10#[-∞, 20) | 20#[10, 20) | 30#[30, +∞)");
        for (int i = 0; i < 5; i++) {
            System.out.printf("[ChoiceFormatWithPattern][choice: %s, result: %s]\n", i * 10, patternChoiceFormat.format(i * 10));
        }

        // ChoiceFormat.nextDouble(30) 表示比30大的最小一个数
        double[] limitsNew = {10, 20, ChoiceFormat.nextDouble(30)};
        String[] formatsNew = {"S=[-∞, 20)", "S=[20, 30]", "S=(30, +∞)"};
        ChoiceFormat choiceFormatNew = new ChoiceFormat(limitsNew, formatsNew);
        for (int i = 0; i < 5; i++) {
            System.out.printf("[ChoiceFormatNextDouble][choice: %s, result: %s]\n", i * 10, choiceFormatNew.format(i * 10));
        }
    }

    private static void dateTimeFormatExample(Locale locale) {
        DateFormat dateTimeInstance = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG, locale);
        System.out.println("[DateFormat][getDateTimeInstance]: " + dateTimeInstance.format(new Date()));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale);
        System.out.println("[SimpleDateFormat]: " + simpleDateFormat.format(new Date()));

        // DateFormat存在多线程安全问题，一般使用DateTimeFormatter进行日期时间格式化
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", locale);
        System.out.println("[DateTimeFormatter]: " + dateTimeFormatter.format(LocalDateTime.now(ZoneOffset.ofHours(9))));
    }

    private static void messageFormatExample(Locale locale) {
        System.out.println(MessageFormat.format("Hello, This is {0} from {1}!", "Seakeer", "China"));

        MessageFormat messageFormat = new MessageFormat("[{0,number,##,###.00}] [{1,choice,100#X|1000#Y}] " +
                "[{2,date,yyyy-MM-dd}] [{3,time,HH:mm:ss}]",
                locale);
        System.out.println(messageFormat.format(new Object[]{1234567.89, 1000, new Date(), new Date()}));
    }

    private static void formatterExample(Locale locale) {
        String s = new Formatter(locale)
                .format("%s, %d, %.2f %n %tF %tT", "String", 1234567, 1.2, LocalDateTime.now(), LocalDateTime.now())
                .toString();
        System.out.println(s);
        System.out.println("---------------------");

        System.out.println(String.format(locale, "%s, %d, %.2f %n %tF %tT", "String", 1234567, 1.2, LocalDateTime.now(), LocalDateTime.now()));
        System.out.println("---------------------");

        System.out.printf(locale, "%s, %d, %.2f %n %tF %tT", "String", 1234567, 1.2, LocalDateTime.now(), LocalDateTime.now());
    }

}
