package iptv.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;

/**
 * 系统名：电视购彩管理系统  
 * 子系统名：MD5工具类
 * 著作权：COPYRIGHT (C) 2017 SNM INFORMATION SYSTEMS
 * CORPORATION ALL RIGHTS RESERVED.
 * 
 * @author nianchun.li
 * @createTime 2017年9月19日 下午3:46:37
 */
public class MD5Util {
	
	public final static String encode(String s) {
        char hexDigits[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};       
        try {
            byte[] btInput = s.getBytes("utf-8");
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	
	  /**
	   * Utf8URL编码
	   * @param text
	   * @return
	   */
	  public static final String Utf8URLencode(String text) {
	    StringBuffer result = new StringBuffer();
	    for (int i = 0; i < text.length(); i++) {
	      char c = text.charAt(i);
	      if (c >= 0 && c <= 255) {
	        result.append(c);
	      }else {
	        byte[] b = new byte[0];
	        try {
	          b = Character.toString(c).getBytes("UTF-8");
	        }catch (Exception ex) {
	        }
	        for (int j = 0; j < b.length; j++) {
	          int k = b[j];
	          if (k < 0) k += 256;
	          result.append("%" + Integer.toHexString(k).toUpperCase());
	        }
	      }
	    }
	    return result.toString();
	  }


	/**
	 *
	 * @param jsonstr  请求参数json字符串
	 * @param secret_key 参与签名的key
	 * @return
	 * @throws Exception
	 */

	public static String md5Encode(String jsonstr,String secret_key) throws Exception{
		JSONObject json = JSON.parseObject(jsonstr);
		TreeMap<String, String> data = new TreeMap<String, String>();
		for(String key:json.keySet()){
			if("sign".equals(key)){
				//sign不参与签名
				continue;
			}
			data.put(key, json.getString(key));
		}
		List<String> params = new ArrayList<String>();
		// 重组参数
		for(String key:data.keySet()){
			String value = String.format("%s=%s", key, data.get(key));
			params.add(value);
		}
		// 组合参数和签名 secret_key
		String temp = Utf8URLencode(StringUtils.join(params, "&").toLowerCase() + "&key=" + secret_key);
		String result=MD5Util.encode(temp);
		return new String(result);

	}
	
	public static void main(String[]args){
		String a="source=snm_bilibili&user_login=&user_id=27515308&seq_no=87153287181213153333&price=2000&buy_num=1&pay_type=2&guid=tv3456789&login_name=杭仔萌萌哒&product_id=zc20181206&total=2000&video_type=fvod&pay_way=1&client_ip=127.0.0.1&key=snm112901*!lkkjWngso4%*&o+-(j242Ssdrfslj";
		
		String b=URLEncoder.encode(a);
		System.out.println(b);
		String sign=encode(b);
		System.out.println(sign);


		}
}


