package iptv.util;

/**
 * <p>Company: maywide</p>
 * <p>Description: </p>
 *  系统常量，保存系统常量，该常量必须是在framework里面使用的，否则请添加到biz下
 */
public interface Constant {
	
	final String COMMA = ",";
	final String NULL_FIELDS = "nullFields";       //保存查询条件为空的字段
	final String NOTNULL_FIELDS = "notNullFields"; //保存查询条件不为空的字段
	
	final String ORDER_ASC = "ASC";          
	final String ORDER_DESC = "DESC";
	
	final int FLAG_NULL = 1;     //为空标志
	final int FLAG_NOTNULL = 2;  //不为空标志
	
	final Integer DEFAULT_SYSID = 1;
	
	interface TimeUnit {
		/**
		 * 小时 <code>10</code>
		 */
		public static final String HOUR = "10";
		/**
		 * 日 <code>0</code>
		 */
		public static final String DAY = "0"; // 日
		/**
		 * 周 <code>20</code>
		 */
		public static final String WEAK = "20"; // 周
		/**
		 * 月 <code>1</code>
		 */
		public static final String MONTH = "1"; // 月
		/**
		 * 年 <code>2</code>
		 */
		public static final String YEAR = "2";



	}
	
	
	  interface ServerResponseStatus{
	        //成功
	        int SUCCESS=0;
	        //失败
	        int FAIL=1;
	    }

	    interface YOUKU{
	        interface InterfaceName{
	            //节目详情页分页数据接口
	            String OttFireworksNodesDetail="youku.ott.fireworks.nodes.detail";

	            //获取桌面抽屉列表
	            String OttFireworksNodesPage="youku.ott.fireworks.nodes.page";
	        }
	    }

	    interface Model{
	        interface BYKShowSync{
	            //初始
	            String SyncStatus_Init="0";
	            //成功
	            String SyncStatus_SUCCESS="1";
	            //失败
	            String SyncStatus_FAIL="2";
	        }
	    }
	
	
	
	
	
}