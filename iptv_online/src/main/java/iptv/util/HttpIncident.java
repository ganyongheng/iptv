package iptv.util;

import iptv.modules.base.service.impl.IncidentPushServcie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


//此处注解不能省却（0）作用是将（2）ntClient = this;this进行赋值（注：如果无注解ntClient 将null）
@Component
public class HttpIncident {
	/**
	 * 此处是要使用的service需要spring注入（1）
	 */
	@Autowired
	private HttpUtilsException httpUtilsException;
	private static HttpIncident ntClient;

	@Autowired
	private IncidentPushServcie incidentPushServcie;

	/**
	 * 注意此处注解（2）
	 * @PostConstruct修饰的方法会在服务器加载Servle的时候运行，并且只会被服务器执行一次。PostConstruct在构造函数之后执行,init()方法之前执行。
	 * 先给该类赋值，然后通过（1）出注入进来。这样不影响dao等service下面调用的注入！
	 */
	@PostConstruct
	public void init() {
		ntClient = this;
		ntClient.httpUtilsException = this.httpUtilsException;
	}

	/**
	 * 主要使用场景（3）
	 * 使用这样模式的调用方式httpUtilsException现在是作为HttpIncident的属性
	 */
	public static void incidentPush(String incidentdesc,String lvlcode,String incidentcategor) {
		ntClient.httpUtilsException.httpUtilsIncidentPush(incidentdesc,lvlcode,incidentcategor);
	}
	/**
	 * 发送一次告警
	 * @param incidentdesc
	 * @param lvlcode
	 * @param incidentcategor
	 */
	public void incidentPush_ONCE(String incidentdesc,String lvlcode,String incidentcategor) {
		incidentPushServcie.incidentPush(incidentdesc, lvlcode, incidentcategor);
	}
}
