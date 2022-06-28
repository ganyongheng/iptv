package iptv.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import iptv.config.redis.RedisCache;
import iptv.modules.aiqiyi.service.impl.MobileUserProductAiqiyiServiceImpl;
import iptv.modules.base.service.impl.IncidentPushServcie;
import iptv.modules.base.service.impl.MobileAutopayConfigService;
import iptv.modules.tx.service.impl.MobileInterService;
import iptv.modules.tx.service.impl.MobileUserProductServiceImpl;
import iptv.modules.youku.service.impl.MobileUserProductYoukuServiceImpl;
import iptv.util.*;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.connection.RedisZSetCommands.Range;
import org.springframework.data.redis.connection.RedisZSetCommands.Limit;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 同步会员权益到3A定时任务(腾讯,优酷,爱奇艺)
 */
@Component
public class SyncRightsTimerTask extends QuartzJobBean {


    protected Logger logger = LoggerFactory.getLogger(SyncRightsTimerTask.class);

    protected Logger taskLog = LoggerFactory.getLogger("syncRightsTimeLogger");

    protected Logger dataLog = LoggerFactory.getLogger("syncRightsDataLogger");

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static ExecutorService pool = Executors.newFixedThreadPool(5);

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MobileAutopayConfigService mobileAutopayConfigService ;

    @Autowired
    private MobileInterService mobileInterService ;

    @Autowired
    private MobileUserProductServiceImpl mobileUserProductService ;

    @Autowired
    private MobileUserProductYoukuServiceImpl mobileUserProductYoukuService ;

    @Autowired
    private MobileUserProductAiqiyiServiceImpl mobileUserProductAiqiyiService;


    @Autowired
    private SysConfig sysConfig;

    @Autowired
    private IncidentPushServcie incidentPushServcie;

    @Autowired
    private YouKuRequstUtils youKuRequstUtils;




    /**
     * 同步微信权益时vip_bid值选择
     */
    private static Map<String,String> Vip_BidMap = new HashMap<String,String>();

