package iptv.config.log;

import java.util.Date;

public class InterLog {
	private static final long serialVersionUID = -7186799593088159390L;
	
	private Date date;
	private String totaltime;
	private String method;
	private String reqIp;
	private String reqJson;
	private String respJson;
	
	
	

	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getTotaltime() {
		return totaltime;
	}
	public void setTotaltime(String totaltime) {
		this.totaltime = totaltime;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getReqIp() {
		return reqIp;
	}
	public void setReqIp(String reqIp) {
		this.reqIp = reqIp;
	}
	public String getReqJson() {
		return reqJson;
	}
	public void setReqJson(String reqJson) {
		this.reqJson = reqJson;
	}
	public String getRespJson() {
		return respJson;
	}
	public void setRespJson(String respJson) {
		this.respJson = respJson;
	}
	
	
	
	


}
