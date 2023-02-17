package com.cmcc.newcalllib.tool;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author jihongfei
 * Utils for date/time format.
 */
public class DateUtils {

    public static final String FMT_DATE = "yyyy-MM-dd";
    public static final String FMT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final String FMT_TIME = "HH:mm:ss";
    public static final long MILLISECONDS_PER_DAY = 86400000L;
    public static final String[] WEEK_NAMES = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    /**
     * get formatted date string in 'yyyy-MM-dd' of current time.
     */
    public static String getCurDate() {
        return formatDate(new Date(), DateUtils.FMT_DATE);
    }

    /**
     * get formatted date string in 'HH:mm:ss' of current time.
     */
    public static String getCurTime() {
        return formatDate(new Date(), DateUtils.FMT_TIME);
    }


    /**
     * get formatted date string in 'yyyy-MM-dd HH:mm:ss' of current time.
     */
    public static String getCurDateTime() {
        return formatDate(new Date(), DateUtils.FMT_DATE_TIME);
    }

    /**
     * get formatted date string in given format of current time.
     */
    public static String getCurDate(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.CHINESE);
        return sdf.format(new Date());
    }

    /**
     * get formatted date string in given format.
     */
    public static String formatDate(Date date, String pattern) {
        String formatDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.CHINESE);
        formatDate = sdf.format(date);
        return formatDate;
    }

    /**
     * parse date string by given format.
     */
    public static Date parseDate(String fmtDate, String fmt) {
        if (fmtDate == null || fmt == null) {
            return null;
        }
        try {
            return new SimpleDateFormat(fmt, Locale.CHINESE).parse(fmtDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get date after specific days.
     */
    public static Date nextDay(int num) {
        Calendar curr = Calendar.getInstance();
        curr.set(Calendar.DAY_OF_MONTH, curr.get(Calendar.DAY_OF_MONTH) + num);
        return curr.getTime();
    }


    /**
     * get date string after specific days.
     */
    public static String getSpecifiedDayAfter(Date specifiedDate, int num) {
        Calendar c = Calendar.getInstance();
        c.setTime(specifiedDate);
        int day = c.get(Calendar.DATE);
        c.set(Calendar.DATE, day + num);
        return formatDate(c.getTime(), DateUtils.FMT_DATE);
    }


    /**
     * get days between start date and end date.
     */
    public static long getDaysBetween(Date start, Date end) {
        String format = DateUtils.FMT_DATE;
        start = DateUtils.parseDate(DateUtils.formatDate(start, format), format);
        end = DateUtils.parseDate(DateUtils.formatDate(end, format), format);

        long diff = 0;
        if (start != null && end != null) {
            diff = (end.getTime() - start.getTime()) / DateUtils.MILLISECONDS_PER_DAY;
        }
        return diff;
    }

    /**
     * get week of given date.
     */
    public static String getWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int num = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        return WEEK_NAMES[num];
    }

    /**
     * test cases
     */
    public static void main(String[] args) {
        System.out.println(getCurTime());
        System.out.println(getCurDateTime());
        System.out.println(getCurDate());
        System.out.println(getCurDate("yyyy-MM-dd HH"));
        System.out.println(formatDate(new Date(), "yyyy-MM-dd HH:mm"));
        System.out.println(parseDate("2022-04-25", FMT_DATE));
        System.out.println(nextDay(1));
        System.out.println(getSpecifiedDayAfter(new Date(), 2));
        System.out.println(getDaysBetween(new Date(), nextDay(2)));
        System.out.println(getWeek(new Date()));
    }
}