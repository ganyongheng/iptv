package iptv.task;


import com.alibaba.fastjson.JSONObject;
import iptv.modules.base.service.impl.MobileOrderInterService;
import iptv.modules.tx.entity.db.MobileUserProduct;
import iptv.modules.tx.service.impl.MobileUserProductServiceImpl;
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

@Component
public class MobilePersonalAccountTask extends QuartzJobBean {

    protected static Logger log = LoggerFactory.getLogger(MobilePersonalAccountTask.class);

    private static int MAX_WORKER_NUM = Runtime.getRuntime().availableProcessors() * 2 + 1;

    static {
        MobilePersonalAccountTask.MAX_WORKER_NUM = 5;
        log.info("个性月自动续费定时任务线程数：" + MobilePersonalAccountTask.MAX_WORKER_NUM);
    }

    private static ExecutorService pool = Executors.newFixedThreadPool(MobilePersonalAccountTask.MAX_WORKER_NUM);

    @Autowired
    private SysBaseUtil sysBaseUtil;

    @Autowired
    private SysConfig sysConfig;

    @Autowired
    private MobileUserProductServiceImpl mobileUserProductService;
    @Autowired
    private MobileOrderInterService mobileOrderInterService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

        String account_switch="";

        try {
            account_switch= sysBaseUtil.getSysBaseParam("Mobile_Personal_Account",
                    "Mobile_Personal_Account_Switch");
        } catch (Exception e) {
            log.error("读取数据库配置，自动续费开关出错【Mobile_Personal_Account_Switch】"+e.getCause(),e);
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
                log.error("读取数据库配置，获取自动续费提前续费天数出错【Mobile_Personal_Account_Advance_Days】"+e.getCause(),e);
                return;
                //throw new Exception("读取数据库配置，获取自动续费提前续费天数出错【Mobile_Personal_Account_Advance_Days】");
            }
            if(StringUtils.isBlank(advance_days)){
                log.error("未配置获取自动续费提前续费天数出错【Mobile_Personal_Account_Advance_Days】");
                return;
                //throw new Exception("未配置获取自动续费提前续费天数出错【Mobile_Personal_Account_Advance_Days】");
            }

            //获取当前时间
            Date CurDate=new Date();
            int nums=Integer.parseInt(advance_days);
            String current_time= DateUtil.DateToString(CurDate,DateUtil.YYYY_MM_DD_HH_MM_SS);

            List<MobileUserProduct> mobileUserProductList=mobileUserProductService.getMobileUserProductAccountList(current_time,nums);
            if(null!=mobileUserProductList&&mobileUserProductList.size()>0){
                for(MobileUserProduct mobileUserProduct:mobileUserProductList){
                    // 线程池执行具体方法
                    pool.execute(new MobilePersonalAccountForTask(mobileUserProduct));
                }
            }
        }else{
            log.info("自动续费开关当前不是开启状态，本次续费退出！");
        }
    }

    /**
     * 出账执行类
     */
    private class MobilePersonalAccountForTask implements Runnable {
        private MobileUserProduct mobileUserProduct;

        public MobilePersonalAccountForTask(MobileUserProduct mobileUserProduct) {
            this.mobileUserProduct=mobileUserProduct;
        }
        public void run() {
            try {
                //多加一层开关，防止想中断时不能中断，默认为开启
                if(!BizConstant.Code.Code_NO.equals(sysConfig.getMobileDoAccountSwitch())){
                    JSONObject result=mobileOrderInterService.doAccountForMobileUserProduct(mobileUserProduct);
/*                    if(BizConstant.Code.Result_Code_Success_Num_0.equals(result.getString("code"))){
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
                log.error("用户id为"+mobileUserProduct.getUserId()+"，vuid为"+mobileUserProduct.getVuid()
                        +"自动续费产品包"+mobileUserProduct.getProductCode()+"出错！",e);
            }
        }
    }
}
