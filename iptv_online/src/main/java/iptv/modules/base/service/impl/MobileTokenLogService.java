package iptv.modules.base.service.impl;

import iptv.modules.base.entity.db.MobileTokenLog;
import iptv.modules.base.mapper.MobileTokenLogMapper;
import iptv.util.SysConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.util.Date;

@Service("mobileTokenLogService")
public class MobileTokenLogService {
	private static Logger log = LoggerFactory.getLogger(MobileTokenLogService.class);

	@Autowired
	private SysConfig sysConfig;

	@Autowired
	private MobileTokenLogMapper mobileTokenLogMapper;

	public void saveMobileTokenLog(String source,String token,String msg) {
		
		//获取token失效时间配置
		String TX_ACCESS_TOKEN_EXPIRETIME = sysConfig.getTX_ACCESS_TOKEN_EXPIRETIME();
		if (StringUtils.isBlank(TX_ACCESS_TOKEN_EXPIRETIME)) {
			//默认失效时长为1个半小时，单位为秒
	          TX_ACCESS_TOKEN_EXPIRETIME = "5400";
        }				
         //设置本地token
         long time = Long.valueOf(TX_ACCESS_TOKEN_EXPIRETIME) * 1000;
         Date expiretime = new Date(new Date().getTime() + time);
		// 获取本机ip和端口
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			MobileTokenLog mobileTokenLog = new MobileTokenLog();
			mobileTokenLog.setToken(token);
			mobileTokenLog.setIp(addr.getHostAddress());
			mobileTokenLog.setCreatetime(new Date());
			mobileTokenLog.setExpiretime(expiretime);
			mobileTokenLog.setMsg(msg);
			mobileTokenLog.setSource(source);
			mobileTokenLogMapper.insert(mobileTokenLog);
		} catch (Exception e) {
			log.error("【saveMobileTokenLog】记录token日志出错："+e.getCause(),e);
		}
	}

}
