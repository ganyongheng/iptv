package iptv.util;

public class ConstDef {

	// 状态 1-生效 0-无效
	public static int STATUS_VALID = 1;
	public static int STATUS_EXPIRE = 0;
	
	public static int READ_LOG_MAX_RECORD = 4000;
	public static int READ_LOG_MAX_THREAD = 3;
	
	public static int USER_STATUS_VALID = 1;
	public static int USER_STATUS_UNUSE =0;
	
	public static int FILE_READ_PROCESSING = 1;
	public static int FILE_READ_FINISHED =9;
	
	
	public static String USER_LOGIN_LOG_TOPIC = "USER_LOGIN_LOG_TOPIC";
	public static String USER_NEW_USER_TOPIC = "USER_NEW_USER_TOPIC";
	public static String KEY_REDIS = "KEY_REDIS_LIST_VALUE";
	// add by luyang 20220215
	//存放大于21亿的 vuid
	public static String KEY_REDIS_NEW = "KEY_REDIS_LIST_VALUE_NEW";
	// end by luyang 20220215
	public static String KEY_REDIS_STATUS = "KEY_REDIS_STATUS";
	public static String KEY_GET_VUID_STATUS_DELET = "2";
	public static String KEY_GET_VUID_STATUS_ADD = "1";
	
	
	public static int SYNC_STATUS_ON = 1;
	public static int SYNC_STATUS_OFF =0;
	


	
}
