package iptv.task;


import iptv.modules.youku.service.impl.MobileDownOrderInfoServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 优酷下载用户订单数据
 * 该定时任务暂不开启
 */
@Component
public class DowmYoukuOrderTask {

    protected Logger logger = LoggerFactory.getLogger(DowmYoukuOrderTask.class);

    @Autowired
    private MobileDownOrderInfoServiceImpl mobileDownOrderInfoService;


    public void syncTokenToMobileTask() {
        logger.info("【dowmMobileOrderTask】开始执行任务..");
        mobileDownOrderInfoService.operate();
        logger.info("【dowmMobileOrderTask】执行任务结束..");

    }

}
