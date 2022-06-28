package iptv.config.aspect;

import com.alibaba.fastjson.JSONObject;
import iptv.config.redis.RedisCache;
import iptv.util.BizConstant;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Order(2)
public class ReqParamAspect {
	
	private static Logger log = LoggerFactory.getLogger("publicReqParamLogger");
	
	@Autowired
	private RedisCache redisCache;
	

	@Pointcut("execution(* iptv.modules.tx.controller.PublicController.*(..)) && !execution(* iptv.modules.tx.controller.PublicController.test*(..)) ")
	public void pointCut() {

	}
	/**
	 * 进入方法前处理
	 * 
	 * @param point
	 */
	@Before("pointCut()")
	public void doBefore(JoinPoint point) {
		Object[] args = point.getArgs();
		for(Object obj : args){
			if(obj instanceof JSONObject){
				JSONObject req = (JSONObject)obj;
				String methodName = point.getSignature().getName();
				log.info("Method Name : [" + methodName + "] --->"+req.toJSONString());
				String source = req.getString("source");
//				if(req.getString("cooperatorCode")!=null&&StringUtils.isNotBlank(req.getString("cooperatorCode"))){
//					if(source.length()-source.replaceAll("_", "").length()>1){
//						req.put("source", req.getString("source").subSequence(0, source.lastIndexOf("_")));
//					}
//					return;
//				}
//				if(source.length()-source.replaceAll("_", "").length()>1){
				if(source.contains("_youku")){
					req.put("cooperatorCode", BizConstant.Code.Order.Cooperator_Code_Youku);
					req.put("source", req.getString("source").subSequence(0, source.lastIndexOf("_")));
				}else if(source.contains("_tencent")){
					req.put("cooperatorCode", BizConstant.Code.Order.Cooperator_Code_Tencent);
					req.put("source", req.getString("source").subSequence(0, source.lastIndexOf("_")));
				} else if (source.contains("_aiqiyi")) {
					req.put("cooperatorCode", BizConstant.Code.Order.Cooperator_Code_Aiqiyi);
					req.put("source", req.getString("source").subSequence(0, source.lastIndexOf("_")));
				} else {
					req.put("cooperatorCode", BizConstant.Code.Order.Cooperator_Code_Tencent);
				}
				
				if("confirmOrderSingle".equals(methodName)){
					/*if(BizConstant.Code.Order.Cooperator_Code_Tencent.equals(req.getString("cooperatorCode"))){
						req.put("vippkg","basic_1");
					}
					if(BizConstant.Code.Order.Cooperator_Code_Youku.equals(req.getString("cooperatorCode"))){
						req.put("vippkg","youku_1");
					}*/
					/*if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(req.getString("cooperatorCode"))) {
						return;
					}*/

					String vippkg = req.getString("vippkg");
					if(StringUtils.isNotBlank(vippkg)&&vippkg.contains("_")){
						//如果为空的话，接口会返回参数，不包含_,接口会返回找不到产品编码
						String[] split = vippkg.split("_");
						req.put("vippkg",split[0]+"_1");
					}
				}
			}
		}
		
	}

}
