package iptv.config.log;

import com.alibaba.fastjson.JSONObject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
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

@Aspect
@Component
public class InterControllerLogAspect {
    //线程安全
    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS");

    private ThreadLocal<InterLog> threadLocalLog = new  ThreadLocal<InterLog>();

    private static final Logger testGetLogger = LoggerFactory.getLogger("testGetLogger");
    private static final Logger getAccessTokenLogger = LoggerFactory.getLogger("getAccessTokenLogger");
    private static final Logger logger = LoggerFactory.getLogger("defultLogger");
    private static final Logger vipInfoLogger = LoggerFactory.getLogger("vipInfoProcess");
    private static final Logger confirmOrderSingleLogger = LoggerFactory.getLogger("confirmOrderSingleProcess");
    private static final Logger refundAutopayLogger = LoggerFactory.getLogger("refundAutopayLogger");


    @Pointcut("execution(* iptv.modules.tx.controller.*.*(..))")
    public void pointCut() {

    }
    @Before("pointCut()")
    public void boBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        InterLog interLog=new InterLog();
        interLog.setReqIp(request.getRemoteHost()+":"+request.getRemotePort());
        interLog.setDate(new Date());
        interLog.setReqJson(JSONObject.toJSONString(joinPoint.getArgs()[0]));
        interLog.setMethod(joinPoint.getSignature().getName());
        threadLocalLog.set(interLog);
    }
    @AfterReturning(pointcut = "pointCut()",returning = "result")
    public void afterReturn(JoinPoint joinPoint, Object result) {
        InterLog interLog =threadLocalLog.get();
        if(null==result){
            interLog.setRespJson(null);
        }else{
            interLog.setRespJson(result.toString());
        }
        interLog.setTotaltime(String.valueOf(new Date().getTime()-interLog.getDate().getTime()));
        //打印日志
        printLogWithInterLog(interLog);
    }

    public void printLogWithInterLog(InterLog interLog) {
        String logStr="date="+dateFormat.format(interLog.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())+",totaltime="+interLog.getTotaltime()+"ms"
                + ",method="+interLog.getMethod()+",reqIp="+interLog.getReqIp()+",reqJson="
                + interLog.getReqJson()+",respJson="+interLog.getRespJson();

        switch (interLog.getMethod()) {
            case "testGetLogger":testGetLogger.info(logStr);break;
            case "getAccessToken":getAccessTokenLogger.info(logStr);break;
            case "vipInfo":vipInfoLogger.info(logStr);break;
            case "confirmOrderSingle":confirmOrderSingleLogger.info(logStr);break;
            case "refundAutopay":refundAutopayLogger.info(logStr);break;
            default:logger.info(logStr);
        }
    }

}
