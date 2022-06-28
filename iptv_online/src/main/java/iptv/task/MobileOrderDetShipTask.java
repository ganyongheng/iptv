package iptv.task;


import com.alibaba.fastjson.JSONObject;
import iptv.common.BossException;
import iptv.config.redis.RedisCache;
import iptv.modules.base.entity.db.MobileOrderInfoDet;
import iptv.modules.base.service.impl.MobileOrderInterService;
import iptv.util.BizConstant;
import iptv.util.DateUtil;
import iptv.util.SysBaseUtil;
import iptv.util.SysConfig;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 *  用于旧接口confirm_order下单的用户包年或者包季拆分子订单的扫描发货
 * 个性月自动续费定时任务
 */
@Component
public class MobileOrderDetShipTask extends QuartzJobBean {

    protected static Logger logger = LoggerFactory.getLogger(MobileOrderDetShipTask.class);

    private static int MAX_WORKER_NUM = Runtime.getRuntime().availableProcessors() * 2 + 1;

    static {
        MobileOrderDetShipTask.MAX_WORKER_NUM = 5;
        logger.info("个性月自动续费定时任务线程数：" + MobileOrderDetShipTask.MAX_WORKER_NUM);
    }

    private static ExecutorService pool = Executors.newFixedThreadPool(MobileOrderDetShipTask.MAX_WORKER_NUM);

    @Autowired
    private SysBaseUtil sysBaseUtil;

    @Autowired
    private MobileOrderInterService mobileOrderInterService;

    @Autowired
    private SysConfig sysConfig;

    @Autowired
    private RedisCache redisCache;



    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

        //执行续费任务开始  读取配置，任务开关
        String account_switch="";
        try {
            account_switch= sysBaseUtil.getSysBaseParam("Mobile_Personal_Account",
                    "Mobile_Personal_Account_Switch");
        } catch (Exception e) {
            logger.error("读取数据库配置，自动续费开关出错【Mobile_Personal_Account_Switch】"+e.getCause(),e);
            return;
            //throw new Exception("读取数据库配置，自动续费开关出错【Mobile_Personal_Account_Switch】");
        }
        if(BizConstant.Code.Code_YES.equals(account_switch)){
            //开关是开启状态 获取自动续费提前续费天数
            String advance_days="";
            try {
                advance_days= sysBaseUtil.getSysBaseParam("Mobile_Personal_Account",
                        "Mobile_Personal_Account_Advance_Days");
            } catch (Exception e) {
                logger.error("读取数据库配置，获取自动续费提前续费天数出错【Mobile_Personal_Account_Advance_Days】"+e.getCause(),e);
                return;
                //throw new Exception("读取数据库配置，获取自动续费提前续费天数出错【Mobile_Personal_Account_Advance_Days】");
            }
            if(StringUtils.isBlank(advance_days)){
                logger.error("未配置获取自动续费提前续费天数出错【Mobile_Personal_Account_Advance_Days】");
                return;
                //throw new Exception("未配置获取自动续费提前续费天数出错【Mobile_Personal_Account_Advance_Days】");
            }

            //获取当前时间
            Date CurDate=new Date();
            int nums=Integer.parseInt(advance_days);
            String current_time= DateUtil.DateToString(CurDate,DateUtil.YYYY_MM_DD_HH_MM_SS);

            List<MobileOrderInfoDet> mobileOrderInfoDetList=mobileOrderInterService.getMobileOrderInfoDetList(current_time,nums);
            if(null!=mobileOrderInfoDetList&&mobileOrderInfoDetList.size()>0){
                for(MobileOrderInfoDet mobileOrderInfoDet:mobileOrderInfoDetList){
                    // 线程池执行具体方法
                    pool.execute(new MobilePersonalAccountForTask(mobileOrderInfoDet));
                }
            }else{
                logger.info("自动续费开关当前不是开启状态，本次续费退出！");
            }
        }

    }

    //开始出账
    private class MobilePersonalAccountForTask implements Runnable {
        private MobileOrderInfoDet mobileOrderInfoDet;

        public MobilePersonalAccountForTask(MobileOrderInfoDet mobileOrderInfoDet) {
            this.mobileOrderInfoDet=mobileOrderInfoDet;
        }

        public void run() {
            try {
                //多加一层开关，防止想中断时不能中断，默认为开启
                if(!BizConstant.Code.Code_NO.equals(sysConfig.getMobileDoAccountSwitch())){
                    boolean lock = false;
                    try {
                        //即使锁自动开了，此时时间戳不同了
                        lock = redisCache.setnxWithExptime("MobileOrderDetShipTask_"+mobileOrderInfoDet.getSource()+"_"+mobileOrderInfoDet.getTraceno(),"1",30);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        logger.error("redis连接失败");
                        throw new BossException();
                    }

                    if(lock){
                        JSONObject result=mobileOrderInterService.doAccountForMobileOrderInfoDet(mobileOrderInfoDet);
                    }

                  /*  if(BizConstant.Code.Result_Code_Success_Num_0.equals(result.getString("code"))){
                        //调用腾讯续费成功
                        log.info("用户id为"+mobileUserProduct.getUserid()+"，vuid为"+mobileUserProduct.getVuid()
                                +"自动续费产品包"+mobileUserProduct.getProduct_code()+"成功！"+result.getString("msg"));
                    }else{
                        //调用腾讯续费失败
                        log.error("用户id为"+mobileUserProduct.getUserid()+"，vuid为"+mobileUserProduct.getVuid()
                                +"自动续费产品包"+mobileUserProduct.getProduct_code()+"失败！"+result.getString("msg"));
                    }*/
                }
            } catch (Exception e) {
                logger.error("用户id为"+mobileOrderInfoDet.getUserid()+"，vuid为"+mobileOrderInfoDet.getVuid()
                        +"子订单续费产品包"+mobileOrderInfoDet.getVippkg()+"出错！",e);
            }
        }
    }

}
