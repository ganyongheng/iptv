package iptv.config;

import iptv.task.*;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {
    @Value("${CacheRedisVuidTask.cron}")
    private String cacheRedisVuidTaskCron;

    @Value("${MobileOrderFailReSendTask.cron}")
    private String mobileOrderFailReSendTaskCron;

    @Value("${MobileOrderSingleFailReSendTask.cron}")
    private String mobileOrderSingleFailReSendTaskCron;

    @Value("${SyncTokenToMobileTask.cron}")
    private String syncTokenToMobileTaskCron;

    @Value("${UpdateLocalTxTokenTask.cron}")
    private String updateLocalTxTokenTaskCron;

    @Value("${SyncRightsTimerTask.cron}")
    private String syncRightsTimerTaskCron;

    @Value("${MobileOrderDetShipTask.cron}")
    private String mobileOrderDetShipTaskCron;

    @Value("${MobilePersonalAccountTask.cron}")
    private String mobilePersonalAccountTaskCron;

    /**
     * redis获取vuid定时任务
     * @return
     */
    @Bean
    public JobDetail cacheRedisVuidJobDetail(){
        return JobBuilder.newJob(CacheRedisVuidTask.class)//PrintTimeJob我们的业务类
                .withIdentity("cacheRedisVuidTask")//可以给该JobDetail起一个id
                //每个JobDetail内都有一个Map，包含了关联到这个Job的数据，在Job类中可以通过context获取
                //.usingJobData("msg", "Hello Quartz")//关联键值对
                .storeDurably()//即使没有Trigger关联时，也不需要删除该JobDetail
                .build();
    }
    @Bean
    public Trigger cacheRedisVuidJobTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cacheRedisVuidTaskCron);
        return TriggerBuilder.newTrigger()
                .forJob(cacheRedisVuidJobDetail())//关联上述的JobDetail
                .withIdentity("cacheRedisVuidTaskService")//给Trigger起个名字
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    /**
     * 订单重发定时任务
     * @return
     */
    @Bean
    public JobDetail mobileOrderFailResendJobDetail(){
        return JobBuilder.newJob(MobileOrderFailReSendTask.class)//PrintTimeJob我们的业务类
                .withIdentity("mobileOrderFailReSendTask")//可以给该JobDetail起一个id
                //每个JobDetail内都有一个Map，包含了关联到这个Job的数据，在Job类中可以通过context获取
                //.usingJobData("msg", "Hello Quartz")//关联键值对
                .storeDurably()//即使没有Trigger关联时，也不需要删除该JobDetail
                .build();
    }
    @Bean
    public Trigger mobileOrderFailResendJobTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(mobileOrderFailReSendTaskCron);
        return TriggerBuilder.newTrigger()
                .forJob(mobileOrderFailResendJobDetail())//关联上述的JobDetail
                .withIdentity("mobileOrderFailResendTaskService")//给Trigger起个名字
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    /**
     * 新流程订单重发定时任务
     * @return
     */
    @Bean
    public JobDetail mobileOrderSingleFailReSendJobDetail(){
        return JobBuilder.newJob(MobileOrderSingleFailReSendTask.class)//PrintTimeJob我们的业务类
                .withIdentity("mobileOrderSingleFailReSendTask")//可以给该JobDetail起一个id
                //每个JobDetail内都有一个Map，包含了关联到这个Job的数据，在Job类中可以通过context获取
                //.usingJobData("msg", "Hello Quartz")//关联键值对
                .storeDurably()//即使没有Trigger关联时，也不需要删除该JobDetail
                .build();
    }
    @Bean
    public Trigger mobileOrderSingleFailReSendJobTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(mobileOrderSingleFailReSendTaskCron);
        return TriggerBuilder.newTrigger()
                .forJob(mobileOrderSingleFailReSendJobDetail())//关联上述的JobDetail
                .withIdentity("mobileOrderSingleFailReSendTaskService")//给Trigger起个名字
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    /**
     * 同步token定时任务
     * @return
     */
    @Bean
    public JobDetail syncTokenToMobileJobDetail(){
        return JobBuilder.newJob(SyncTokenToMobileTask.class)//PrintTimeJob我们的业务类
                .withIdentity("syncTokenToMobileTask")//可以给该JobDetail起一个id
                //每个JobDetail内都有一个Map，包含了关联到这个Job的数据，在Job类中可以通过context获取
                //.usingJobData("msg", "Hello Quartz")//关联键值对
                .storeDurably()//即使没有Trigger关联时，也不需要删除该JobDetail
                .build();
    }
    @Bean
    public Trigger syncTokenToMobileJobTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(syncTokenToMobileTaskCron);
        return TriggerBuilder.newTrigger()
                .forJob(syncTokenToMobileJobDetail())//关联上述的JobDetail
                .withIdentity("syncTokenToMobileTaskService")//给Trigger起个名字
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    /**
     * 更新token定时任务
     * @return
     */
    @Bean
    public JobDetail updateLocalTxTokenJobDetail(){
        return JobBuilder.newJob(UpdateLocalTxTokenTask.class)//PrintTimeJob我们的业务类
                .withIdentity("updateLocalTxTokenTask")//可以给该JobDetail起一个id
                //每个JobDetail内都有一个Map，包含了关联到这个Job的数据，在Job类中可以通过context获取
                //.usingJobData("msg", "Hello Quartz")//关联键值对
                .storeDurably()//即使没有Trigger关联时，也不需要删除该JobDetail
                .build();
    }
    @Bean
    public Trigger updateLocalTxTokenJobTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(updateLocalTxTokenTaskCron);
        return TriggerBuilder.newTrigger()
                .forJob(updateLocalTxTokenJobDetail())//关联上述的JobDetail
                .withIdentity("updateLocalTxTokenTaskService")//给Trigger起个名字
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    /**
     * 同步权益时间到3A
     * @return
     */
    @Bean
    public JobDetail syncRightsTimerTaskDetail(){
        return JobBuilder.newJob(SyncRightsTimerTask.class)//PrintTimeJob我们的业务类
                .withIdentity("syncRightsTimerTask")//可以给该JobDetail起一个id
                //每个JobDetail内都有一个Map，包含了关联到这个Job的数据，在Job类中可以通过context获取
                //.usingJobData("msg", "Hello Quartz")//关联键值对
                .storeDurably()//即使没有Trigger关联时，也不需要删除该JobDetail
                .build();
    }
    @Bean
    public Trigger syncRightsTimerTaskTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(syncRightsTimerTaskCron);
        return TriggerBuilder.newTrigger()
                .forJob(syncRightsTimerTaskDetail())//关联上述的JobDetail
                .withIdentity("syncRightsTimerTaskService")//给Trigger起个名字
                .withSchedule(cronScheduleBuilder)
                .build();
    }


    @Bean
    public JobDetail mobileOrderDetShipTaskDetail(){
        return JobBuilder.newJob(MobileOrderDetShipTask.class)//PrintTimeJob我们的业务类
                .withIdentity("mobileOrderDetShipTask")//可以给该JobDetail起一个id
                //每个JobDetail内都有一个Map，包含了关联到这个Job的数据，在Job类中可以通过context获取
                //.usingJobData("msg", "Hello Quartz")//关联键值对
                .storeDurably()//即使没有Trigger关联时，也不需要删除该JobDetail
                .build();
    }
    @Bean
    public Trigger mobileOrderDetShipTaskTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(mobileOrderDetShipTaskCron);
        return TriggerBuilder.newTrigger()
                .forJob(mobileOrderDetShipTaskDetail())//关联上述的JobDetail
                .withIdentity("mobileOrderDetShipTaskService")//给Trigger起个名字
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail mobilePersonalAccountTaskDetail(){
        return JobBuilder.newJob(MobilePersonalAccountTask.class)//PrintTimeJob我们的业务类
                .withIdentity("mobilePersonalAccountTask")//可以给该JobDetail起一个id
                //每个JobDetail内都有一个Map，包含了关联到这个Job的数据，在Job类中可以通过context获取
                //.usingJobData("msg", "Hello Quartz")//关联键值对
                .storeDurably()//即使没有Trigger关联时，也不需要删除该JobDetail
                .build();
    }
    @Bean
    public Trigger mobilePersonalAccountTaskTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(mobilePersonalAccountTaskCron);
        return TriggerBuilder.newTrigger()
                .forJob(mobilePersonalAccountTaskDetail())//关联上述的JobDetail
                .withIdentity("mobilePersonalAccountTaskService")//给Trigger起个名字
                .withSchedule(cronScheduleBuilder)
                .build();
    }

}