package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


import java.io.Serializable;
import java.util.Date;


@Data
@TableName("snm_token_log")
public class SnmTokenLog implements Serializable {
	
	private static final long serialVersionUID = -93328156653919544L;



	@TableId(value = "id", type = IdType.AUTO)
	private Integer id;

	@TableField(value = "token")
	private String token;
	
	@TableField(value = "ip")
	private String ip;
	
	@TableField(value = "port")
	private String port;
	
	
	@TableField(value = "msg")
	private String msg;
		
	@TableField(value = "create_time")
	private Date createtime;
	
	@TableField(value = "expire_time")
	private Date expiretime;
	

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public Date getCreatetime() {
		return createtime;
	}

	public void setCreatetime(Date createtime) {
		this.createtime = createtime;
	}

	public Date getExpiretime() {
		return expiretime;
	}

	public void setExpiretime(Date expiretime) {
		this.expiretime = expiretime;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	
	

	
	
	

}
