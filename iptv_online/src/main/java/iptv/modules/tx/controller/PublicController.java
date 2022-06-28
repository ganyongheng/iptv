package iptv.modules.tx.controller;

import com.alibaba.fastjson.JSONObject;

import iptv.common.BusinessException;
import iptv.common.CheckUtils;
import iptv.config.redis.RedisCache;
import iptv.modules.base.service.impl.MobileOrderInterService;
import iptv.modules.tx.business.BaseBusiness;
import iptv.modules.tx.factory.BaseBusinessFactory;
import iptv.modules.tx.service.impl.MobileInterService;
import iptv.util.BizConstant;
import iptv.util.HttpIncident;
import iptv.util.HttpUtils;
import iptv.util.SysConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Author wyy
 * Date 2022/3/16 17:35
 **/
@Controller
@RequestMapping("/snm_user_center")
public class PublicController {

    private static Logger log = LoggerFactory.getLogger(PublicController.class);

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private SysConfig sysConfig;

    @Autowired
    private MobileInterService mobileInterService;

    @Autowired
    private MobileOrderInterService mobileOrderInterService;

    @Autowired
    private BaseBusinessFactory baseBusinessFactory;

    /**
     * 用户接入 该接口目的是将移动IPTV用户与腾讯平台用户对应起来，是让移动用户获得腾讯平台提供服务的基础。移动IPTV平台需要通过该接口，
     * 向snm_boss后台来获取移动用户对应的腾讯平台用户id。
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "getVuid", method = RequestMethod.POST)
    @ResponseBody
    public String getVuid(@RequestBody JSONObject req) {
        String cooperatorCode = req.get("cooperatorCode").toString();
        BaseBusiness baseBusiness = baseBusinessFactory.creatBaseBusiness(cooperatorCode);
        return baseBusiness.getBaseVuid(req);
    }

    /**
     * 获取调用腾讯接口需要的token
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "getAccessToken", method = RequestMethod.POST)
    @ResponseBody
    public String getAccessToken(@RequestBody JSONObject req) {
        String cooperatorCode = req.get("cooperatorCode").toString();
        BaseBusiness baseBusiness = baseBusinessFactory.creatBaseBusiness(cooperatorCode);
        return baseBusiness.getBaseAccessToken(req);
    }

    /**
     * 查询会员权益
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "vip_info", method = RequestMethod.POST)
    @ResponseBody
    public String vipInfo(@RequestBody JSONObject req) {
        String cooperatorCode = req.get("cooperatorCode").toString();
        BaseBusiness baseBusiness = baseBusinessFactory.creatBaseBusiness(cooperatorCode);
        return baseBusiness.vipInfo(req);
    }

    /**
     * 支付接入 该接口用于移动用户在购买腾讯侧的产品包后，移动平台借助snm_boss平台告知腾讯进行发货。
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "confirm_order_single", method = RequestMethod.POST)
    @ResponseBody
    public String confirmOrderSingle(@RequestBody JSONObject req) {
        String cooperatorCode = req.get("cooperatorCode").toString();
        BaseBusiness baseBusiness = baseBusinessFactory.creatBaseBusiness(cooperatorCode);
        return baseBusiness.confirmOrderSingle(req);
    }

    /**
     * 支付接入 该接口用于移动用户在购买腾讯侧的产品包后，移动平台借助snm_boss平台告知腾讯进行发货。
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "confirm_order", method = RequestMethod.POST)
    @ResponseBody
    public String confirmOrder(@RequestBody JSONObject req) {

        JSONObject resp = new JSONObject();
        // 校验参数
        try {
            CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-渠道代码【source】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("vippkg"), "请求失败：缺少请求参数-产品包唯一id【vippkg】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("vuid"), "请求失败：缺少请求参数-腾讯视频用户id【vuid】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("userId"), "请求失败：缺少请求参数-用户id【userId】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("ext_traceno"), "请求失败：缺少请求参数-平台的订单号【ext_traceno】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("inner_pay_time"), "请求失败：缺少请求参数-订单支付时间【inner_pay_time】",
                    BizConstant.Code.Missing_Parameter);
        } catch (BusinessException e) {
            log.error("【confirmOrder】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【confirmOrder】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }

        // 进入service处理
        boolean lock = false;
        try {
            try {
                //给锁设置超时时间 默认30秒
                lock = redisCache.setnxWithExptime("MobileOrderLock_" + req.getString("source") + "_" + req.getString("ext_traceno"), req.getString("ext_traceno"), 30);
//				lock = true;
            } catch (Exception e) {
                log.error("下单发货请求获取订单临时锁失败：【" + req.getString("ext_traceno") + "】" + e.getCause(), e);
                //lock=true;
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "下单发货请求获取订单临时锁失败");
                resp.put("order_id", req.getString("ext_traceno"));
                resp.put("vuid", req.getString("vuid"));
                return JSONObject.toJSONString(resp);
            }
            if (lock) {
                String cooperatorCode = req.getString("cooperatorCode");
                //腾讯渠道
                if (BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)) {
                    //是否确认发货标志
                    String confirmDeliver = req.getString("confirm_deliver") == null ? "0" : req.getString("confirm_deliver").toString();
                    String reqVippkg = req.getString("vippkg");
                    if (!"1".equals(confirmDeliver)) {
                        req = mobileOrderInterService.removeWarnBuyVippkg(req);
                    }
                    if (req.getString("vippkg") == null) {
                        resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                        resp.put("msg", "用户userId:" + req.getString("userId") + "，在限定时间内重复购买产品:" + reqVippkg + ",不进行发货");
                        resp.put("order_id", req.getString("ext_traceno"));
                        resp.put("vuid", req.getString("vuid"));
                        return JSONObject.toJSONString(resp);
                    }
                    //下单主方法
                    resp = mobileOrderInterService.createAndConfirmOrder(req);
                }
                //优酷渠道
                else if (BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)) {
                    resp = mobileOrderInterService.youkuConfirmOrder(req);
                }
                //未知
                else {
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "存在未支持的第三方视频平台编码：" + cooperatorCode + ",不进行发货");
                    resp.put("order_id", req.getString("ext_traceno"));
                    resp.put("vuid", req.getString("vuid"));
                    return JSONObject.toJSONString(resp);
                }
            } else {
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "存在订单号为" + req.getString("ext_traceno") + "的请求正在处理");
                resp.put("order_id", req.getString("ext_traceno"));
                resp.put("vuid", req.getString("vuid"));
                return JSONObject.toJSONString(resp);
            }
        } catch (Exception e) {
            log.error("【confirmOrder】处理订单数据出错：" + JSONObject.toJSONString(req) + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "【confirmOrder】处理订单数据出错:" + e.getMessage());
            return JSONObject.toJSONString(resp);
        } finally {
            if (lock) {
                try {
                    redisCache.del("MobileOrderLock_" + req.getString("source") + "_" + req.getString("ext_traceno"));
                } catch (Exception e) {
                    log.error("下单发货删除临时锁失败：【" + req.getString("ext_traceno") + "】" + e.getCause(), e);
                }
            }
        }

        return JSONObject.toJSONString(resp);
    }

    /**
     * 取消自动续费接口
     * @param req
     * @return
     */
    @RequestMapping(value = "cancel_autopay", method = RequestMethod.POST)
    @ResponseBody
    public String cancelAutopay(@RequestBody JSONObject req) {

        JSONObject resp = new JSONObject();
        // 校验参数
        try {
            CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-渠道代码【source】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("vippkg"), "请求失败：缺少请求参数-产品包唯一id【vippkg】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("vuid"), "请求失败：缺少请求参数-腾讯视频用户id【vuid】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("userId"), "请求失败：缺少请求参数-用户id【userId】",
                    BizConstant.Code.Missing_Parameter);
        } catch (BusinessException e) {
            log.error("【cancel_account】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【cancel_account】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }
        boolean lock = false;

        try {
            //给锁设置超时时间 默认30秒
            lock = redisCache.setnxWithExptime("MobileCancelOrderLock_" + req.getString("source") + "_" + req.getString("userId"), req.getString("userId"), 30);
//			lock = true;
        } catch (Exception e) {
            log.error("下单发货请求获取订单临时锁失败：【" + req.getString("ext_traceno") + "】" + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "重复请求，取消自动续费请求获取临时锁失败");
            return JSONObject.toJSONString(resp);
        }
        if (lock) {

            try {
                resp = mobileOrderInterService.cancelAccount(req);
            } catch (Exception e) {
                log.error("【cancel_account】取消续订出错：" + JSONObject.toJSONString(req) + e.getCause(), e);
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "【cancel_account】取消续订出错");
                return JSONObject.toJSONString(resp);
            }
        }

        return JSONObject.toJSONString(resp);

    }

