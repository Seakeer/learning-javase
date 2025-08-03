package me.seakeer.learning.javase.other.i18nl10n;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * DateTimeUtil;
 *
 * @author Seakeer;
 * @date 2024/9/30;
 */
public class DateTimeUtil {

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String YYYY_MM_DD_HH_12_MM_SS = "yyyy-MM-dd hh:mm:ss";
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    public static final String YYYY_MM_DD_HH_12_MM = "yyyy-MM-dd hh:mm";
    public static final String YYYY_MM_DD = "yyyy-MM-dd";


    public static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);

    public static final Map<String, DateTimeFormatter> PATTERN_FORMATTER_MAP = new HashMap<String, DateTimeFormatter>() {
        {
            put(YYYY_MM_DD_HH_MM_SS, DEFAULT_DATE_TIME_FORMATTER);
            put(YYYY_MM_DD_HH_12_MM_SS, DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_12_MM_SS));
            put(YYYY_MM_DD_HH_MM, DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM));
            put(YYYY_MM_DD_HH_12_MM, DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_12_MM));
            put(YYYY_MM_DD, DateTimeFormatter.ofPattern(YYYY_MM_DD));
        }
    };

    public static DateTimeFormatter getFormatter(String pattern) {
        DateTimeFormatter dateTimeFormatter = PATTERN_FORMATTER_MAP.get(pattern);
        return null != dateTimeFormatter ? dateTimeFormatter : DateTimeFormatter.ofPattern(pattern);
    }

    /**
     * 获取当前时间戳 毫秒级别
     */
    public static long nowMsec() {
        return System.currentTimeMillis();
    }

    /**
     * 获取指定时区偏移量的时间字符串
     *
     * @param zoneOffset
     * @param dateTimeFormatter
     * @return
     */
    public static String nowStr(ZoneOffset zoneOffset, DateTimeFormatter dateTimeFormatter) {
        OffsetDateTime offsetDateTime = OffsetDateTime.now(zoneOffset);
        return offsetDateTime.format(dateTimeFormatter);
    }

    public static String nowStr(ZoneOffset zoneOffset, String pattern) {
        return nowStr(zoneOffset, getFormatter(pattern));
    }

    /**
     * 获取指定时区ID的时间字符串
     *
     * @param zoneId
     * @param dateTimeFormatter
     * @return
     */
    public static String nowStr(ZoneId zoneId, DateTimeFormatter dateTimeFormatter) {
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
        return zonedDateTime.format(dateTimeFormatter);
    }

    public static String nowStr(ZoneId zoneId, String pattern) {
        return nowStr(zoneId, getFormatter(pattern));
    }


    /**
     * 将指定时区偏移量的日期时间字符串转换为时间戳
     *
     * @param srcDteTimeStr
     * @param srcZoneOffset
     * @param srcFormatter
     * @return
     */
    public static long toMsec(String srcDteTimeStr, ZoneOffset srcZoneOffset, DateTimeFormatter srcFormatter) {
        LocalDateTime localDateTime = LocalDateTime.parse(srcDteTimeStr, srcFormatter);
        OffsetDateTime offsetDateTime = localDateTime.atOffset(srcZoneOffset);
        Instant instant = Instant.from(offsetDateTime);
        return instant.toEpochMilli();
    }

    public static long toMsec(String srcDteTimeStr, ZoneOffset srcZoneOffset, String srcPattern) {
        return toMsec(srcDteTimeStr, srcZoneOffset, getFormatter(srcPattern));
    }


    /**
     * 将指定时区ID的日期时间字符串转换为时间戳
     *
     * @param srcDteTimeStr
     * @param srcZoneId
     * @param srcFormatter
     * @return
     */
    public static long toMsec(String srcDteTimeStr, ZoneId srcZoneId, DateTimeFormatter srcFormatter) {
        LocalDateTime localDateTime = LocalDateTime.parse(srcDteTimeStr, srcFormatter);
        ZonedDateTime zonedDateTime = localDateTime.atZone(srcZoneId);
        Instant instant = Instant.from(zonedDateTime);
        return instant.toEpochMilli();
    }

    public static long toMsec(String srcDteTimeStr, ZoneId srcZoneId, String srcPattern) {
        return toMsec(srcDteTimeStr, srcZoneId, getFormatter(srcPattern));
    }


    /**
     * 将时间戳转换为指定时区偏移量的日期时间字符串
     *
     * @param msec
     * @param targetZoneOffset
     * @param targetFormatter
     * @return
     */
    public static String fromMsec(long msec, ZoneOffset targetZoneOffset, DateTimeFormatter targetFormatter) {
        return Instant.ofEpochMilli(msec)
                .atOffset(targetZoneOffset)
                .format(targetFormatter);
    }

    public static String fromMsec(long msec, ZoneOffset targetZoneOffset, String pattern) {
        return Instant.ofEpochMilli(msec)
                .atOffset(targetZoneOffset)
                .format(getFormatter(pattern));
    }

    /**
     * 将时间戳转换为指定时区ID的日期时间字符串
     *
     * @param msec
     * @param targetZoneId
     * @param targetFormatter
     * @return
     */
    public static String fromMsec(long msec, ZoneId targetZoneId, DateTimeFormatter targetFormatter) {
        return Instant.ofEpochMilli(msec)
                .atZone(targetZoneId)
                .format(targetFormatter);
    }

    public static String fromMsec(long msec, ZoneId targetZoneId, String targetPattern) {
        return Instant.ofEpochMilli(msec)
                .atZone(targetZoneId)
                .format(getFormatter(targetPattern));
    }

    /**
     * 将某个时区偏移量的日期时间字符串转换为另外一个时区偏移量的日期时间字符串
     *
     * @param srcDateTimeStr
     * @param srcZoneOffset
     * @param srcFormatter
     * @param targetZoneOffset
     * @param targetFormatter
     * @return
     */
    public static String convert(String srcDateTimeStr, ZoneOffset srcZoneOffset, DateTimeFormatter srcFormatter,
                                 ZoneOffset targetZoneOffset, DateTimeFormatter targetFormatter) {
        return convert(LocalDateTime.parse(srcDateTimeStr, srcFormatter).atOffset(srcZoneOffset), targetZoneOffset, targetFormatter);
    }

    public static String convert(LocalDateTime srcLocalDateTime, ZoneOffset srcZoneOffset,
                                 ZoneOffset targetZoneOffset, DateTimeFormatter targetFormatter) {
        return convert(srcLocalDateTime.atOffset(srcZoneOffset), targetZoneOffset, targetFormatter);
    }

    public static String convert(OffsetDateTime srcOffsetDateTime,
                                 ZoneOffset targetZoneOffset, DateTimeFormatter targetFormatter) {
        return srcOffsetDateTime.withOffsetSameInstant(targetZoneOffset).format(targetFormatter);
    }

    /**
     * 将某个时区的日期时间字符串转换为另外一个时区的日期时间字符串
     *
     * @param srcDateTimeStr
     * @param srcZoneId
     * @param srcFormatter
     * @param targetZoneId
     * @param targetFormatter
     * @return
     */
    public static String convert(String srcDateTimeStr, ZoneId srcZoneId, DateTimeFormatter srcFormatter,
                                 ZoneId targetZoneId, DateTimeFormatter targetFormatter) {
        return convert(LocalDateTime.parse(srcDateTimeStr, srcFormatter).atZone(srcZoneId), targetZoneId, targetFormatter);
    }

    public static String convert(LocalDateTime srcLocalDateTime, ZoneId srcZoneId,
                                 ZoneId targetZoneId, DateTimeFormatter targetFormatter) {
        return convert(srcLocalDateTime.atZone(srcZoneId), targetZoneId, targetFormatter);
    }

    public static String convert(ZonedDateTime srcZonedDateTime,
                                 ZoneId targetZoneId, DateTimeFormatter targetFormatter) {
        return srcZonedDateTime.withZoneSameInstant(targetZoneId).format(targetFormatter);
    }

    public static void main(String[] args) {
        long nowMsec = nowMsec();
        String nowStr = nowStr(ZoneOffset.ofHours(+8), "yyyy-MM-dd HH:mm:ss");

        System.out.printf("nowMsec: %s; nowStr: %s\n", nowMsec, nowStr);

        long toMsec = toMsec(nowStr, ZoneOffset.ofHours(+8), "yyyy-MM-dd HH:mm:ss");
        String fromMsec = fromMsec(nowMsec, ZoneOffset.ofHours(+8), "yyyy-MM-dd HH:mm:ss");
        System.out.printf("toMsec: %s; fromMsec: %s\n", toMsec, fromMsec);


        String convert = convert(nowStr, ZoneOffset.ofHours(+8), DEFAULT_DATE_TIME_FORMATTER, ZoneOffset.ofHours(-8), DEFAULT_DATE_TIME_FORMATTER);
        System.out.println("convert: " + nowStr + " ---> " + convert);

//        ZoneId.getAvailableZoneIds().forEach(System.out::println);
    }
}
