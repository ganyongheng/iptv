package iptv.modules.tx.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import iptv.common.BossException;
import iptv.common.BusinessException;
import iptv.config.redis.RedisCache;
import iptv.modules.base.factory.confirmordersingle.ConfirmOrderSingleProcessFactory;
import iptv.modules.base.process.ConfirmOrderSingleProcess;
import iptv.modules.base.process.VipInfoProcess;
import iptv.modules.tx.factory.BaseBusinessFactory;
import iptv.modules.tx.factory.getaccesstoken.GetAccessTokenProcessFactory;
import iptv.modules.tx.factory.getvuid.GetVuidProcessFactory;
import iptv.modules.tx.process.GetAccessTokenProcess;
import iptv.modules.tx.process.GetVuidProcess;
import iptv.modules.base.factory.vipinfo.VipInfoProcessFactory;
import iptv.util.BizConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import iptv.util.DateTimeUtil;
import iptv.util.DateUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Author wyy
 * Date 2022/3/16 17:01
 **/
@Component
public class BaseBusiness implements InitializingBean {

    private static Logger log = LoggerFactory.getLogger(BaseBusiness.class);

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private GetAccessTokenProcessFactory getAccessTokenProcessFactory;

    @Autowired
    private GetVuidProcessFactory getVuidProcessFactory;

    @Autowired
    private VipInfoProcessFactory vipInfoProcessFactory;

    @Autowired
    private ConfirmOrderSingleProcessFactory confirmOrderSingleProcessFactory;


    public String getBaseAccessToken(JSONObject req) {
        JSONObject resp = new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");
        try {
            String cooperatorCode = req.get("cooperatorCode").toString();
            GetAccessTokenProcess getAccessTokenProcess = getAccessTokenProcessFactory.creatBaseAction(cooperatorCode);
            //获取token
            String accessToken = getAccessTokenProcess.getAccessTokenFromTx();
            resp.put("accessToken", accessToken);
        } catch (Exception e) {
            log.error("【getAccessToken】查询AccessToken出错：" + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Systemrror);
            resp.put("msg", "【getAccessToken】查询AccessToken出错：" + e.getMessage());
            return JSONObject.toJSONString(resp);
        }
        return JSONObject.toJSONString(resp);
    }

    /**
     * 查询会员权益
     *
     * @param req
     * @return
     */
    public String vipInfo(JSONObject req) {
        JSONObject resp = new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");
        try {
            String cooperatorCode = req.get("cooperatorCode").toString();
            VipInfoProcess vipInfoProcess = vipInfoProcessFactory.creatBaseAction(cooperatorCode);
            //校验参数
            vipInfoProcess.checkBaseReparam(req);
            //请求查询权益时间
            resp = vipInfoProcess.queryTime(req);

            //如果腾讯发货成功，返回最后一秒
            if (resp.getLong("end") != null && BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)) {
                Long eTime = resp.getLong("end");
                eTime = DateUtil.getEndOfDate(eTime*1000);
                resp.put("end", eTime);
            }
        } catch (BusinessException e) {
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "【vipinfo】查询会员权益出错：" + e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【getAccessToken】查询AccessToken出错：" + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "【vipinfo】查询会员权益出错");
            return JSONObject.toJSONString(resp);
        }
        return JSONObject.toJSONString(resp);
    }

    /**
     * 支付接入 该接口用于移动用户在购买腾讯侧的产品包后，移动平台借助snm_boss平台告知腾讯进行发货。
     *
     * @param req
     * @return
     */
    public String confirmOrderSingle(JSONObject req) {
        JSONObject resp = new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");

        try {
            String cooperatorCode = req.get("cooperatorCode").toString();
            //创建具体类
            ConfirmOrderSingleProcess confirmOrderSingleProcess = confirmOrderSingleProcessFactory.creatBaseAction(cooperatorCode);
            //校验参数
            confirmOrderSingleProcess.checkBaseReparam(req);
            //发货具体逻辑
            resp = confirmOrderSingleProcess.confirmOrderSingle(req, resp);
            //如果腾讯发货成功，返回最后一秒
            if (resp.getLong("vip_endtime") != null && BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)) {
                Long eTime = resp.getLong("vip_endtime");
                eTime = DateUtil.getEndOfDate(eTime*1000);
                resp.put("vip_endtime", eTime);
            }
        } catch (BusinessException e) {
            //log
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            //log.error("【getAccessToken】查询AccessToken出错：" + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
            resp.put("msg", "订单发货成功");
            resp.put("order_id", req.getString("ext_traceno"));
            resp.put("vuid", req.getString("vuid"));
            resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
            resp.put("vip_endtime", System.currentTimeMillis() / 1000 + 31 * 24 * 60 * 60);
            return JSONObject.toJSONString(resp);
        }
        return JSONObject.toJSONString(resp);
    }


    public String getBaseVuid(JSONObject req) {
        Map<String, String> mapRetuen = new HashMap<>();
        String userId = req.getString("userId");
        mapRetuen.put("userId", userId);
        boolean lock = false;
        try {
            GetVuidProcess getVuidProcess = getVuidProcessFactory.creatBaseAction(req.getString("cooperatorCode"));
            //校验公共必传参数
            getVuidProcess.checkBaseReparam(req);
            //获取vuid
            getVuidProcess.getVuidFromThird(req, mapRetuen, lock);
        } catch (BusinessException b) {
            mapRetuen.put("code", "1");
            mapRetuen.put("msg", b.getMessage());
        } catch (BossException bo) {
            // 如果redis不能链接就不要加锁
            log.error("【MobileUserInterController】其他错误：" + bo.getMessage(), bo);
            mapRetuen.put("code", "1");
            mapRetuen.put("msg", "redis连接失败");
        } catch (Exception e) {
            log.error("【MobileUserInterController】其他错误：" + e.getMessage(), e);
            mapRetuen.put("code", "1");
            mapRetuen.put("msg", "其他错误");
        } finally {
            log.info("返回结果为：" + JSON.toJSONString(mapRetuen));
            try {
                if(lock){
                    redisCache.del("MobileUserGetvuidLock_" + userId);
                }
            } catch (Exception e) {
                log.error("用户id=" + userId + "在获取vuid时解锁出错：" + e.getCause(), e);
            }
            return JSON.toJSONString(mapRetuen);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        BaseBusinessFactory.create("tencent", this);
    }
}
