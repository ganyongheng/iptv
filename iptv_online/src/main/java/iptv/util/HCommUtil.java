package iptv.util;

import org.apache.commons.lang3.StringUtils;


import java.text.SimpleDateFormat;
import java.util.*;

public class HCommUtil {

	public static String YYYY_MM_DD_HH_MM_SS ="yyyy-MM-dd HH:mm:ss";
	public static String YYYY_MM_DD_HH_MM ="yyyy-MM-dd HH:mm";
	public static String YYYY_MM_DD ="yyyy-MM-dd";
	public static String HH_MM_SS ="HH:mm:ss";
	public static String YYYYMMDD ="yyyyMMdd";

	/**
	 * 把str的list 转为 string[] 的list,
	 * 在 excel,csv等转化时有用
	 * @param strlist
	 * @return [["111"],["1112"]]
	 */
	public static List<String[]> convertStrList(List<String> strlist){
		List<String[]> targetList = new ArrayList();
		for (String str : strlist) {
			String[] tmpArr = new String[1];
			tmpArr[0]= str;
			targetList.add(tmpArr);
		}
		return targetList;
	}
	
	/**
	 * ("000001","000124")
	 * @param beginStr
	 * @param endStr
	 * @return [000001, 000002, 000003, 000004, 000005, 000006........]
	 */
	public static List<String> genArrRange(String beginStr,String endStr){
		int begini = Integer.valueOf(beginStr);
		int endi = Integer.valueOf(endStr);
		List<String> targetList = new ArrayList<String>();
		int tmpi = begini;
		while(tmpi<=endi){
			String tmpstr = complePrefixStr(tmpi,6);
			targetList.add(tmpstr);
			++tmpi;
		}
		return targetList;
	}
	
	/**
	 * ip是否在range中
	 * @param targetIp
	 * @param ipbeginLong
	 * @param ipendLong
	 * @return
	 */
	public static boolean ipInRangeLong(String targetIp,long ipbeginLong,long ipendLong){
		long targetIpLong = HCommUtil.ipStrToLong(targetIp);
		if((ipbeginLong<targetIpLong)&&(targetIpLong<ipendLong)){
			return true;
		}
		return false;
	}
	

	/**
	 * ip是否在range中
	 * @param targetIp
	 * @param ipbegin
	 * @param ipend
	 * @return
	 */
	public static boolean ipInRange(String targetIp,String ipbegin,String ipend){
		long targetIpLong = HCommUtil.ipStrToLong(targetIp);
		long ipbeginLong = HCommUtil.ipStrToLong(ipbegin);
		long ipendLong = HCommUtil.ipStrToLong(ipend);
		return ipInRangeLong(targetIp, ipbeginLong, ipendLong);
	}
	

	public static Long ipStrToLong(String ipaddress) {
		long[] ip = new long[4];
		int i = 0;
		for (String ipStr : ipaddress.split("\\.")) {
			ip[i] = Long.parseLong(ipStr);
			i++;
		}
		return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
	}
	

	/**
	 * 判断是不是数组
	 * @param obj
	 * @return
	 */
    public static boolean isArrayList(Object obj) {
        return obj != null && obj.getClass().isArray() || (obj instanceof Collection) || (obj instanceof ArrayList);
    }
	
