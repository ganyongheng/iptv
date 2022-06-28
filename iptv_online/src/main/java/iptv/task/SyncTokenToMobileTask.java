package iptv.task;

import iptv.modules.tx.service.impl.MobileInterService;
import iptv.util.SysBaseUtil;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SyncTokenToMobileTask extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger("syncTokenToMobileTaskLogger");


    @Autowired
    private MobileInterService mobileInterService;

    @Autowired
    private SysBaseUtil sysBaseUtil;


    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        logger.info("【syncTokenToMobileTask】开始执行任务..");

        Map<String, String> urlList=new HashMap<String, String>();
        //获取需要同步token的第三方系统url配置
        try {
            urlList=sysBaseUtil.getSysBaseParam("SYNC_TOKEN_TO_THIRD_SYSTEM_URL");
        } catch (Exception e1) {
            logger.error("获取需要同步token的第三方系统url配置出错："+e1.getCause(),e1);
        }

        if(null!=urlList&&!urlList.isEmpty()){
            for(String source:urlList.keySet()){
                // 调用接口进行同步
                try {
                    if(StringUtils.isNotBlank(urlList.get(source))){
                        String[]url_list=urlList.get(source).split(";");
                        for(String url:url_list){
                            mobileInterService.getAccessTokenFromTxForMobile(source,url,logger,"后台定时刷新");
                        }
                    }
                } catch (Exception e) {
                    logger.error("同步token给"+source+"出错："+e.getCause(),e);
                }
            }
        }

        logger.info("【syncTokenToMobileTask】执行任务结束..");


    }
}