    @PostConstruct
    public void init() {
        //初始化Vip_BidMap
        try{
            if(null == sysConfig.getVip_Bid() || "".equals(sysConfig.getVip_Bid())){
                logger.error("同步微信权益时vip_bid值选择暂未配置！！！");
            }
            String[] split = sysConfig.getVip_Bid().split("&&");
            for (String str : split) {
                String[] split2 = str.split("#");
                Vip_BidMap.put(split2[0], split2[1]);
            }
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("同步微信权益时vip_bid值选择获取失败！！！");
        }
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            //成功拿到锁之后 ,给锁加个超时时间,防止突然中断服务,导致锁不能释放,默认1个小时失效
            boolean lock=redisCache.setnxWithExptime("SyncRightsTimer_Lock", "timer_lock",60*60);
            if(lock){
                try {

                    //上锁成功,则执行
                    Date curDate=new Date();
                    Long curTime=curDate.getTime();
                    //取出指定范围的数据
                    Range score=new Range();
                    //大于等于
                    score.gte(0);
                    //小于等于
                    String delayTime = sysConfig.getSyncRightsTimer_DelayTime();
                    if (StringUtils.isBlank(delayTime)) {
                        delayTime = "5";//默认5分钟(延迟时间,取数据!)
                    }
                    Long delay = Long.parseLong(delayTime);
                    logger.info("延迟获取数据时间:"+delayTime+"分钟!");
                    curTime = curTime-delay*60*1000 ;
                    score.lte(curTime);

                    //每次取的数量
                    Limit nums=new Limit();
                    String dequeueNumsRedis=sysConfig.getSyncRightsTimer_DequeueNums();
                    if(StringUtils.isNotBlank(dequeueNumsRedis)){
                        nums.count(Integer.valueOf(dequeueNumsRedis));
                    }else{
                        //如果redis没有值,默认取50000个
                        nums.count(50000);
                    }

                    //到redis集合取数据
                    Set<String> dataSet = redisCache.zRevRangeByScore("SyncRightsTimer_SortSet", score, nums, String.class);
                    logger.info("获取时间:"+format2.format(new Date(curTime))+"之前数据:"+ JSON.toJSONString(dataSet));

                    String syncRightsTime_Max_Nums = sysConfig.getSyncRightsTime_Max_Nums();
                    if (StringUtils.isBlank(syncRightsTime_Max_Nums)) {
                        //订单状态回调通知最大次数
                        syncRightsTime_Max_Nums = "3";
                    }

                    if(dataSet != null  && dataSet.size()>0){
                        Map<String, String> map = mobileAutopayConfigService.getMapBySource();
                        logger.info("********获取配置source与url:"+JSON.toJSONString(map));
                        taskLog.info("********获取配置source与url:"+JSON.toJSONString(map));
                        taskLog.info("获取时间:"+format2.format(new Date(curTime))+"之前数据:"+JSON.toJSONString(dataSet));
                        for(String value : dataSet){
                            JSONObject data = JSONObject.parseObject(value);
                            if(data == null || data.size() == 0 ){
                                continue;
                            }
                            //重发次数
                            Integer resendNum = data.getIntValue("nums");
                            if (resendNum >= Integer.valueOf(syncRightsTime_Max_Nums) ) {
                                //告警
                                alarm("syncRightsTime_订单ext_traceno为[" + data.getString("ext_traceno") + "]用户id为[" + data.getString("vuid")+ "]的数据订单状态回调通知失败达到最大次数");
                                taskLog.info("syncRightsTime_订单ext_traceno为[" + data.getString("ext_traceno") + "]用户id为[" + data.getString("vuid")+ "]的数据订单状态回调通知失败达到最大次数");
                                logger.info("syncRightsTime_订单ext_traceno为[" + data.getString("ext_traceno") + "]用户id为[" + data.getString("vuid")+ "]的数据订单状态回调通知失败达到最大次数");
                            }else{
                                // 线程池执行具体方法
                                pool.execute(new SyncRightsTask(data , map));
                            }
                        }
                        //移除集合里面的数据
                        redisCache.zRemRangeByScore("SyncRightsTimer_SortSet", 0, curTime);
                    }
                }catch (Exception e){
                    logger.error("****SyncRightsTimer****用户权益时间验证出错:"+e.getCause(),e);
                    e.printStackTrace();
                } finally{
                    try {
                        //解锁
                        redisCache.del("SyncRightsTimer_Lock");
                    } catch (Exception e) {
                        logger.error("*****解锁报错_【SyncRightsTimer_Lock】"+e.getCause(),e);
                    }
                }
            }else{
                logger.info("获取SyncRightsTimer_Lock锁失败!");
            }
        }catch (Exception e){
            logger.error("【SyncRightsTimer】连接redis出错:"+e.getCause(),e);
            e.printStackTrace();
        }
    }


    /**
     * 线程类
     */
    private class SyncRightsTask implements Runnable{
        private JSONObject data ;
        private Map<String, String> map ;
        public SyncRightsTask(JSONObject data , Map<String, String> map) {
            this.data = data ;
            this.map = map ;
        }
        @Override
        public void run() {
            syncRights(data,map);
        }
    }

    /**
     *具体定时任务执行方法
     * @param data 订单数据
     * @param map  key为source value为url
     */
    public void syncRights(JSONObject data ,  Map<String, String> map){
        //订单唯一标识
        String ext_traceno = data.get("ext_traceno") == null ? "" : String.valueOf(data.get("ext_traceno"));
        //需要校验的权益时间
        String vip_endtime_calculate = data.get("vip_endtime_calculate") == null ? "" : String.valueOf(data.get("vip_endtime_calculate"));
        taskLog.info("订单号ext_traceno="+ext_traceno+",*****需要校验的权益时间:"+vip_endtime_calculate);
        logger.info("订单号ext_traceno="+ext_traceno+",*****需要校验的权益时间:"+vip_endtime_calculate);
        Integer vipCalculateTime = 0 ;
        if(StringUtils.isNotBlank(vip_endtime_calculate)){
            vipCalculateTime = Integer.parseInt(vip_endtime_calculate);
        }
        String product_type = data.getString("product_type");
        String vuid = data.getString("vuid");
        String notify_type = data.get("notify_type") == null ? "" : String.valueOf(data.get("notify_type"));
        //tencent  youku  aiqiyi
        String cooperatorCode = data.get("cooperator_code") == null ? "" : String.valueOf(data.get("cooperator_code"));

        JSONObject requestParam = new JSONObject();
        //必填
        requestParam.put("vuid", vuid);
        JSONObject req = new JSONObject();

        String source = String.valueOf(data.get("source"));
        String userid = String.valueOf(data.get("userid"));

        if(StringUtils.isBlank(ext_traceno)){
            alarm("syncRightsTime_订单ext_traceno为[" + ext_traceno + "]用户id为[" + vuid+ "]的数据从订单获取ext_traceno为空!");
            taskLog.info("订单号ext_traceno="+ext_traceno+",从订单获取ext_traceno为空,已告警!"+JSON.toJSONString(data));
            logger.info("订单号ext_traceno="+ext_traceno+",从订单获取ext_traceno为空,已告警!"+JSON.toJSONString(data));
            return ;
        }

        if(StringUtils.isBlank(vuid)){
            //告警
            alarm("syncRightsTime_订单ext_traceno为[" + ext_traceno + "]用户id为[" + vuid+ "]的数据,vuid为空!");
            taskLog.info("vuid为空,已告警!"+JSON.toJSONString(data));
            logger.error("vuid为空,已告警!"+JSON.toJSONString(data));
            return ;
        }
        if(StringUtils.isBlank(source)){
            //告警
            alarm("syncRightsTime_订单ext_traceno为[" + ext_traceno + "]用户id为[" + vuid+ "]的数据,source为空!");
            taskLog.info("source为空,已告警!"+JSON.toJSONString(data));
            logger.error("source为空,已告警!"+JSON.toJSONString(data));
            return ;
        }
        if(StringUtils.isBlank(userid)){
            //告警
            alarm("syncRightsTime_订单ext_traceno为[" + ext_traceno + "]用户id为[" + vuid+ "]的数据,userid为空!");
            taskLog.info("userid为空,已告警!"+JSON.toJSONString(data));
            logger.error("userid为空,已告警!"+JSON.toJSONString(data));
            return ;
        }

        if(StringUtils.isBlank(notify_type)){
            //告警
            alarm("syncRightsTime_订单ext_traceno为[" + ext_traceno + "]用户id为[" + vuid+ "]的数据,notify_type为空!");
            taskLog.info("订单号ext_traceno="+ext_traceno+",notify_type为空,已告警!"+JSON.toJSONString(data));
            logger.error("订单号ext_traceno="+ext_traceno+",notify_type为空,已告警!"+JSON.toJSONString(data));
            return ;
        }


        taskLog.info("订单号ext_traceno="+ext_traceno+",cooperator_code:"+cooperatorCode);
        logger.info("订单号ext_traceno="+ext_traceno+",cooperator_code:"+cooperatorCode);

        req.put("source", source);

        try {
            //必填
            requestParam.put("ext_traceno", ext_traceno);
            String req_source = "";
            if("youku".equals(cooperatorCode)){
                req_source = source+"_youku";
            } else if ("aiqiyi".equals(cooperatorCode)) {
                req_source = source + "_aiqiyi";
            } else {
                req_source = source;
            }
            requestParam.put("source", req_source);//必填
            requestParam.put("notify_type", notify_type);//必填
            //添加产品类型(适配融合包多个权益查询)
            req.put("product_type", product_type);
            req.put("vuid", vuid);
            req.put("cooperatorCode", cooperatorCode);
            taskLog.info("订单号ext_traceno="+ext_traceno+",获取会员权益信息参数:"+JSON.toJSONString(req));
            //获取会员权益时间
            JSONObject vipInfo = vipInfo(req);
            taskLog.info("订单号ext_traceno="+ext_traceno+",获取会员权益返回值:"+JSON.toJSONString(vipInfo));
            logger.info("订单号ext_traceno="+ext_traceno+",获取会员权益返回值:"+JSON.toJSONString(vipInfo));
            Object code = vipInfo.get("code");
            if(code != null && BizConstant.Code.Result_Code_Success_Num_0.equals(String.valueOf(code))){
                Object end = vipInfo.get("end");//权益到期时间
                taskLog.info("订单号ext_traceno="+ext_traceno+",获取会员权益到期时间:"+end);
                logger.info("订单号ext_traceno="+ext_traceno+",获取会员权益到期时间:"+end);
                Integer rightTime = null ;
                if(end != null ){
                    //获取到腾讯或者优酷的权益时间
                    rightTime = Integer.parseInt(String.valueOf(end));
                }
                String url = map.get(source);
                //拿到空的需要校验的权益时间 则把rightTime时间的当天最后时间发送运营商(腾讯)
                if(rightTime != null && rightTime > 0 && vipCalculateTime == 0){
                    taskLog.info("订单号ext_traceno="+ext_traceno+",校验权益时间为空,直接把权益时间处理后通知运营商*********");
                    logger.info("订单号ext_traceno="+ext_traceno+",校验权益时间为空,直接把权益时间处理后通知运营商*********");

                    long rt = rightTime*1000L;
                    Date etime = new Date(rt);//权益时间
                    String rtFormat = format.format(etime);
                    String t = rtFormat+" 23:59:59" ;
                    Date date = format2.parse(t);
                    long e = date.getTime()/1000;
                    //新增优酷是点到点，腾讯是一天最后一秒,爱奇艺是一天最后一秒
                    if("youku".equals(cooperatorCode)){
                        requestParam.put("vip_endtime", etime.getTime()/1000);//必填
                    }else{
                        requestParam.put("vip_endtime", e);//必填
                    }
                    if(StringUtils.isBlank(url)){
                        taskLog.info("订单号ext_traceno="+ext_traceno+",请求url没有配置,source="+source);
                        logger.info("订单号ext_traceno="+ext_traceno+",请求url没有配置,source="+source);
                        //告警
                        alarm("syncRightsTime_订单ext_traceno为[" + ext_traceno + "]用户id为[" + vuid+ "]的数据,请求url没有配置,source="+source);
                        return ;
                    }
                    taskLog.info("订单号ext_traceno="+ext_traceno+",进入订单状态回调通知接口,请求url="+url+",请求参数:"+JSON.toJSONString(requestParam));
                    logger.info("订单号ext_traceno="+ext_traceno+",进入订单状态回调通知接口,请求url="+url+",请求参数:"+JSON.toJSONString(requestParam));
                    //订单状态回调通知接口
                    String result = HttpUtils.doPost(url, requestParam);
                    taskLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知接口返回值:"+result);
                    logger.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知接口返回值:"+result);
                    String resultCode = "";
                    if(StringUtils.isNotBlank(result)){
                        JSONObject resultJson = JSON.parseObject(result);
                        resultCode = String.valueOf(resultJson.get("code"));
                    }
                    if(!BizConstant.Code.Result_Code_Success_Num_0.equals(resultCode)){
                        join(data);//重新加入队列
                        taskLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列*********");
                        logger.error("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列*********");
                        dataLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列:"+JSON.toJSONString(data));//通知失败数据记录
                        return ;
                    }
                    dataLog.info("通知成功:"+JSON.toJSONString(data));//通知成功数据记录
                    //更新用户产品权益表数据
                    this.updateMobileUserBySource(ext_traceno,cooperatorCode,etime,source,userid,product_type,vuid);
                }else if(rightTime != null && rightTime > 0 && vipCalculateTime > 0 ){
                    long rt = rightTime*1000L;
                    long vt = vipCalculateTime*1000L ;
                    Date etime = new Date(rt);//权益时间
                    Date vtDate = new Date(vt);//校验时间
                    String rtFormat = format.format(etime);
                    String vtFormat = format.format(vtDate);
                    taskLog.info("订单号ext_traceno="+ext_traceno+",notify_type="+notify_type+",rightTime="+rightTime+",vipCalculateTime="+vipCalculateTime+",rtFormat="+rtFormat+",vtFormat="+vtFormat);
                    logger.info("订单号ext_traceno="+ext_traceno+",notify_type="+notify_type+",rightTime="+rightTime+",vipCalculateTime="+vipCalculateTime+",rtFormat="+rtFormat+",vtFormat="+vtFormat);
                    //notify_type 1.通过重试，腾讯权益发货成功； 2.权益时间修正；不是同一天或者重发的后,需要发送校验并更新数据库
                    if(!rtFormat.equals(vtFormat) || "1".equals(notify_type)){
                        String t = rtFormat+" 23:59:59" ;
                        Date date = format2.parse(t);
                        long e = date.getTime()/1000;
                        if("youku".equals(cooperatorCode)){
                            requestParam.put("vip_endtime", etime.getTime()/1000);//必填
                        }else{
                            requestParam.put("vip_endtime", e);//必填
                        }
                        if(StringUtils.isBlank(url)){
                            taskLog.info("订单号ext_traceno="+ext_traceno+",请求url没有配置,source="+source);
                            logger.info("订单号ext_traceno="+ext_traceno+",请求url没有配置,source="+source);
                            //告警
                            alarm("syncRightsTime_订单ext_traceno为[" + ext_traceno + "]用户id为[" + vuid+ "]的数据,请求url没有配置,source="+source);
                            return ;
                        }
                        taskLog.info("订单号ext_traceno="+ext_traceno+",进入订单状态回调通知接口,请求url="+url+",请求参数:"+JSON.toJSONString(requestParam));
                        logger.info("订单号ext_traceno="+ext_traceno+",进入订单状态回调通知接口,请求url="+url+",请求参数:"+JSON.toJSONString(requestParam));
                        //订单状态回调通知接口
                        String result = HttpUtils.doPost(url, requestParam);

                        taskLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知接口返回值:"+result);
                        logger.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知接口返回值:"+result);
                        String resultCode = "";
                        if(StringUtils.isNotBlank(result)){
                            JSONObject resultJson = JSON.parseObject(result);
                            resultCode = String.valueOf(resultJson.get("code"));
                        }

                        if(!BizConstant.Code.Result_Code_Success_Num_0.equals(resultCode)){
                            join(data);//重新加入队列
                            taskLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列*********");
                            logger.error("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列*********");
                            dataLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列:"+JSON.toJSONString(data));//通知失败数据记录
                            return ;
                        }
                        dataLog.info("通知成功:"+JSON.toJSONString(data));//通知成功数据记录
                    }
                    //优酷权益时间修正就发送请求通知回调
                    if("youku".equals(cooperatorCode) && "2".equals(notify_type)){
                        requestParam.put("vip_endtime", etime.getTime()/1000);//必填
                        if(StringUtils.isBlank(url)){
                            taskLog.info("订单号ext_traceno="+ext_traceno+",请求url没有配置,source="+source);
                            logger.error("订单号ext_traceno="+ext_traceno+",请求url没有配置,source="+source);
                            //告警
                            alarm("syncRightsTime_订单ext_traceno为[" + ext_traceno + "]用户id为[" + vuid+ "]的数据,请求url没有配置,source="+source);
                            return ;
                        }
                        taskLog.info("订单号ext_traceno="+ext_traceno+",进入订单状态回调通知接口,请求url="+url+",请求参数:"+JSON.toJSONString(requestParam));
                        logger.info("订单号ext_traceno="+ext_traceno+",进入订单状态回调通知接口,请求url="+url+",请求参数:"+JSON.toJSONString(requestParam));
                        //订单状态回调通知接口
                        String result = HttpUtils.doPost(url, requestParam);
                        taskLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知接口返回值:"+result);
                        logger.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知接口返回值:"+result);
                        String resultCode = "";
                        if(StringUtils.isNotBlank(result)){
                            JSONObject resultJson = JSON.parseObject(result);
                            resultCode = String.valueOf(resultJson.get("code"));
                        }
                        if(!BizConstant.Code.Result_Code_Success_Num_0.equals(resultCode)){
                            join(data);//重新加入队列
                            taskLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列*********");
                            logger.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列*********");
                            dataLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列:"+JSON.toJSONString(data));//通知失败数据记录
                            return ;
                        }
                        dataLog.info("通知成功:"+JSON.toJSONString(data));//通知成功数据记录
                    }
                    //爱奇艺权益时间修正就发送请求通知回调
                    if("aiqiyi".equals(cooperatorCode) && "2".equals(notify_type)){
                        requestParam.put("vip_endtime", etime.getTime()/1000);//必填
                        if(StringUtils.isBlank(url)){
                            taskLog.info("订单号ext_traceno="+ext_traceno+",请求url没有配置,source="+source);
                            logger.info("订单号ext_traceno="+ext_traceno+",请求url没有配置,source="+source);
                            //告警
                            alarm("syncRightsTime_订单ext_traceno为[" + ext_traceno + "]用户id为[" + vuid+ "]的数据,请求url没有配置,source="+source);
                            return ;
                        }
                        taskLog.info("订单号ext_traceno="+ext_traceno+",进入订单状态回调通知接口,请求url="+url+",请求参数:"+JSON.toJSONString(requestParam));
                        logger.info("订单号ext_traceno="+ext_traceno+",进入订单状态回调通知接口,请求url="+url+",请求参数:"+JSON.toJSONString(requestParam));
                        //订单状态回调通知接口
                        String result = HttpUtils.doPost(url, requestParam);
                        taskLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知接口返回值:"+result);
                        logger.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知接口返回值:"+result);
                        String resultCode = "";
                        if(StringUtils.isNotBlank(result)){
                            JSONObject resultJson = JSON.parseObject(result);
                            resultCode = String.valueOf(resultJson.get("code"));
                        }
                        if(!BizConstant.Code.Result_Code_Success_Num_0.equals(resultCode)){
                            join(data);//重新加入队列
                            taskLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列*********");
                            logger.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列*********");
                            dataLog.info("订单号ext_traceno="+ext_traceno+",订单状态回调通知失败,重新加入队列:"+JSON.toJSONString(data));//通知失败数据记录
                            return ;
                        }
                        dataLog.info("通知成功:"+JSON.toJSONString(data));//通知成功数据记录

                    }
                    logger.info("etime-->>>"+etime);
                    //更新用户产品权益表数据
                    this.updateMobileUserBySource(ext_traceno,cooperatorCode,etime,source,userid,product_type,vuid);
                }else {
                    taskLog.info("订单号ext_traceno="+ext_traceno+",获取会员权益成功，但权益到期时间(end值)为空!");
                    logger.info("订单号ext_traceno="+ext_traceno+",获取会员权益成功，但权益到期时间(end值)为空!");
                    dataLog.info("订单号ext_traceno="+ext_traceno+",获取会员权益成功，但权益到期时间(end值)为空!"+JSON.toJSONString(data));
                }
            }else{
                join(data);//重新加入队列
                taskLog.info("订单号ext_traceno="+ext_traceno+",获取会员权益失败,重新加入队列*********");
                logger.info("订单号ext_traceno="+ext_traceno+",获取会员权益失败,重新加入队列*********");
                dataLog.info("获取会员权益失败,重新加入队列:"+JSON.toJSONString(data));//获取会员权益失败数据记录
            }
        } catch (Exception e) {
            join(data);//重新加入队列
            taskLog.info("订单号ext_traceno="+ext_traceno+",SyncRightsTimer报错,重新加入队列:"+e.getMessage());
            dataLog.info("订单号ext_traceno="+ext_traceno+",SyncRightsTimer报错,重新加入队列:"+JSON.toJSONString(data));
            logger.error("订单号ext_traceno="+ext_traceno+",SyncRightsTimer报错,重新加入队列"+e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 数据重新加入redis队列
     * @param req
     */
    public void join(JSONObject req){
        try {
            req.put("nums", req.getIntValue("nums") + 1);
            // 将数据放到redis集合
            long time = 1*60*1000;
            Date afterDate = new Date(new Date().getTime() + time);
            redisCache.zAdd("SyncRightsTimer_SortSet",afterDate.getTime(),JSONObject.toJSONString(req));
        } catch (Exception e) {
            taskLog.info("....重新加入队列报错:"+JSON.toJSONString(req));
            logger.error("....重新加入队列报错:"+JSON.toJSONString(req)+"*******"+e.getMessage());
            dataLog.info("....重新加入队列报错:"+JSON.toJSONString(req));
            e.printStackTrace();
        }
    }



    /**
     * 获取会员信息
     * @param req
     * @return
     */
    public JSONObject vipInfo(JSONObject req) {
        JSONObject resp=new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");
        resp.put("vuid", req.getString("vuid"));
        String cooperatorCode = req.getString("cooperatorCode");
        if(StringUtils.isNotBlank(cooperatorCode)&&"youku".equals(cooperatorCode)){
            resp = getYoukuVipInfo(req);
        } else if (StringUtils.isNotBlank(cooperatorCode)&&"aiqiyi".equals(cooperatorCode)) {
            resp = getAiqiyiVipInfo(req);
        } else {
            try {
                Map<String ,Object> tx_req=new HashMap<String ,Object>();
                tx_req.put("version", "2.0");
                tx_req.put("format", "json");
                tx_req.put("user_type", "0");
                tx_req.put("vuserid", req.getString("vuid"));
                tx_req.put("vendor_platform", sysConfig.getVENDOR_PLATFORM());
                String url = sysConfig.getTX_VIP_INFO_URL();
                if(StringUtils.isBlank(url)){
                    logger.error("SyncRightsTimerTask定时任务获取腾讯token参数TX_VIP_INFO_URL暂未配置");
                }
                String result;
                //获取accesstoken
                String accesstoken=mobileInterService.getAccessToken();
                tx_req.put("access_token", accesstoken);
                taskLog.info("*******请求腾讯获取会员信息参数:url="+url+",param=" + JSON.toJSONString(tx_req));
                logger.info("*******请求腾讯获取会员信息参数:url="+url+",param=" + JSON.toJSONString(tx_req));
                result=HttpUtils.doGet(url, tx_req);
                taskLog.info("*******请求腾讯获取会员信息返回:" + result);
                logger.info("*******请求腾讯获取会员信息返回:" + result);

                //获取产品类型
                String product_type = req.getString("product_type");
                //默认查询115增值包权益
                Integer vipBidInt = 115;
                String vipBidStr = Vip_BidMap.get(product_type);
                if(null == vipBidStr || "".equals(vipBidStr)){
                    logger.error("查询vuid为:"+req.getString("vuid")+"的腾讯权益出错。产品类型为:"+product_type+"暂未配置对应vipBid值,查询默认115权益");
                }else{
                    vipBidInt = Integer.valueOf(vipBidStr);
                }

                if (StringUtils.isBlank(result)) {
                    logger.error("请求腾讯获取会员信息接口返回为空");
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "请求腾讯获取会员信息接口返回为空");
                    return resp;
                }

                JSONObject result_json=JSONObject.parseObject(result);
                JSONObject resultcode=JSONObject.parseObject(result_json.getString("result"));

                if (BizConstant.Code.Result_Code_Success_Num_0.equals(resultcode.getString("code"))) {
                    JSONObject data = JSONObject.parseObject(result_json.getString("data"));
                    JSONArray vipInfos = data.getJSONArray("vipInfos");
                    if(null!=vipInfos&&vipInfos.size()>0){
                        for (int i = 0; i < vipInfos.size(); i++) {
                            JSONObject job = vipInfos.getJSONObject(i);
                            //if(BizConstant.Tencent.VipInfo.vip_bid_cinema==job.getInteger("vip_bid")){
                            //if(Vip_Bid.intValue() == job.getInteger("vip_bid")){
                            if(vipBidInt == job.getInteger("vip_bid")){
                                resp.put("is_vip",job.getInteger("vip") );
                                resp.put("start", job.getInteger("start"));
                                resp.put("end",job.getInteger("end") );
                                resp.put("vip_bid",job.getInteger("vip_bid"));
                            }
                        }
                    }else{
                        //0 非会员
                        resp.put("is_vip",0 );
                    }

                }else{
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "请求腾讯获取会员信息接口返回失败");
                    return resp;
                }
            } catch (Exception e) {
                logger.error("请求腾讯获取会员信息出错!" + e.getCause(), e);
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求腾讯获取会员信息出错!");
                return resp;
            }
        }
        return resp;
    }

    /**
     * 查询优酷会员权益信息
     * @param req
     * @return
     */
    private JSONObject getYoukuVipInfo(JSONObject req) {
        JSONObject resp = new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");
        try{
            Object vuid = req.get("vuid");
            String source = req.get("source")+"";
            Map<String, Object> requestParams = new HashMap<String, Object>();
            String channelIdBySource = youKuRequstUtils.getChannelIdBySource(source);
            requestParams.put("thirdPartId", vuid);
            requestParams.put("pid", channelIdBySource);
            taskLog.info("*******请求优酷获取会员信息参数:" + JSON.toJSONString(requestParams));
            logger.info("*******请求优酷获取会员信息参数:" + JSON.toJSONString(requestParams));
            ServerResponse doRequest = youKuRequstUtils.doRequest("ott.operator.order.query", requestParams);
            String bizParam = JSON.toJSONString(doRequest);
            taskLog.info("*******请求优酷获取会员信息返回:" + bizParam);
            logger.info("*******请求优酷获取会员信息返回:" + bizParam);
            int status = doRequest.getStatus();
            if (0 == status) {
                Object data_obj = doRequest.getData();
                JSONObject data = (JSONObject) JSON.parse(data_obj.toString());
                Object code = data.get("code");
                if ("10000".equals(code)) {
                    JSONObject bizResp = (JSONObject) data.get("bizResp");
                    JSONObject model = (JSONObject) bizResp.get("model");
                    Object object_currentRight = model.get("currentRight");
                    if(object_currentRight!=null){
                        JSONObject currentRight = (JSONObject) object_currentRight;
                        Long startTime = Long.valueOf(currentRight.get("startTime").toString());
                        Long endTime = Long.valueOf(currentRight.get("endTime").toString());
                        String vip = currentRight.get("vip")+"";
                        if("true".equals(vip)){
                            resp.put("is_vip",1 );
                        }else{
                            resp.put("is_vip",0 );
                        }
                        resp.put("start", startTime/1000);
                        resp.put("end",endTime/1000);
                    }else{
                        resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                        resp.put("msg", "请求优酷会员信息返回为空!");
                    }
                }else{
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "请求优酷会员信息返回为空!");
                }
            }else {
                logger.error("请求优酷会员信息返回错误-----" + bizParam);
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求优酷会员信息返回为空!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("请求优酷ott.operator.order.query接口出错-----" + e.getMessage());
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求优酷会员信息出错!");
        }
        return resp ;
    }




    /**
     *查询爱奇艺会员权益信息
     * @param req
     * @return
     */
    private JSONObject getAiqiyiVipInfo(JSONObject req) {
        JSONObject resp = new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");
        //组装参数请求爱奇艺
        TreeMap<String, Object> aiqiyi_req = new TreeMap<String, Object>();
        String reqId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        aiqiyi_req.put("version",sysConfig.getAIQIYI_GET_VIP_INFO_VERSION());
        aiqiyi_req.put("reqId", reqId);
        aiqiyi_req.put("partner_no",sysConfig.getAIQIYI_PARTNER_NO());
        aiqiyi_req.put("open_id", req.get("vuid"));

        String sign = AiqiyiAuthenticationUtil.authentication(aiqiyi_req);
        sign = StringUtils.lowerCase(sign);
        aiqiyi_req.put("sign", sign);
        String url = sysConfig.getAIQIYI_VIP_INFO_URL();
        if(StringUtils.isBlank(url)){
            logger.error("爱奇艺获取会员权益AIQIYI_VIP_INFO_URL暂未配置");
        }
        String result;
        JSONObject aiqiyi_req_json = new JSONObject(aiqiyi_req);

        try {
            taskLog.info("*******请求爱奇艺获取会员信息参数:" + JSON.toJSONString(aiqiyi_req_json));
            logger.info("*******请求爱奇艺获取会员信息参数:" + JSON.toJSONString(aiqiyi_req_json));
            result = HttpUtils.doPost(url, aiqiyi_req_json);
            taskLog.info("*******请求爱奇艺获取会员信息返回:" + result);
            logger.info("*******请求爱奇艺获取会员信息返回:" + result);
        } catch (Exception e) {
            logger.error("请求爱奇艺获取会员信息出错!" + e.getCause(), e);
            taskLog.error("请求爱奇艺获取会员信息出错!" + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求爱奇艺获取会员信息出错!");
            return resp;
        }

        if (StringUtils.isBlank(result)) {
            logger.error("请求爱奇艺获取会员信息接口返回为空");
            taskLog.error("请求爱奇艺获取会员信息接口返回为空");
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求爱奇艺获取会员信息接口返回为空");
            return resp;
        }
        JSONObject result_json = JSONObject.parseObject(result);
        String resultcode = result_json.getString("code");
        if (BizConstant.Aiqiyi.SuccessCode.equals(resultcode)) {
            //获取会员信息成功
            JSONArray data = result_json.getJSONArray("data");
            String endStr = "";
            String startStr = "";
            if (!data.isEmpty() || data.size() > 0) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject job = data.getJSONObject(i);
                    endStr = job.getString("endTime");
                    startStr = job.getString("startTime");
                }
            }
            Date endDate = DateUtil.StringToDate(endStr,"yyyy-MM-dd HH:mm:ss");
            Date startDate = DateUtil.StringToDate(startStr, "yyyy-MM-dd HH:mm:ss");
            long endDateTime = endDate.getTime();
            long startDateTime = startDate.getTime();
            long now = System.currentTimeMillis();
            if (startDateTime < now && now < endDateTime) {
                //是vip
                resp.put("is_vip",1);
                resp.put("start", startDateTime/1000);
                resp.put("end", endDateTime/1000);
            } else {
                //还没到开通时间，不是vip
                resp.put("start", startDateTime/1000);
                resp.put("end", endDateTime/1000);
                resp.put("is_vip", 0);
            }

        } else if (BizConstant.Aiqiyi.NoRightsCode.equals(resultcode)) {
            resp.put("is_vip", 0);
        } else if (BizConstant.Aiqiyi.SignatureErrorCode.equals(resultcode)) {
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求爱奇艺获取会员信息签名错误");
        } else {
            //获取会员信息失败
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求爱奇艺获取会员信息接口返回失败");
            return resp;
        }
        return resp;
    }


    /**
     * 更新用户产品权益表数据
     * @param ext_traceno 订单唯一标识
     * @param cooperatorCode 渠道
     * @param etime   结束时间
     * @param source 来源
     * @param userid userid
     * @param product_type 产品类型
     * @param vuid vuid
     */
    public void updateMobileUserBySource(String ext_traceno,String cooperatorCode,Date etime,String source,String userid,String product_type,String vuid){
        if("youku".equals(cooperatorCode)){
            int count = mobileUserProductYoukuService.updateMobileUserProductYouku(etime, source, userid, product_type, vuid);
            taskLog.info("订单号ext_traceno="+ext_traceno+",更新优酷用户产品权益表********更新条数count:"+count+",更新参数为etime="+etime+",source="+source+",userid="+userid+",product_type="+product_type+",vuid="+vuid);
            logger.info("订单号ext_traceno="+ext_traceno+",更新优酷用户产品权益表********更新条数count:"+count+",更新参数为etime="+etime+",source="+source+",userid="+userid+",product_type="+product_type+",vuid="+vuid);
        } else if ("aiqiyi".equals(cooperatorCode)) {//爱奇艺
            int count = mobileUserProductAiqiyiService.updateMobileUserProductaiqiyi(etime, source, userid, product_type, vuid);
            taskLog.info("订单号ext_traceno="+ext_traceno+",更新爱奇艺用户产品权益表********更新条数count:"+count+",更新参数为etime="+etime+",source="+source+",userid="+userid+",product_type="+product_type+",vuid="+vuid);
            logger.info("订单号ext_traceno="+ext_traceno+",更新爱奇艺用户产品权益表********更新条数count:"+count+",更新参数为etime="+etime+",source="+source+",userid="+userid+",product_type="+product_type+",vuid="+vuid);
        } else {//腾讯
            int count = mobileUserProductService.updateMobileUserProduct(etime, source, userid, product_type, vuid);
            taskLog.info("订单号ext_traceno="+ext_traceno+",更新腾讯用户产品权益表********更新条数count:"+count+",更新参数为etime="+etime+",source="+source+",userid="+userid+",product_type="+product_type+",vuid="+vuid);
            logger.info("订单号ext_traceno="+ext_traceno+",更新腾讯用户产品权益表********更新条数count:"+count+",更新参数为etime="+etime+",source="+source+",userid="+userid+",product_type="+product_type+",vuid="+vuid);
        }
    }

    /**
     * 告警
     * @param msg
     */
    private void alarm(String msg){
        incidentPushServcie.incidentPush(msg, BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
    }




    //@Scheduled(cron="${SyncRightsTimerTask.corn}")
    public void test(){
        logger.info("任务开始1");
        System.out.println(Vip_BidMap);
    }
}
