package iptv.modules.tx.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import iptv.common.BusinessException;
import iptv.config.redis.RedisCache;
import iptv.modules.base.service.impl.MobileTokenLogService;
import iptv.modules.base.service.impl.SnmTokenLogService;
import iptv.modules.tx.entity.db.MobileUserInfo;
import iptv.modules.tx.entity.vo.AccessToken;
import iptv.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import java.text.SimpleDateFormat;
import java.util.Random;

/**
 * SyncRightsTimerTask定时任务获取腾讯token并放到redis中
 */
@Component
public class MobileInterService {

    private static Logger log = LoggerFactory.getLogger(MobileInterService.class);
    private static java.util.logging.Logger logHttpErr = java.util.logging.Logger.getLogger("getVuidErrLogger");
    private static final java.util.logging.Logger logViud = java.util.logging.Logger.getLogger("httpUtilsLogger");

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private SnmTokenLogService snmTokenLogService;

    @Autowired
    private MobileTokenLogService mobileTokenLogService;

    @Autowired
    private SysConfig sysConfig;
    @Autowired
    private MobileUserInfoServiceImpl mobelUserInfoService;

    @Autowired
    private HttpIncident httpIncident;

    private static SimpleDateFormat httpUtilsdateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

    /**
     * 获取access_token
     * zc add
     * @return
     * @throws Exception
     */
    public String getAccessToken() throws Exception {

        String accessToken="";
        try {
            accessToken = redisCache.getCache("TX_ACCESS_TOKEN", String.class);
        } catch (Exception e) {
            //			//方案1 直接从腾讯获取
            //			log.error("从redis获取token出错,直接到腾讯拿："+e.getCause(),e);
            //			//从腾讯获取token
            //			accessToken = getAccessTokenFromTx();
            //			//设置token的有效时长并存在redis中
            //			resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN",accessToken);
            //			return accessToken;

            //方案2 从本地缓存取
            log.error("从redis获取token出错，现转为从本地缓存取！！！"+e.getCause(),e);
            accessToken=getAccessTokenWithFile();
            return accessToken;
        }
        if (StringUtils.isBlank(accessToken)) {
            // 双重检测机制
            synchronized (MobileInterService.class) {
                // 重新检验是否获取到锁
                accessToken = redisCache.getCache("TX_ACCESS_TOKEN", String.class);
                if (StringUtils.isNotBlank(accessToken)) {
                    return accessToken;
                }
                //从腾讯获取token
                accessToken = getAccessTokenFromTx();
                //设置token的有效时长并存在redis中
                resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN",accessToken);

                //记录token日志
                snmTokenLogService.saveSnmTokenLog(accessToken,null);

                //现在发现旧的token和新的token都能同时用，所以暂时不需要加分布式锁，如果不能用，再放开注释
                //				// 分布式锁
                //				boolean accessTokenLock = redisCache.setnx("TX_ACCESS_TOKEN_LOCK", "lock");
                //				if (accessTokenLock) {
                //					try {
                //						// 成功拿到锁之后 ，给锁加个超时时间，防止突然中断服务，导致锁不能释放，默认10秒失效
                //						redisCache.putCacheWithExpireTime("TX_ACCESS_TOKEN_LOCK", "lock", 10);
                //						// log.info("[resetAndGetAccessToken
                //						// start]:"+dateFormat.format(new Date()));
                //						accessToken = resetAndGetAccessToken();
                //						// log.info("[resetAndGetAccessToken
                //						// end]:"+dateFormat.format(new Date()));
                //					} catch (Exception e) {
                //						log.error("【getAccessToken】重新获取token出错：" + e.getCause(), e);
                //					} finally {
                //						redisCache.del("TX_ACCESS_TOKEN_LOCK");
                //					}
                //				} else {
                //					// 别的机器正在获取token,稍等3秒，重新拿redis的token值
                //					Thread.sleep(3000);
                //					// 重新检验是否获取到锁
                //					accessToken = redisCache.getCache("TX_ACCESS_TOKEN", String.class);
                //					return accessToken;
                //				}
            }
        }
        return accessToken;
    }

