package iptv.util;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class HttpUtilsException {
	
	//方法实现留空，用来做aop切面异步执行告警
	public JSONObject httpUtilsIncidentPush(String incidentdesc, String lvlcode, String incidentcategory){
		JSONObject msg=new JSONObject();
		msg.put("incidentdesc", incidentdesc);
		msg.put("lvlcode", lvlcode);
		msg.put("incidentcategory", incidentcategory);
		return msg;		
	}
}
