package iptv.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;


public class PropertiesUtil {
	
	private static PropertiesUtil propertiesUtil;
	private Map<Object, Object> config;
	private Log log = LogFactory.getLog(this.getClass());
	public Properties getProperties(String path) {
		try {
			Properties p = new Properties();
			FileInputStream input = new FileInputStream(path);  
			p.load(input);
			input.close();
			return p;
		} catch (IOException e) {
			log.debug(e);
		}
		return null;
	}
	
	private void init(String pName){
		config = getProperties(pName);
	}
	public Map<Object, Object> getConfig(){
		return config;
	}
	protected PropertiesUtil(String pName){
		init(pName);
	}
	public static PropertiesUtil getPropertiesUtil(String pName){
		if(propertiesUtil == null)
			propertiesUtil = new PropertiesUtil(pName);
		return propertiesUtil;
	}
	/**
	 * 获取配置文件属性值
	 * @param key
	 * @return
	 */
	public Object getValue(String key){
		return config.get(key.trim());
	}
	
	/**
	 * 获取配置文件属性值 ,获取不到返回默认值
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getProperty(String key, String defaultValue){
	     Object value = config.get(key.trim());
	        if(null==value||StringUtils.isBlank(value.toString())){
	            value = defaultValue;
	        }
	        return value.toString();
	}
	
	/**
	 * 获取配置文件属性值 ,获取不到返回空白
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getProperty(String key){
	     Object value = config.get(key.trim());
	        if(null==value||StringUtils.isBlank(value.toString())){
	            value = "";
	        }
	        return value.toString();
	}
	

}