    /**
     * 获取access_token 和token的有效时长
     *
     * @return
     * @throws Exception
     */
    public JSONObject getAccessTokenWithExpireTime(JSONObject req) throws Exception {

        JSONObject resp = new JSONObject();
        // 获取token
        String accessToken = getAccessToken();
        // 获取有效时长
        Long expireTime = redisCache.getKeyExpireTime("TX_ACCESS_TOKEN");

        resp.put("accessToken", accessToken);
        resp.put("expireTime", expireTime);

        return resp;
    }

    /**
     * access_token失效，重新获取
     *
     * @return
     */
    public String getAccessTokenFromTx() throws Exception {

        String url = sysConfig.getTX_ACCESS_TOKEN_URL();
        String appid = sysConfig.getTX_APPID();
        String appkey = sysConfig.getTX_APPKEY();
        if (StringUtils.isBlank(url) || StringUtils.isBlank(appid) || StringUtils.isBlank(appkey)) {
            log.error("请求腾讯获取access_token时，请求配置缺少！");
            throw new Exception("请求腾讯获取access_token时，请求配置缺少！");
        }

        String result = HttpUtils.doGet(url + "&appid=" + appid + "&appkey=" + appkey);
        if (StringUtils.isBlank(result)) {
            log.error("请求腾讯获取access_token返回为空！");
            throw new Exception("请求腾讯获取access_token返回为空！");
        }
        JSONObject json = JSONObject.parseObject(result);
        JSONObject json_result = JSONObject.parseObject(json.getString("result"));
        if (!BizConstant.Code.Result_Code_Success_Num_0.equals(json_result.getString("code"))) {
            // 返回报错
            log.error("请求腾讯获取access_token返回报错：" + json_result.getString("code") + ":" + json_result.getString("msg"));
            throw new Exception("请求腾讯获取access_token返回报错：" + json_result.getString("code") + ":" + json_result.getString("msg"));
        }
        JSONObject data = JSONObject.parseObject(json.getString("data"));
        String accessToken = data.getString("access_token");
        return accessToken;
    }


    /**
     * 设置token 的超时时间到redis
     * @param tokenName
     * @param tokenValue
     */
    public void resetTokenExpireTimeByRedis(String tokenName,String tokenValue){
        try {
            String expireTime = sysConfig.getTX_ACCESS_TOKEN_EXPIRETIME();
            if (StringUtils.isBlank(expireTime)) {
                //默认失效时长为1个半小时，单位为秒
                expireTime = "5400";
            }
            //设置token的有效时长并存在本地缓存中
            if("TX_ACCESS_TOKEN".equals(tokenName)){
                AccessToken.setAccesstoken(tokenValue);
                //设置本地token
                long time = Long.valueOf(expireTime) * 1000;
                Date expire_time = new Date(new Date().getTime() + time);
                AccessToken.setExpiretime(expire_time);
                AccessToken.setUpdateTime(new Date());
            }

            redisCache.putCacheWithExpireTime(tokenName, tokenValue, Long.valueOf(expireTime));
        } catch (Exception e) {
            log.error("设置token 的超时时间到redis出错："+e.getCause(),e);
        }
    }


    public String getAccessTokenWithFile() throws Exception {

        //先判断本地缓存里面是否有
        if(!isAccessTokenValid()){
            // 双重检测机制
            synchronized (MobileInterService.class) {
                if(!isAccessTokenValid()){
                    //不存在，重新获取
                    reGetAccessTokenWithFile();
                }
            }
        }

        return AccessToken.getAccesstoken();
    }

    public boolean isAccessTokenValid() throws Exception{
        boolean flag=true;
        if(StringUtils.isBlank(AccessToken.getAccesstoken())){
            //不存在
            return false;
        }
        if(null==AccessToken.getExpiretime()||DateTimeUtil.is_A_Before_B(AccessToken.getExpiretime(), new Date())){
            //token的有效时间小于当前时间
            return false;
        }
        return flag;
    }

