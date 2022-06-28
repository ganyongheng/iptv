package iptv.task;

import com.alibaba.fastjson.JSONObject;
import iptv.config.redis.RedisCache;
import iptv.modules.base.service.impl.MobileOrderInterService;
import iptv.util.SysConfig;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于旧接口confirm_order下单的用户自动续费扫描发货
 */
@Component(value="mobileOrderFailReSendTask")
public class MobileOrderFailReSendTask extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger("mobileOrderFailReSendTask");
    private static final Logger log = LoggerFactory.getLogger(MobileOrderFailReSendTask.class);

    private static int MAX_WORKER_NUM = Runtime.getRuntime().availableProcessors() * 2 + 1;

    static {
        MobileOrderFailReSendTask.MAX_WORKER_NUM = 5;
        log.info("订单请求腾讯下单失败重试的线程数：" + MobileOrderFailReSendTask.MAX_WORKER_NUM);
    }

    private static ExecutorService pool = Executors
            .newFixedThreadPool(MobileOrderFailReSendTask.MAX_WORKER_NUM);

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MobileOrderInterService mobileOrderInterService;

    @Autowired
    private SysConfig sysConfig;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.debug("【mobileOrderFailReSendTask】开始执行任务..");

        String localIp="null";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            if(null!=addr){
                localIp=addr.getHostAddress();
            }
        } catch (UnknownHostException e) {
            log.error("【mobileOrderFailReSendTask】获取本机服务ip端口出错："+e.getCause(),e);
        }
        //执行任务时，上锁（分布式锁）
        try {
            //成功拿到锁之后 ，给锁加个超时时间，防止突然中断服务，导致锁不能释放，默认1个小时失效
            boolean lock=redisCache.setnxWithExptime("MobileOrderFailReSendTask_Lock", localIp,60*60);
            if(lock){
                try {
//					redisCache.putCacheWithExpireTime("MobileOrderFailReSendTask_Lock", localIp, 60*60);
                    //上锁成功，则执行
                    Date curDate=new Date();
                    Long curTime=curDate.getTime();

                    //取出指定范围的数据
                    RedisZSetCommands.Range score=new RedisZSetCommands.Range();
                    //大于等于
                    score.gte(0);
                    //小于等于
                    score.lte(curTime);

                    //每次取的数量
                    RedisZSetCommands.Limit nums=new RedisZSetCommands.Limit();
                    String dequeueNumsRedis=sysConfig.getMobileOrderResendDequeueTask_DequeueNums();
                    if(StringUtils.isNotBlank(dequeueNumsRedis)){
                        nums.count(Integer.valueOf(dequeueNumsRedis));
                    }else{
                        //如果redis没有值，默认取100个
                        nums.count(100);
                    }

                    //到redis集合取数据
                    Set<String> dataSet=redisCache.zRevRangeByScore("MobileOrderResendDequeueTask_SortSet", score, nums, String.class);

                    if(!dataSet.isEmpty()&&dataSet.size()>0){
                        //如果能取出集合数据，根据set的数据到指定的数据里面取出待出列的数据并push到重发队列中
                        for(String key:dataSet){
                            String data=redisCache.getCache("MobileResendOrderData_"+key, String.class);
                            if(StringUtils.isBlank(data)){
                                logger.info("订单重发任务出发送队列时，根据key[MobileResendOrderData_"+key+"]获取不到数据");
                                continue;
                            }
                            // 线程池执行具体方法
                            pool.execute(new MobileOrderFailResendForTask(data));
                            //移除集合里面的数据
                            redisCache.zRem("MobileOrderResendDequeueTask_SortSet", key);
                            redisCache.del("MobileResendOrderData_"+key);
                            continue;
                        }
                    }else{
//						logger.info("当前任务BiliOrderReConfirm_Set需要发送数据为空");
                    }
                } catch (Exception e) {
                    log.error("失败订单重试任务出错："+e.getCause(),e);
                } finally{
                    try {
                        //解锁
                        redisCache.del("MobileOrderFailReSendTask_Lock");
                    } catch (Exception e) {
                        log.error("失败订单重试任务解锁出错，请人工删除锁【MobileOrderFailReSendTask_Lock】"+e.getCause(),e);
                    }
                }
            }else{
                String curIp=redisCache.getCache("MobileOrderFailReSendTask_Lock", String.class);
//				logger.info("当前有别的节点【"+curIp+"】正在执行该任务，当前任务不执行！");
            }
        } catch (Exception e) {
            log.error("【mobileOrderFailReSendTask】连接redis出错:"+e.getCause(),e);
        }

        log.debug("【mobileOrderFailReSendTask】执行任务结束..");
    }

    //跟新失败记录的执行线程
    private class MobileOrderFailResendForTask implements Runnable {
        private String jsonData;

        public MobileOrderFailResendForTask(String jsonData) {
            this.jsonData=jsonData;
        }
        public void run() {
            try {
                //移动失败订单重试
                mobileOrderInterService.createAndConfirmOrderResend(JSONObject.parseObject(jsonData));

            } catch (Exception e) {
                logger.error("失败订单数据"+jsonData+"重试出错："+e.getCause(),e);
            }
        }
    }
}
