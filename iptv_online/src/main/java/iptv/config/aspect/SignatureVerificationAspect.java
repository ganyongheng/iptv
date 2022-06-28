package iptv.config.aspect;

import com.alibaba.fastjson.JSONObject;
import iptv.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;

/**
 * 验签切面(用于智能音箱)
 */
@Component
@Aspect
public class SignatureVerificationAspect {

    private static final Logger log = LoggerFactory.getLogger(SignatureVerificationAspect.class);

    private static final Logger getMsg = LoggerFactory.getLogger("getMsg");
    private static final Logger checkMsg = LoggerFactory.getLogger("checkMsg");
    /**智能音箱移动业管平台订购关系同步日志*/
    private static final Logger syncAccount = LoggerFactory.getLogger("syncAccount");

    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS");

    private static HashSet<String> mathodSet = null;
    static {
        mathodSet = new HashSet<>((int) (4/.75f) + 1);
        mathodSet.add("getMsg");
        mathodSet.add("checkMsg");
        mathodSet.add("loginSuccess");
        mathodSet.add("getProduct");
    }

    @Pointcut("execution(* iptv.modules.music.controller.MusicPayController.*(..))")
    public void pointCut() {

    }


    /**
     * 环绕通知验签失败直接返回
     * @param joinPoint
     * @return
     */
    @Around("pointCut()")
    public String afterReturn(ProceedingJoinPoint joinPoint) {
        JSONObject response = new JSONObject();
        Date start = new Date();
        Date end;
        try {
            //获取方法名
            String methodName = joinPoint.getSignature().getName();
            //获取ip
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getRemoteHost()+":"+request.getRemotePort();
            //获取请求参数
            String req = JSONObject.toJSONString(joinPoint.getArgs()[0]);
            //开始验签
            boolean falge = inputParameterVerification(joinPoint);
            if(falge){
                Object result = joinPoint.proceed();
                end = new Date();
                response = JSONObject.parseObject(result.toString());
            }else{
                //验签错误直接返回
                //TODO 确定签名错误返回值
                response.put("code","999");
                response.put("msg","签名错误");
                end = new Date();
                printLog(methodName,ip,req,start,end,response.toString());
                return response.toString();
            }
            printLog(methodName,ip,req,start,end,response.toString());
            return response.toString();
        } catch (Throwable e) {
            e.printStackTrace();
            log.error("SignatureVerificationAspect切面验签(智能音箱)增强方法出错",e);
            response.put("code","999");
            response.put("msg","系统内部错误");
            return response.toString();
        }
    }

    /**
     * 打印日志
     * @param methodName 方法名
     * @param ip  ip地址
     * @param req 请求参数
     * @param start 开始时间
     * @param end   结束时间
     * @param resp  返回参数
     */
    public void printLog(String methodName,String ip,String req,Date start,Date end,String resp){
        String logStr = "date="+dateFormat.format(start.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())+",totaltime=" +
                String.valueOf(end.getTime()-start.getTime())+"ms,method="+methodName+",reqIp="+ip
                +",reqJson="+req+",respJson="+resp;
        switch (methodName) {
            //TODO 日志目录需要修改
            case "getMsg":log.info(logStr);break;
            case "checkMsg":log.info(logStr);break;
            case "syncAccount":syncAccount.info(logStr);break;
            default:log.info(logStr);
        }
    }

    /**
     * 校验签名
     * @param joinPoint
     * @return
     */
    public boolean inputParameterVerification(JoinPoint joinPoint){
        //获取方法名
        String methodName = joinPoint.getSignature().getName();
        //如果有方法不需要验签这里匹对方法名直接返回true
        JSONObject object = (JSONObject)joinPoint.getArgs()[0];
        String autograph = "";
        try {
            //TODO 这里需要修改签名盐值
            if (mathodSet.contains(methodName)) {
                autograph = MD5Util.encode(object.getString("transactionID") + "snm");
            } else {
                autograph = MD5Util.md5Encode(object.toJSONString(), "1");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("方法："+methodName+"，请求参数："+object.toJSONString()+" 获取签名错误",e);
        }
        String sign = object.getString("sign");
        if(StringUtils.isBlank(sign) || !sign.equals(autograph)){
            //测试查看签名上线可注掉
            log.info("正确签名为:"+autograph);
            return false;
        }else{
            return true;
        }
    }

}