    public void reGetAccessTokenWithFile() throws Exception{
        //重新获取token
        String accessToken=getAccessTokenFromTx();
        AccessToken.setAccesstoken(accessToken);
        //设置token的超时时间
        String TX_ACCESS_TOKEN_EXPIRETIME = sysConfig.getTX_ACCESS_TOKEN_EXPIRETIME();
        if (StringUtils.isBlank(TX_ACCESS_TOKEN_EXPIRETIME)) {
            //默认失效时长为1个半小时，单位为秒
            TX_ACCESS_TOKEN_EXPIRETIME = "5400";
        }
        long time = Long.valueOf(TX_ACCESS_TOKEN_EXPIRETIME) * 1000;

        Date expiretime = new Date(new Date().getTime() + time);
        AccessToken.setExpiretime(expiretime);
    }


    /**
     * 同步token给成思时向腾讯获取
     *
     * @return
     */
    public JSONObject getAccessTokenFromTxForMobile(String source, String third_url, Logger logger, String msg) throws Exception {

        JSONObject resp=new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "成功");

        //向腾讯获取
        String url = sysConfig.getTX_ACCESS_TOKEN_URL();
        String appid = sysConfig.getTX_APPID();
        String appkey = sysConfig.getTX_APPKEY();
        if (StringUtils.isBlank(url) || StringUtils.isBlank(appid) || StringUtils.isBlank(appkey)) {
            logger.error("请求腾讯获取access_token时，请求配置缺少,请检查参数配置！");
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求腾讯获取access_token时，请求配置缺少,请检查参数配置！");
            return resp;
        }

