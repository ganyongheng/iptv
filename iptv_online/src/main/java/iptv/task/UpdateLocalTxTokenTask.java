package iptv.task;

import com.alibaba.fastjson.JSONObject;
import iptv.config.redis.RedisCache;
import iptv.modules.tx.service.impl.MobileInterService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
public class UpdateLocalTxTokenTask extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(UpdateLocalTxTokenTask.class);

    @Autowired
    private MobileInterService mobileInterService;

    @Autowired
    private RedisCache redisCache;


    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.debug("【UpdateLocalTxTokenTask】开始执行任务..");

        try {
            JSONObject result=mobileInterService.updateLocalTxToken("后台定时刷新");
            log.info("【updateLocalTxTokenTask】本次更新token为："+result.getString("token"));
        } catch (Exception e) {
            log.error("【updateLocalTxTokenTask】更新本地腾讯accessToken出错："+e.getCause(),e);
        }
        log.debug("【UpdateLocalTxTokenTask】执行任务结束..");
    }
}