    /**
     * 检查snm_boss平台心跳接口
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "test_snm_boss", method = RequestMethod.POST)
    @ResponseBody
    public String testSnmBoss(@RequestBody JSONObject req) {
        //直接返回
        return "snmbossok";
    }

    /**
     * 检查snm_boss平台心跳接口
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "test_tencent", method = RequestMethod.POST)
    @ResponseBody
    public String testTencent(@RequestBody JSONObject req) {
        //调用腾讯发货地址
        String url = sysConfig.getTX_CONFIRM_ORDER_URL();
        if (StringUtils.isBlank(url)) {
            log.error("调用腾讯发货的url没有配置【TX_CONFIRM_ORDER_URL】！");
            return "snmbosserror";
        }
        //获取access_token
        String accessToken = "";
        try {
            accessToken = mobileInterService.getAccessToken();
        } catch (Exception e) {
            log.error("获取accessToken失败：" + e.getCause(), e);
        }
        String confirmOrder_url = url + "&access_token=" + accessToken + "&order_id=201903241627AC4B13A7&user_type=0&vuserid=289039149&ext_reserved=...";
        String confirmOrder_resp = HttpUtils.doGet(confirmOrder_url);

        if (StringUtils.isBlank(confirmOrder_resp)) {
            return "snmbosserror";
        } else {
            try {
                JSONObject confirmOrder_resp_json = JSONObject.parseObject(confirmOrder_resp);
                JSONObject confirmOrder_resp_result = JSONObject.parseObject(confirmOrder_resp_json.getString("result"));
                if (!BizConstant.Code.Result_Code_Success_Num_0.equals(confirmOrder_resp_result.getString("code"))) {
                    //如果返回不是成功，告警
                    //调用告警接口通知告警系统,只执行一个简单方法，让aop切面调用接口告警
                    log.error("腾讯心跳检测，调用腾讯接口返回错误：" + confirmOrder_resp);
                    HttpIncident.incidentPush("腾讯心跳检测，调用腾讯接口返回错误：" + confirmOrder_resp, BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
                }
            } catch (Exception e) {
                log.error("心跳检测转换json数据出错：" + e.getCause(), e);
                return "snmbosserror";
            }
        }
        //直接返回
        return "snmbossok";
    }

    @RequestMapping(value = "refund_autopay", method = RequestMethod.POST)
    @ResponseBody
    public String refundAutopay(@RequestBody JSONObject req) {
        JSONObject resp = new JSONObject();
        // 校验参数
        try {
            CheckUtils.checkEmpty(req.getString("vuid"), "请求失败：缺少请求参数-用户vuid【vuid】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-渠道来源【source】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("userId"), "请求失败：缺少请求参数-用户编号【userId】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("vippkg"), "请求失败：缺少请求参数-用户编号【vippkg】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("cooperatorCode"), "请求失败：缺少请求参数-用户编号【cooperatorCode】",
                    BizConstant.Code.Missing_Parameter);
        } catch (BusinessException e) {
            log.error("【refund_autopay】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【refund_autopay】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }

        if(!BizConstant.Code.Order.Cooperator_Code_Youku.equals(req.getString("cooperatorCode"))){
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "暂时只支持cooperatorCode:youku，的退款申请");
            return JSONObject.toJSONString(resp);
        }

        boolean lock=false;
        try {
            try {
                //给锁设置超时时间 默认30秒
                lock = redisCache.setnxWithExptime("MobileOrderRefundLock_" + req.getString("source") +"_"+ req.getString("ext_traceno"), req.getString("ext_traceno"),30);
//				lock = true;
            } catch (Exception e) {
                log.error("申请退费请求获取临时锁失败：【"+req.getString("ext_traceno")+"】"+e.getCause(),e);
                //lock=true;
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "申请退费请求获取临时锁失败");
                resp.put("order_id", req.getString("ext_traceno"));
                resp.put("vuid", req.getString("vuid"));
                return JSONObject.toJSONString(resp);
            }
            if(lock){
                resp = mobileOrderInterService.youkuRefundOrder(req);
            }
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【refund_autopay】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }


    }

}
