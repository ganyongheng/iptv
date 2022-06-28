package iptv.common;

public class BusinessException extends Exception {
	private String msg;
	private String retcode;
	private Object retbo;
	
	
	public String getRetcode() {
		return retcode;
	}

	public void setRetcode(String retcode) {
		this.retcode = retcode;
	}
	

	public BusinessException(String msg){
		this.msg = msg; 
	}
	
	
	public BusinessException(String msg, String retcode){
		this.msg = msg; 
		this.retcode = retcode;
	}
	
	public String toString(){
		return "com.inter.exception.BusinessException:" + msg;
	}
	
	public String getMessage(){
		return msg;
	}

	public Object getRetbo() {
		return retbo;
	}

	public void setRetbo(Object retbo) {
		this.retbo = retbo;
	}

}