        String result="";
        try {
            result = HttpUtils.doGet(url + "&appid=" + appid + "&appkey=" + appkey,logger);
        } catch (Exception e1) {
            httpIncident.incidentPush_ONCE(BizConstant.IncidentPush.httpUtilsdateFormat.format(new Date())+"---syncTokenToMobileTask请求腾讯获取access_token出错！"+e1.getCause(), BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            logger.error("请求腾讯获取token出错："+e1.getCause(),e1);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求腾讯获取token出错："+e1.getCause());
            return resp;
        }

        if (StringUtils.isBlank(result)) {
            httpIncident.incidentPush_ONCE(BizConstant.IncidentPush.httpUtilsdateFormat.format(new Date())+"---syncTokenToMobileTask请求腾讯获取access_token返回为空！", BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            logger.error("请求腾讯获取access_token返回为空！");
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求腾讯获取access_token返回为空！");
            return resp;
        }
        JSONObject json = JSONObject.parseObject(result);
        JSONObject json_result = JSONObject.parseObject(json.getString("result"));
        if (!BizConstant.Code.Result_Code_Success_Num_0.equals(json_result.getString("code"))) {
            // 返回报错
            httpIncident.incidentPush_ONCE(BizConstant.IncidentPush.httpUtilsdateFormat.format(new Date())+"---syncTokenToMobileTask请求腾讯获取access_token返回报错：" + result, BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            logger.error("请求腾讯获取access_token返回报错：" + json_result.getString("code") + ":" + json_result.getString("msg"));
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求腾讯获取access_token返回报错：" + json_result.getString("code") + ":" + json_result.getString("msg"));
            return resp;
        }
        JSONObject data = JSONObject.parseObject(json.getString("data"));
        String accessToken = data.getString("access_token");
        if(StringUtils.isBlank(accessToken)){
            // 返回报错
            httpIncident.incidentPush_ONCE(BizConstant.IncidentPush.httpUtilsdateFormat.format(new Date())+"---syncTokenToMobileTask请求腾讯获取access_token返回成功，但access_token字段为空！" +result, BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            logger.error("请求腾讯获取access_token返回成功，但access_token字段为空！" );
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求腾讯获取access_token返回成功，但access_token字段为空！");
            return resp;
        }

        if (StringUtils.isBlank(third_url)) {
            logger.error("渠道"+source+"未配置同步token给第三方的请求url！");
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "渠道"+source+"未配置同步token给第三方的请求url！");
            return resp;
        }
        JSONObject req = new JSONObject();
        req.put("Source", source);
        req.put("AccessToken", accessToken);
        String mobile_result="";
        try {
            mobile_result=HttpUtils.doPost(third_url, req, logger);
        } catch (Exception e) {
            httpIncident.incidentPush_ONCE(BizConstant.IncidentPush.httpUtilsdateFormat.format(new Date())+"---syncTokenToMobileTask同步token给渠道"+source+"--url为:"+third_url+"出错："+e.getCause(), BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            logger.error("同步token给渠道"+source+"出错："+e.getCause(),e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "同步token给渠道"+source+"出错："+e.getCause());
            return resp;
        }

        if(StringUtils.isBlank(mobile_result)){
            httpIncident.incidentPush_ONCE(BizConstant.IncidentPush.httpUtilsdateFormat.format(new Date())+"---syncTokenToMobileTask同步token给渠道"+source+"--url为:"+third_url+"返回为空！", BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            logger.error("同步token给渠道"+source+"返回为空！");
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "同步token给渠道"+source+"返回为空！");
            return resp;
        }

        //记录token日志
        mobileTokenLogService.saveMobileTokenLog(source,accessToken,msg);

        return resp;
    }

    /**
     * 定时更新本地腾讯的 asscesstoken
     * @throws Exception
     */
    public JSONObject updateLocalTxToken(String msg) throws Exception{

        JSONObject result=new JSONObject();
        result.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        result.put("msg", "成功");

        //从腾讯获取token
        String accessToken = this.getAccessTokenFromTx();
        if(StringUtils.isBlank(accessToken)){
            httpIncident.incidentPush_ONCE(BizConstant.IncidentPush.httpUtilsdateFormat.format(new Date())+"---UpdateLocalTxTokenTask请求腾讯获取access_token返回为空！", BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            throw new Exception("【updateLocalTxToken】调用腾讯获取token失败");
        }

        result.put("token", accessToken);

        //设置token的有效时长并存在本地缓存中
        AccessToken.setAccesstoken(accessToken);

        //获取token失效时间配置
        String TX_ACCESS_TOKEN_EXPIRETIME = sysConfig.getTX_ACCESS_TOKEN_EXPIRETIME();
        if (StringUtils.isBlank(TX_ACCESS_TOKEN_EXPIRETIME)) {
            //默认失效时长为1个半小时，单位为秒
            TX_ACCESS_TOKEN_EXPIRETIME = "5400";
        }

        //设置本地token
        long time = Long.valueOf(TX_ACCESS_TOKEN_EXPIRETIME) * 1000;
        Date expiretime = new Date(new Date().getTime() + time);
        AccessToken.setExpiretime(expiretime);

        //设置redis-token
        try {
            redisCache.putCacheWithExpireTime("TX_ACCESS_TOKEN", accessToken, Long.valueOf(TX_ACCESS_TOKEN_EXPIRETIME));
        } catch (Exception e) {
            log.error("记录本地token到redis出错:"+e.getCause(),e);
            result.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            result.put("msg", "记录本地token到redis出错:"+e.getCause());
            httpIncident.incidentPush_ONCE(BizConstant.IncidentPush.httpUtilsdateFormat.format(new Date())+"---UpdateLocalTxTokenTask记录本地token到redis出错！"+e.getCause(), BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            return result;
        }

        //记录token日志
        snmTokenLogService.saveSnmTokenLog(accessToken,msg);

        return result;

    }

    /**
     * 生成本地订单流水号
     * @return
     */
    public String getLocalTraceno() {
        String hashCodeStr = RandomCode.getUUID().hashCode() + "";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String dateStr = sdf.format(new Date());
        Random r = new Random();
        int rand = r.nextInt(90) + 10;

        String noStr = "MB" + dateStr + rand;

        hashCodeStr = hashCodeStr.replace("-", "");
        int dif = 28 - noStr.length();
        int hashCodeLen = hashCodeStr.length();
        if (dif < hashCodeLen) {

            hashCodeStr = hashCodeStr.substring(0, dif);

        } else if (dif >= hashCodeLen) {

            int size = dif - hashCodeLen;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                sb.append("0");
            }
            hashCodeStr = sb.toString() + hashCodeStr;

        }
        noStr = noStr + hashCodeStr;

        return noStr;
    }
}
