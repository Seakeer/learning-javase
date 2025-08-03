package me.seakeer.learning.javase.other.i18nl10n;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * I18nExample;
 *
 * @author Seakeer;
 * @date 2024/8/12;
 */
public class I18nL10nExample {

    public static void main(String[] args) {

        System.out.println("--------------------Locale Example-----------------------");
        localeExample();

        System.out.println("--------------------文本翻译 ResourceBundle Example-----------------------");
        resourceBundleExample();

        System.out.println("--------------------日期时间 DateTime Example-----------------------");
        dateTimeExample();

        System.out.println("--------------------数字格式 NumberFormat Example-----------------------");
        numberFormatExample();

        System.out.println("--------------------货币 Currency Example-----------------------");
        currencyExample();

    }

    private static void localeExample() {
        System.out.println("Default Locale: " + Locale.getDefault().toString() + ", " + Locale.getDefault().getDisplayName());

        Arrays.stream(Locale.getAvailableLocales())
                .forEach(locale -> {
                    System.out.println(locale.toString() + ",  " + locale.getDisplayLanguage() + ",  " + locale.getDisplayCountry()
                            + ",  " + locale.getDisplayLanguage(Locale.CHINA) + ",  " + locale.getDisplayCountry(Locale.CHINA));
                });
    }

    private static void resourceBundleExample() {
        ResourceBundle bundleZhCn = ResourceBundle.getBundle("i18n", Locale.CHINA);
        String isoTitle = bundleZhCn.getString("title");
        // 采用的是ISO-8859-1编码，转为UTF-8，避免中文乱码
        String title = new String(isoTitle.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        System.out.println(title);

        ResourceBundle bundleEnUs = ResourceBundle.getBundle("i18n", Locale.US);
        String isoContent = bundleEnUs.getString("content");
        String content = new String(isoContent.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        System.out.println(content);
    }

    private static void dateTimeExample() {

        long nowMsec = DateTimeUtil.nowMsec();
        String nowStr = DateTimeUtil.nowStr(ZoneOffset.ofHours(+8), "yyyy-MM-dd HH:mm:ss");

        System.out.printf("nowMsec: %s; nowStr: %s\n", nowMsec, nowStr);

        long toMsec = DateTimeUtil.toMsec(nowStr, ZoneOffset.ofHours(+8), "yyyy-MM-dd HH:mm:ss");
        String fromMsec = DateTimeUtil.fromMsec(nowMsec, ZoneOffset.ofHours(+8), "yyyy-MM-dd HH:mm:ss");
        System.out.printf("toMsec: %s; fromMsec: %s\n", toMsec, fromMsec);


        String convert = DateTimeUtil.convert(nowStr, ZoneOffset.ofHours(+8), DateTimeUtil.DEFAULT_DATE_TIME_FORMATTER,
                ZoneOffset.ofHours(-8), DateTimeUtil.DEFAULT_DATE_TIME_FORMATTER);
        System.out.println("convert: " + nowStr + " ---> " + convert);

        //ZoneId.getAvailableZoneIds().forEach(System.out::println);
    }

    private static void numberFormatExample() {
        NumberFormat currencyInstance = NumberFormat.getCurrencyInstance();
        System.out.println(currencyInstance.format(100));
    }

    private static void currencyExample() {
        Currency cny = Currency.getInstance(Locale.CHINA);
        Currency usd = Currency.getInstance(Locale.US);
        Currency hkd = Currency.getInstance(new Locale("zh", "HK"));
        Arrays.asList(cny, usd, hkd).forEach(currency ->
                System.out.printf("货币代码: %s; 货币名称: %s - %s; 货币符号: %s - %s; 货币数字码: %s; 默认小数点位数: %s\n",
                        currency.getCurrencyCode(),
                        currency.getDisplayName(Locale.CHINA),
                        currency.getDisplayName(Locale.US),
                        currency.getSymbol(Locale.CHINA),
                        currency.getSymbol(Locale.US),
                        currency.getNumericCode(),
                        currency.getDefaultFractionDigits())
        );

//        Currency.getAvailableCurrencies().forEach(currency -> {
//            System.out.printf("Currency CurrencyCode: %s, DisplayName: %s - %s, Symbol: %s - %s, NumericCode: %s, DefaultFractionDigits: %s\n",
//                    currency.getCurrencyCode(),
//                    currency.getDisplayName(Locale.CHINA),
//                    currency.getDisplayName(Locale.US),
//                    currency.getSymbol(Locale.CHINA),
//                    currency.getSymbol(Locale.US),
//                    currency.getNumericCode(),
//                    currency.getDefaultFractionDigits()
//            );
//
//        });
    }
}