	/**
	 * 
	 * mac地址的转换
	 * @param macStr macStr 00:00:00:00:00:00
	 * @return
	 */
	public static String convertMac(String macStr){
		if(StringUtils.isEmpty(macStr))return "";
		String[] macArr =  macStr.split(":");
		String targetMacStr = "";
		for (int i = 0; i < macArr.length; i++) {
			targetMacStr+=macArr[i];
		}
		return targetMacStr;
	}
	

    
    //随机产生N位字母
	public static String getRandomString(int length) { //length表示生成字符串的长度
		String base = "0123456789qwertyuiopasdfghjklzxcvbnm";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {   
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}
	
	//随机产生N位数字随机数
	public static String getRandomNum(int length) { //length表示生成字符串的长度
		String base = "0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {   
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}
	
	

	/**
	 * 判断对象或对象数组中每一个对象非空: 对象为null，字符序列长度为0，集合类、Map为empty
	 * @param obj
	 * @return
	 */
	public static boolean isObjNotBlank(Object obj) {
		Boolean rtn = ! HCommUtil.isObjBlank(obj);
		return rtn;
	}

	
	/**
	 * 判断对象或对象数组中每一个对象是否为空: 对象为null，字符序列长度为0，集合类、Map为empty
	 * @param obj
	 * @return
	 */
	public static boolean isObjBlank(Object obj) {
		if (obj == null){
			return true;
		}
		if(obj instanceof String){
			return StringUtils.isBlank((String)obj);
		}
		if (obj instanceof CharSequence){
			return ((CharSequence) obj).length() == 0;
		}
		if (obj instanceof CharSequence){
			return ((CharSequence) obj).length() == 0;
		}
		if (obj instanceof Collection){
			return ((Collection) obj).isEmpty();
		}

		if (obj instanceof Map){
			return ((Map) obj).isEmpty();
		}

		if (obj instanceof Object[]) {
			Object[] object = (Object[]) obj;
			if (object.length == 0) {
				return true;
			}
			boolean empty = true;
			for (int i = 0; i < object.length; i++) {
				if (!HCommUtil.isObjBlank(object[i])) {
					empty = false;
					break;
				}
			}
			return empty;
		}
		return false;
	}
	

	
    private static int countNum = 10000000;
    public synchronized static String getNewTimeId(String prifixSymbol, String splitSymbol){
        if(StringUtils.isBlank(prifixSymbol)){
            prifixSymbol = "";
        }
        if(StringUtils.isBlank(splitSymbol)){
            splitSymbol = "";
        }
//        String str =(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date());
        String str =(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
        String  timeId = prifixSymbol + str.replaceAll("-","").replaceAll(":","").replaceAll(" ","") +splitSymbol+ (countNum++);
        if(countNum >= 90000000){
            countNum = 10000000;
        }
        return timeId;
    }
    
    /**
     * 根据时间获取ID
     * @return
     */
    public synchronized static String getNewTimeId(String splitSymbol){
        if(StringUtils.isBlank(splitSymbol)){
            splitSymbol = "";
        }
        String str =(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date());
        String  timeId = str.replaceAll("-","").replaceAll(":","").replaceAll(" ","") +"-"+ HCommUtil.getRandomNum(4) +splitSymbol+ (countNum++);
        if(countNum >= 90000000){
            countNum = 10000000;
        }
        return timeId;
    }
    
    /**
     * 根据时间获取ID 20位ID
     * @return
     */
    public  static String getNewTimeId(){
    	String timeId = getNewTimeId(null, null);
        return timeId;
    }
    
    
	
	
	/**
	 * "2016-01-01","2016-01-03"
	 * @param beginDatestr 开始日期
	 * @param endDatestr 不包含包含
	 * @return  [2016-01-01, 2016-01-02]

	 * @throws Exception 
	 */
	public static List<String> genDateRange(String beginDatestr,String endDatestr) throws Exception{
		
		Date beginDate = HCommUtil.StringToDate(beginDatestr, HCommUtil.YYYY_MM_DD);
		Date endDate = HCommUtil.StringToDate(endDatestr, HCommUtil.YYYY_MM_DD);
		if(beginDate.after(endDate)){
			throw new Exception("开始时间不能比结束时间后");
		}
		List<String> datestrList = new ArrayList();
		while(true){
			String tmpbeginstr = HCommUtil.DateToString(beginDate, HCommUtil.YYYY_MM_DD);
			if(tmpbeginstr.equals(endDatestr)){
				break;
			}
			beginDate = HCommUtil.addDay(beginDate, 1);
			datestrList.add(tmpbeginstr);
		}
		return datestrList;
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
	 * 获取日期中的某数值。如获取月份
	 * @param date 日期
	 * @param dateType 日期格式
	 * @return 数值
	 */
	private static int getInteger(Date date, int dateType) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(dateType);
	}
	
	/**
	 * 增加日期中某类型的某数值。如增加日期
	 * @param date 日期字符串
	 * @param dateType 类型
	 * @param amount 数值
	 * @return 计算后日期字符串
	 */
	private static String addInteger(String date, int dateType, int amount) {
		String dateString = null;
		DateStyle dateStyle = getDateStyle(date);
		if (dateStyle != null) {
			Date myDate = StringToDate(date, dateStyle);
			myDate = addInteger(myDate, dateType, amount);
			dateString = DateToString(myDate, dateStyle);
		}
		return dateString;
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

	/**
	 * 获取精确的日期
	 * @param timestamps 时间long集合
	 * @return 日期
	 */
	private static Date getAccurateDate(List<Long> timestamps) {
		Date date = null;
		long timestamp = 0;
		Map<Long, long[]> map = new HashMap<Long, long[]>();
		List<Long> absoluteValues = new ArrayList<Long>();

		if (timestamps != null && timestamps.size() > 0) {
			if (timestamps.size() > 1) {
				for (int i = 0; i < timestamps.size(); i++) {
					for (int j = i + 1; j < timestamps.size(); j++) {
						long absoluteValue = Math.abs(timestamps.get(i) - timestamps.get(j));
						absoluteValues.add(absoluteValue);
						long[] timestampTmp = { timestamps.get(i), timestamps.get(j) };
						map.put(absoluteValue, timestampTmp);
					}
				}

				// 有可能有相等的情况。如2012-11和2012-11-01。时间戳是相等的
				long minAbsoluteValue = -1;
				if (!absoluteValues.isEmpty()) {
					// 如果timestamps的size为2，这是差值只有一个，因此要给默认值
					minAbsoluteValue = absoluteValues.get(0);
				}
				for (int i = 0; i < absoluteValues.size(); i++) {
					for (int j = i + 1; j < absoluteValues.size(); j++) {
						if (absoluteValues.get(i) > absoluteValues.get(j)) {
							minAbsoluteValue = absoluteValues.get(j);
						} else {
							minAbsoluteValue = absoluteValues.get(i);
						}
					}
				}

				if (minAbsoluteValue != -1) {
					long[] timestampsLastTmp = map.get(minAbsoluteValue);
					if (absoluteValues.size() > 1) {
						timestamp = Math.max(timestampsLastTmp[0], timestampsLastTmp[1]);
					} else if (absoluteValues.size() == 1) {
						// 当timestamps的size为2，需要与当前时间作为参照
						long dateOne = timestampsLastTmp[0];
						long dateTwo = timestampsLastTmp[1];
						if ((Math.abs(dateOne - dateTwo)) < 100000000000L) {
							timestamp = Math.max(timestampsLastTmp[0], timestampsLastTmp[1]);
						} else {
							long now = new Date().getTime();
							if (Math.abs(dateOne - now) <= Math.abs(dateTwo - now)) {
								timestamp = dateOne;
							} else {
								timestamp = dateTwo;
							}
						}
					}
				}
			} else {
				timestamp = timestamps.get(0);
			}
		}

		if (timestamp != 0) {
			date = new Date(timestamp);
		}
		return date;
	}

	/**
	 * 判断字符串是否为日期字符串
	 * @param date 日期字符串
	 * @return true or false
	 */
	public static boolean isDate(String date) {
		boolean isDate = false;
		if (date != null) {
			if (StringToDate(date) != null) {
				isDate = true;
			}
		}
		return isDate;
	}

	/**
	 * 获取日期字符串的日期风格。失敗返回null。
	 * @param date 日期字符串
	 * @return 日期风格
	 */
	public static DateStyle getDateStyle(String date) {
		DateStyle dateStyle = null;
		Map<Long, DateStyle> map = new HashMap<Long, DateStyle>();
		List<Long> timestamps = new ArrayList<Long>();
		for (DateStyle style : DateStyle.values()) {
			Date dateTmp = StringToDate(date, style.getValue());
			if (dateTmp != null) {
				timestamps.add(dateTmp.getTime());
				map.put(dateTmp.getTime(), style);
			}
		}
		dateStyle = map.get(getAccurateDate(timestamps).getTime());
		return dateStyle;
	}

	/**
	 * 将日期字符串转化为日期。失败返回null。
	 * @param date 日期字符串
	 * @return 日期
	 */
	public static Date StringToDate(String date) {
		DateStyle dateStyle = null;
		return StringToDate(date, dateStyle);
	}

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
	 * 将日期字符串转化为日期。失败返回null。
	 * @param date 日期字符串
	 * @param dateStyle 日期风格
	 * @return 日期
	 */
	public static Date StringToDate(String date, DateStyle dateStyle) {
		Date myDate = null;
		if (dateStyle == null) {
			List<Long> timestamps = new ArrayList<Long>();
			for (DateStyle style : DateStyle.values()) {
				Date dateTmp = StringToDate(date, style.getValue());
				if (dateTmp != null) {
					timestamps.add(dateTmp.getTime());
				}
			}
			myDate = getAccurateDate(timestamps);
		} else {
			myDate = StringToDate(date, dateStyle.getValue());
		}
		return myDate;
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
	 * 将日期转化为日期字符串。失败返回null。
	 * @param date 日期
	 * @param dateStyle 日期风格
	 * @return 日期字符串
	 */
	public static String DateToString(Date date, DateStyle dateStyle) {
		String dateString = null;
		if (dateStyle != null) {
			dateString = DateToString(date, dateStyle.getValue());
		}
		return dateString;
	}

	/**
	 * 将日期字符串转化为另一日期字符串。失败返回null。
	 * @param date 旧日期字符串
	 * @param parttern 新日期格式
	 * @return 新日期字符串
	 */
	public static String StringToString(String date, String parttern) {
		return StringToString(date, null, parttern);
	}

	/**
	 * 将日期字符串转化为另一日期字符串。失败返回null。
	 * @param date 旧日期字符串
	 * @param dateStyle 新日期风格
	 * @return 新日期字符串
	 */
	public static String StringToString(String date, DateStyle dateStyle) {
		return StringToString(date, null, dateStyle);
	}

	/**
	 * 将日期字符串转化为另一日期字符串。失败返回null。
	 * @param date 旧日期字符串
	 * @param olddParttern 旧日期格式
	 * @param newParttern 新日期格式
	 * @return 新日期字符串
	 */
	public static String StringToString(String date, String olddParttern, String newParttern) {
		String dateString = null;
		if (olddParttern == null) {
			DateStyle style = getDateStyle(date);
			if (style != null) {
				Date myDate = StringToDate(date, style.getValue());
				dateString = DateToString(myDate, newParttern);
			}
		} else {
			Date myDate = StringToDate(date, olddParttern);
			dateString = DateToString(myDate, newParttern);
		}
		return dateString;
	}

	/**
	 * 将日期字符串转化为另一日期字符串。失败返回null。
	 * @param date 旧日期字符串
	 * @param olddDteStyle 旧日期风格
	 * @param newDateStyle 新日期风格
	 * @return 新日期字符串
	 */
	public static String StringToString(String date, DateStyle olddDteStyle, DateStyle newDateStyle) {
		String dateString = null;
		if (olddDteStyle == null) {
			DateStyle style = getDateStyle(date);
			dateString = StringToString(date, style.getValue(), newDateStyle.getValue());
		} else {
			dateString = StringToString(date, olddDteStyle.getValue(), newDateStyle.getValue());
		}
		return dateString;
	}

	/**
	 * 增加日期的年份。失败返回null。
	 * @param date 日期
	 * @param yearAmount 增加数量。可为负数
	 * @return 增加年份后的日期字符串
	 */
	public static String addYear(String date, int yearAmount) {
		return addInteger(date, Calendar.YEAR, yearAmount);
	}
	
	/**
	 * 增加日期的年份。失败返回null。
	 * @param date 日期
	 * @param yearAmount 增加数量。可为负数
	 * @return 增加年份后的日期
	 */
	public static Date addYear(Date date, int yearAmount) {
		return addInteger(date, Calendar.YEAR, yearAmount);
	}
	
	/**
	 * 增加日期的月份。失败返回null。
	 * @param date 日期
	 * @param yearAmount 增加数量。可为负数
	 * @return 增加月份后的日期字符串
	 */
	public static String addMonth(String date, int yearAmount) {
		return addInteger(date, Calendar.MONTH, yearAmount);
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
	 * 增加日期的天数。失败返回null。
	 * @param date 日期字符串
	 * @param dayAmount 增加数量。可为负数
	 * @return 增加天数后的日期字符串
	 */
	public static String addDay(String date, int dayAmount) {
		return addInteger(date, Calendar.DATE, dayAmount);
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
	 * 增加日期的小时。失败返回null。
	 * @param date 日期字符串
	 * @param hourAmount 增加数量。可为负数
	 * @return 增加小时后的日期字符串
	 */
	public static String addHour(String date, int hourAmount) {
		return addInteger(date, Calendar.HOUR_OF_DAY, hourAmount);
	}

	/**
	 * 增加日期的小时。失败返回null。
	 * @param date 日期
	 * @param hourAmount 增加数量。可为负数
	 * @return 增加小时后的日期
	 */
	public static Date addHour(Date date, int hourAmount) {
		return addInteger(date, Calendar.HOUR_OF_DAY, hourAmount);
	}
	
	/**
	 * 增加日期的分钟。失败返回null。
	 * @param date 日期字符串
	 * @param hourAmount 增加数量。可为负数
	 * @return 增加分钟后的日期字符串
	 */
	public static String addMinute(String date, int hourAmount) {
		return addInteger(date, Calendar.MINUTE, hourAmount);
	}

	/**
	 * 增加日期的分钟。失败返回null。
	 * @param date 日期
	 * @param hourAmount 增加数量。可为负数
	 * @return 增加分钟后的日期
	 */
	public static Date addMinute(Date date, int hourAmount) {
		return addInteger(date, Calendar.MINUTE, hourAmount);
	}
	
	/**
	 * 增加日期的秒钟。失败返回null。
	 * @param date 日期字符串
	 * @param hourAmount 增加数量。可为负数
	 * @return 增加秒钟后的日期字符串
	 */
	public static String addSecond(String date, int hourAmount) {
		return addInteger(date, Calendar.SECOND, hourAmount);
	}

	/**
	 * 增加日期的秒钟。失败返回null。
	 * @param date 日期
	 * @param hourAmount 增加数量。可为负数
	 * @return 增加秒钟后的日期
	 */
	public static Date addSecond(Date date, int hourAmount) {
		return addInteger(date, Calendar.SECOND, hourAmount);
	}

	/**
	 * 获取日期的年份。失败返回0。
	 * @param date 日期字符串
	 * @return 年份
	 */
	public static int getYear(String date) {
		return getYear(StringToDate(date));
	}

	/**
	 * 获取日期的年份。失败返回0。
	 * @param date 日期
	 * @return 年份
	 */
	public static int getYear(Date date) {
		return getInteger(date, Calendar.YEAR);
	}

	/**
	 * 获取日期的月份。失败返回0。
	 * @param date 日期字符串
	 * @return 月份
	 */
	public static int getMonth(String date) {
		return getMonth(StringToDate(date));
	}

	/**
	 * 获取日期的月份。失败返回0。
	 * @param date 日期
	 * @return 月份
	 */
	public static int getMonth(Date date) {
		return getInteger(date, Calendar.MONTH);
	}

	/**
	 * 获取日期的天数。失败返回0。
	 * @param date 日期字符串
	 * @return 天
	 */
	public static int getDay(String date) {
		return getDay(StringToDate(date));
	}

	/**
	 * 获取日期的天数。失败返回0。
	 * @param date 日期
	 * @return 天
	 */
	public static int getDay(Date date) {
		return getInteger(date, Calendar.DATE);
	}
	
	/**
	 * 获取日期的小时。失败返回0。
	 * @param date 日期字符串
	 * @return 小时
	 */
	public static int getHour(String date) {
		return getHour(StringToDate(date));
	}

	/**
	 * 获取日期的小时。失败返回0。
	 * @param date 日期
	 * @return 小时
	 */
	public static int getHour(Date date) {
		return getInteger(date, Calendar.HOUR_OF_DAY);
	}
	
	/**
	 * 获取日期的分钟。失败返回0。
	 * @param date 日期字符串
	 * @return 分钟
	 */
	public static int getMinute(String date) {
		return getMinute(StringToDate(date));
	}

	/**
	 * 获取日期的分钟。失败返回0。
	 * @param date 日期
	 * @return 分钟
	 */
	public static int getMinute(Date date) {
		return getInteger(date, Calendar.MINUTE);
	}
	
	/**
	 * 获取日期的秒钟。失败返回0。
	 * @param date 日期字符串
	 * @return 秒钟
	 */
	public static int getSecond(String date) {
		return getSecond(StringToDate(date));
	}

	/**
	 * 获取日期的秒钟。失败返回0。
	 * @param date 日期
	 * @return 秒钟
	 */
	public static int getSecond(Date date) {
		return getInteger(date, Calendar.SECOND);
	}

	/**
	 * 获取日期 。默认yyyy-MM-dd格式。失败返回null。
	 * @param date 日期字符串
	 * @return 日期
	 */
	public static String getDate(String date) {
		return StringToString(date, DateStyle.YYYY_MM_DD);
	}

	/**
	 * 获取日期。默认yyyy-MM-dd格式。失败返回null。
	 * @param date 日期
	 * @return 日期
	 */
	public static String getDate(Date date) {
		return DateToString(date, DateStyle.YYYY_MM_DD);
	}

	/**
	 * 获取日期的时间。默认HH:mm:ss格式。失败返回null。
	 * @param date 日期字符串
	 * @return 时间
	 */
	public static String getTime(String date) {
		return StringToString(date, DateStyle.HH_MM_SS);
	}

	/**
	 * 获取日期的时间。默认HH:mm:ss格式。失败返回null。
	 * @param date 日期
	 * @return 时间
	 */
	public static String getTime(Date date) {
		return DateToString(date, DateStyle.HH_MM_SS);
	}

	/**
	 * 获取日期的星期。失败返回null。
	 * @param date 日期字符串
	 * @return 星期
	 */
	public static Week getWeek(String date) {
		Week week = null;
		DateStyle dateStyle = getDateStyle(date);
		if (dateStyle != null) {
			Date myDate = StringToDate(date, dateStyle);
			week = getWeek(myDate);
		}
		return week;
	}

	/**
	 * 获取日期的星期。失败返回null。
	 * @param date 日期
	 * @return 星期
	 */
	public static Week getWeek(Date date) {
		Week week = null;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int weekNumber = calendar.get(Calendar.DAY_OF_WEEK) - 1;
		switch (weekNumber) {
		case 0:
			week = Week.SUNDAY;
			break;
		case 1:
			week = Week.MONDAY;
			break;
		case 2:
			week = Week.TUESDAY;
			break;
		case 3:
			week = Week.WEDNESDAY;
			break;
		case 4:
			week = Week.THURSDAY;
			break;
		case 5:
			week = Week.FRIDAY;
			break;
		case 6:
			week = Week.SATURDAY;
			break;
		}
		return week;
	}
	
	/**
	 * 获取两个日期相差的天数
	 * @param date 日期字符串
	 * @param otherDate 另一个日期字符串
	 * @return 相差天数
	 */
	public static int getIntervalDays(String date, String otherDate) {
		return getIntervalDays(StringToDate(date), StringToDate(otherDate));
	}
	
	/**
	 * @param date 日期
	 * @param otherDate 另一个日期
	 * @return 相差天数
	 */
	public static int getIntervalDays(Date date, Date otherDate) {
		date = HCommUtil.StringToDate(HCommUtil.getDate(date));
		long time = Math.abs(date.getTime() - otherDate.getTime());
		return (int)time/(24 * 60 * 60 * 1000);
	}
	

	enum DateStyle {
		
		MM_DD("MM-dd"),
		YYYY_MM("yyyy-MM"),
		YYYY_MM_DD("yyyy-MM-dd"),
		MM_DD_HH_MM("MM-dd HH:mm"),
		MM_DD_HH_MM_SS("MM-dd HH:mm:ss"),
		YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm"),
		YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),
		
		MM_DD_EN("MM/dd"),
		YYYY_MM_EN("yyyy/MM"),
		YYYY_MM_DD_EN("yyyy/MM/dd"),
		MM_DD_HH_MM_EN("MM/dd HH:mm"),
		MM_DD_HH_MM_SS_EN("MM/dd HH:mm:ss"),
		YYYY_MM_DD_HH_MM_EN("yyyy/MM/dd HH:mm"),
		YYYY_MM_DD_HH_MM_SS_EN("yyyy/MM/dd HH:mm:ss"),
		
		MM_DD_CN("MM月dd日"),
		YYYY_MM_CN("yyyy年MM月"),
		YYYY_MM_DD_CN("yyyy年MM月dd日"),
		MM_DD_HH_MM_CN("MM月dd日 HH:mm"),
		MM_DD_HH_MM_SS_CN("MM月dd日 HH:mm:ss"),
		YYYY_MM_DD_HH_MM_CN("yyyy年MM月dd日 HH:mm"),
		YYYY_MM_DD_HH_MM_SS_CN("yyyy年MM月dd日 HH:mm:ss"),
		
		HH_MM("HH:mm"),
		HH_MM_SS("HH:mm:ss");
		
		
		private String value;
		
		DateStyle(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	enum Week {

		MONDAY("星期一", "Monday", "Mon.", 1),
		TUESDAY("星期二", "Tuesday", "Tues.", 2),
		WEDNESDAY("星期三", "Wednesday", "Wed.", 3),
		THURSDAY("星期四", "Thursday", "Thur.", 4),
		FRIDAY("星期五", "Friday", "Fri.", 5),
		SATURDAY("星期六", "Saturday", "Sat.", 6),
		SUNDAY("星期日", "Sunday", "Sun.", 7);
		
		String name_cn;
		String name_en;
		String name_enShort;
		int number;
		
		Week(String name_cn, String name_en, String name_enShort, int number) {
			this.name_cn = name_cn;
			this.name_en = name_en;
			this.name_enShort = name_enShort;
			this.number = number;
		}
		
		public String getChineseName() {
			return name_cn;
		}

		public String getName() {
			return name_en;
		}

		public String getShortName() {
			return name_enShort;
		}

		public int getNumber() {
			return number;
		}
	}
	
	public static String getRandomFileName(String fileName) {

		// String name = System.currentTimeMillis() +
		// UUID.randomUUID().toString()
		// String name = UUID.randomUUID().toString()
		// + getExtName(fileName);
		String name = HCommUtil.DateToString(new Date(), "yyyyMMddHHmmss") + "_" + fileName;
		return name;
	}
	
	
	
	
	
	/**
	 * 
	 * 前缀补0
	 * complePrefixStr(555,6)
	 * @param cInt
	 * @param range
	 * @return  000555
	 */
	public static String complePrefixStr(int cInt,int range){
		String targetStr = cInt+"";
		while(targetStr.length()<range){
			targetStr = "0"+targetStr;
		}
		return targetStr;
		
	}

	
	public static String getRandomCode(){
		StringBuffer stringBuffer = new StringBuffer();
        Random rd = new Random();
        while (stringBuffer.length() != 20) {
            int value = rd.nextInt(99999);
            String temp = Integer.toHexString(value);
            if (temp.length() == 5) {
                stringBuffer.append(temp);
                
            } 
        }
        String upperCase = stringBuffer.toString().toUpperCase();
        String uuid = UUID.randomUUID().toString(); 
		String userId=MD5Util.encode(uuid+upperCase);
		return userId;
	}
	
	
	public static String getRandomCodeByTime(){
		long currentTimeMillis = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString(); 
		String userId=MD5Util.encode(uuid+currentTimeMillis);
		return userId;
	}
	
	public static String getRandomCodeByTime(long generate){
		long currentTimeMillis = System.currentTimeMillis();
		String uuid = UUID.randomUUID().toString(); 
		String userId=MD5Util.encode(uuid+currentTimeMillis+generate);
		return userId;
	}
	
	public static void main(String[] args) {
		
	    for (int i=0;i<500000;i++) {
	    	String randomCodeByTime = getRandomCodeByTime();
	    	System.out.println(randomCodeByTime);
		}
	}
}