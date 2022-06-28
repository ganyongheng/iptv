package iptv.modules.base.service.impl;


import iptv.common.enums.DataSourceType;
import iptv.config.datasource.DataSource;
import iptv.modules.base.entity.db.SnmTokenLog;
import iptv.modules.base.mapper.SnmTokenLogMapper;
import iptv.util.SysConfig;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Date;

@Service("snmTokenLogService")
/*@DataSource(value = DataSourceType.IPTV_ONLINE)*/
public class SnmTokenLogService {
	@Autowired
	private SysConfig sysConfig;
	private static Logger log = LoggerFactory.getLogger(SnmTokenLogService.class);

	@Autowired
	private SnmTokenLogMapper snmTokenLogMapper;

	public void saveSnmTokenLog(String token,String msg) {

		// 获取token失效时间配置
		String TX_ACCESS_TOKEN_EXPIRETIME = sysConfig.getTX_ACCESS_TOKEN_EXPIRETIME();
		if (StringUtils.isBlank(TX_ACCESS_TOKEN_EXPIRETIME)) {
			// 默认失效时长为1个半小时，单位为秒
			TX_ACCESS_TOKEN_EXPIRETIME = "5400";
		}
		// 设置本地token
		long time = Long.valueOf(TX_ACCESS_TOKEN_EXPIRETIME) * 1000;
		Date expiretime = new Date(new Date().getTime() + time);
		// 获取本机ip和端口
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			SnmTokenLog snmTokenLog = new SnmTokenLog();
			snmTokenLog.setToken(token);
			snmTokenLog.setIp(addr.getHostAddress());
			snmTokenLog.setCreatetime(new Date());
			snmTokenLog.setExpiretime(expiretime);
			snmTokenLog.setMsg(msg);
			snmTokenLogMapper.insert(snmTokenLog);
		} catch (Exception e) {
			log.error("【saveSnmTokenLog】记录token日志出错：" + e.getCause(), e);
		}
	}

}