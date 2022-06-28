package iptv.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

public class RandomCode {
	/**
	 * 生成32位随机数
	 * 
	 * @return string
	 */
	public static String getUUID() {
		return UUID.randomUUID().toString().trim().replaceAll("-", "");
	}
	
	/**
	 * 生产订单号方法
	 * @param prefix
	 * @return
	 */
	private String nextSn(String prefix,String userId){
		
		String hashCodeStr=userId.hashCode()+"";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String dateStr = sdf.format(new Date());
		
		Random r=new Random();
		int rand=r.nextInt(90) +10;
		
		String noStr=prefix+dateStr+rand;
		
		hashCodeStr=hashCodeStr.replace("-", "");
		int dif=28-noStr.length();
		int hashCodeLen=hashCodeStr.length();
		if(dif<hashCodeLen){
			
			hashCodeStr=hashCodeStr.substring(0, dif);
			
		}else if(dif>=hashCodeLen){
			
			int size=dif-hashCodeLen;
			StringBuilder sb=new StringBuilder();
			for(int i=0;i<size;i++){
				sb.append("0");
			}
			hashCodeStr=sb.toString()+hashCodeStr;
			
		}
		noStr=noStr+hashCodeStr;
		
		return noStr;
	}
}
