package iptv.modules.base.service.impl;

import com.alibaba.fastjson.JSONObject;
import iptv.util.IncidentPushHttpUtils;
import iptv.util.SysConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("incidentPushServcie")
public class IncidentPushServcie {
	
	private static Logger log = LoggerFactory.getLogger(IncidentPushServcie.class);

	@Autowired
	private SysConfig sysConfig;
	/**
	 * 推送告警信息接口
	 * @param incidentdesc 告警信息
	 * @param lvlcode      严重级别
	 * @param incidentcategory 告警级别
	 */
	public  void incidentPush(String incidentdesc,String lvlcode,String incidentcategory){
		String localIp=sysConfig.getLocal_Ip();
//		try {
//			InetAddress addr = InetAddress.getLocalHost();
//			if(null!=addr){
//				localIp=addr.getHostAddress();
//			}
//		} catch (UnknownHostException e) {
//			log.error("【incidentPush】获取本机服务ip端口出错："+e.getCause(),e);
//		}
		
		JSONObject req=new JSONObject();
		req.put("appid", sysConfig.getIncidentPush_Appid());
		req.put("appkey", sysConfig.getIncidentPush_Appkey());
		req.put("incidentcode", sysConfig.getIncidentPush_Incidentcode());
		req.put("incidentdesc", "服务器("+localIp+") "+incidentdesc);
		req.put("lvlcode", lvlcode);
		req.put("incidentcategory", incidentcategory);
		req.put("ipv4addr", localIp);
		
		//推送告警信息
		try {
			IncidentPushHttpUtils.doPost(sysConfig.getIncidentPush_Url(), req);
		} catch (Exception e) {
			log.error("推动告警信息出错【"+JSONObject.toJSONString(req)+"】："+e.getCause(),e);
		}	
	}
	

}
