package iptv.task;

import iptv.config.redis.RedisCache;
import iptv.modules.base.service.impl.MobileUserInterService;
import iptv.util.ConstDef;
import iptv.util.SysBaseUtil;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
public class CacheRedisVuidTask extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(CacheRedisVuidTask.class);


    @Autowired
    private SysBaseUtil sysBaseUtil;

    @Autowired
    private MobileUserInterService mobileUserInterService;

    @Autowired
    private RedisCache redisCache;


    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        boolean lock = false;
        try {
            //Long lLen = redisCache.lLen(ConstDef.KEY_REDIS);
            //更换新key
            Long lLen = redisCache.lLen(ConstDef.KEY_REDIS_NEW);
            String countmax = sysBaseUtil.getSysBaseParam("MAX_VUID_COUNT", "MAX_VUID_COUNT");
            //为了防止并发时间戳重复，这里加一把锁
            if(lLen<Integer.valueOf(countmax)){
                try {
                    //即使锁自动开了，此时时间戳不同了
                    lock = redisCache.setnxWithExptime("CacheRedisVuidTask_Lock","CacheRedisVuidTask_Lock",30);
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    log.error("redis连接失败"+e1.getCause());
                }
                if (lock) {
                    String count = sysBaseUtil.getSysBaseParam("VUID_COUNT", "VUID_COUNT");
                    mobileUserInterService.pushVuidRedisCache(1,Integer.valueOf(count));
                    log.info("目前redis剩余数量"+lLen+"；开始往redis缓存数据");
                }else{
                    log.info("已经存在往redis缓存数据任务");
                }

            }else{
                log.info("目前redis剩余数量"+lLen);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            try {
                if(lock){
                    redisCache.del("CacheRedisVuidTask_Lock");
                }
            } catch (Exception e) {
                log.error("CacheRedisVuidTask_Lock在获取vuid时解锁出错：" + e.getCause(), e);
            }
        }
    }
}
