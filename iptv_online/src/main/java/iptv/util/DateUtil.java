package iptv.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static String YYYY_MM_DD_HH_MM_SS ="yyyy-MM-dd HH:mm:ss";
    public static String YYYY_MM_DD_HH_MM ="yyyy-MM-dd HH:mm";
    public static String YYYY_MM_DD ="yyyy-MM-dd";
    public static String HH_MM_SS ="HH:mm:ss";
    public static String YYYYMMDD ="yyyyMMdd";
    public static String YYYYMMDDHH ="yyyyMMddHH";
    public static String YYYYMMDDHHMM ="yyyyMMddHHmm";

    /**
     * 将日期字符串转化为日期。失败返回null。
     * @param date 日期字符串
     * @param parttern 日期格式
     * @return 日期
     */
    public static Date StringToDate(String date, String parttern) {
        Date myDate = null;
        if (date != null) {
            try {
                myDate = getDateFormat(parttern).parse(date);
            } catch (Exception e) {
            }
        }
        return myDate;
    }

    /**
     * 获取SimpleDateFormat
     * @param parttern 日期格式
     * @return SimpleDateFormat对象
     * @throws RuntimeException 异常：非法日期格式
     */
    private static SimpleDateFormat getDateFormat(String parttern) throws RuntimeException {
        return new SimpleDateFormat(parttern);
    }


    /**
     * 比较两个日期 date1 是否小于 date2
     *
     * @throws Exception
     */
    public static boolean is_A_Before_B(Date date1, Date date2)
            throws Exception {
        return date1.before(date2) ? true : false;
    }

    /**
     * 比较两个日期是否相同
     *
     * @throws Exception
     */
    public static boolean compareDateTime(Date date1, Date date2)
            throws Exception {
        return (date1.before(date2) || date2.before(date1)) ? false : true;
    }

    /**
     * 计算两个日期之间相差多少月 精确到月
     *
     * @throws Exception
     * @author LiuYuan
     */
    public static double getDiscrepantMonthI(Date date1, Date date2)
            throws Exception {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(date1);
        c2.setTime(date2);
        int year1 = c1.get(Calendar.YEAR);
        int month1 = c1.get(Calendar.MONTH);
        int year2 = c2.get(Calendar.YEAR);
        int month2 = c2.get(Calendar.MONTH);
        double month = ((year1 - year2) * 12) + (month1 - month2);

        return month;
    }

    /**
     * 计算两个日期之间相差多少月 精确到日期
     *
     * （截止日期－开始日期 的最小月数) + (剩余天数/开始日期所属月的总天数)；
     *
     * @throws Exception
     * @author LiuYuan
     */
    public static double getDiscrepantMonth(Date date1, Date date2)
            throws Exception {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(date1);
        c2.setTime(date2);
        int year1 = c1.get(Calendar.YEAR);
        int month1 = c1.get(Calendar.MONTH);
        int day1 = c1.get(Calendar.DATE);
        int year2 = c2.get(Calendar.YEAR);
        int month2 = c2.get(Calendar.MONTH);
        int day2 = c2.get(Calendar.DATE);
        double month = ((year1 - year2) * 12) + (month1 - month2)
                + (day1 - day2)
                / (double) c2.getActualMaximum(Calendar.DAY_OF_MONTH);
        return month;
    }


    /**
     * 计算日期月份内有多少天
     *
     * @throws Exception
     * @author LiuYuan
     */
    public static int getDateDays(Date date) throws Exception {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * 将日期转化为日期字符串。失败返回null。
     * @param date 日期
     * @param parttern 日期格式
     * @return 日期字符串
     */
    public static String DateToString(Date date, String parttern) {
        String dateString = null;
        if (date != null) {
            try {
                dateString = getDateFormat(parttern).format(date);
            } catch (Exception e) {
            }
        }
        return dateString;
    }

    /**
     * 增加日期的天数。失败返回null。
     * @param date 日期
     * @param dayAmount 增加数量。可为负数
     * @return 增加天数后的日期
     */
    public static Date addDay(Date date, int dayAmount) {
        return addInteger(date, Calendar.DATE, dayAmount);
    }

    /**
     * 获取一天中的最后一秒
     * @param current 毫秒的时间戳
     * @return
     */
    public static long getEndOfDate(Long current){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(new Date(current));
        dateStr = dateStr.subSequence(0, 10)+" 23:59:59";
        try {
            return sdf.parse(dateStr).getTime()/1000;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return current/1000;
        }
    }


    /**
     * 增加日期的月份。失败返回null。
     * @param date 日期
     * @param yearAmount 增加数量。可为负数
     * @return 增加月份后的日期
     */
    public static Date addMonth(Date date, int yearAmount) {
        return addInteger(date, Calendar.MONTH, yearAmount);
    }

    /**
     * 增加日期中某类型的某数值。如增加日期
     * @param date 日期
     * @param dateType 类型
     * @param amount 数值
     * @return 计算后日期
     */
    private static Date addInteger(Date date, int dateType, int amount) {
        Date myDate = null;
        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(dateType, amount);
            myDate = calendar.getTime();
        }
        return myDate;
    }

}
