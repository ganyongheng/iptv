package iptv.modules.youku.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.youku.ott.openapi.sdk.OttOpenapiResponse;
import iptv.common.BusinessException;
import iptv.modules.base.entity.db.IptvProduct;
import iptv.modules.base.entity.db.MobileOrderInfoSingle;
import iptv.modules.base.factory.confirmordersingle.ConfirmOrderSingleProcessFactory;
import iptv.modules.base.process.ConfirmOrderSingleProcess;
import iptv.util.BizConstant;
import iptv.util.Constant;
import iptv.util.ServerResponse;
import iptv.util.YouKuRequstUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author wyq
 * @create 2022/3/21 15:13
 */
@Component
public class YoukuConfirmOrderSingleProcess extends ConfirmOrderSingleProcess implements InitializingBean {

    @Autowired
    private YouKuRequstUtils youKuRequstUtils;


    /**
     * 发货具体逻辑
     *
     * @param req
     * @param resp
     * @return
     */
    @Override
    public JSONObject confirmOrderSingle(JSONObject req, JSONObject resp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        boolean lock = false;
        try {
            resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
            resp.put("msg", "订单发货成功");
            //获取本次订购前权益时间
            JSONObject vipInfo = super.getVipInfo(req, resp);

            if (vipInfo.containsKey("is_vip") && Integer.valueOf(vipInfo.get("is_vip").toString()) == 1) {
                resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                resp.put("msg", "订单发货成功");
                resp.put("order_id", req.getString("ext_traceno"));
                resp.put("vuid", req.getString("vuid"));
                resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                resp.put("vip_endtime", vipInfo.get("end"));
                return resp;
            }
            //获取锁
            try {
                //给锁设置超时时间 默认30秒
                lock = redisCache.setnxWithExptime("MobileOrderSingleLock_" + req.getString("source") + "_" + req.getString("userId"), req.getString("ext_traceno"), 30);
//				lock = true;
            } catch (Exception e) {
                log.error("下单发货请求获取订单临时锁失败：【" + req.getString("ext_traceno") + "】" + e.getCause(), e);
                resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                resp.put("msg", "订单发货成功");
                resp.put("order_id", req.getString("ext_traceno"));
                resp.put("vuid", req.getString("vuid"));
                resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                resp.put("vip_endtime", System.currentTimeMillis() / 1000 + 31 * 24 * 60 * 60);
                return resp;
            }
            if (lock) {
                //校验产品
                IptvProduct iptvProduct = iptvProductService.getIptvProduct(req.getString("vippkg"),
                        req.getString("source"), BizConstant.Code.Order.Cooperator_Code_Youku);
                if (null == iptvProduct) {
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
                    //计算权益时间，并设置resp
                    //analyzEndTime(resp,vipInfo,false);
                    return resp;
                }
                //判断是否已发货
                MobileOrderInfoSingle mobileOrderInfo = mobileOrderInfoSingleService.getMobileOrderInfoSingleByExtTraceno(req.getString("ext_traceno"),
                        req.getString("source"));
                if (null != mobileOrderInfo) {
                    //已发货，返回
                    if (BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
                        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                        resp.put("msg", "订单发货成功");
                        resp.put("order_id", req.getString("ext_traceno"));
                        resp.put("vuid", req.getString("vuid"));
                        resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                        analyzEndTime(resp, vipInfo, false, iptvProduct);
                        return resp;
                    } else {
//				resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
//				resp.put("msg", "请求优酷发货返回失败!");
                        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                        resp.put("msg", "订单发货成功");
                        resp.put("order_id", req.getString("ext_traceno"));
                        resp.put("vuid", req.getString("vuid"));
                        resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                        analyzEndTime(resp, vipInfo, false, iptvProduct);
                        return resp;
                    }

                } else {
                    //未发货，创建实体类
                    mobileOrderInfo = new MobileOrderInfoSingle();
                }
                mobileOrderInfo.setCooperatorCode(BizConstant.Code.Order.Cooperator_Code_Youku);
                mobileOrderInfo.setExtTraceno(req.getString("ext_traceno"));
                mobileOrderInfo.setUserid(req.getString("userId"));
                mobileOrderInfo.setVuid(req.getString("vuid"));
                mobileOrderInfo.setVippkg(req.getString("vippkg"));
                mobileOrderInfo.setInnerPayTime(dateFormat.parse(req.getString("inner_pay_time")));
                mobileOrderInfo.setSource(req.getString("source"));
                mobileOrderInfo.setStatus(BizConstant.Code.Result_Code_Success_Num_0);
                mobileOrderInfo.setOrderType(BizConstant.Tencent.Order_Type_Bill);
                mobileOrderInfo.setThirdVippkg(req.getString("vippkg"));
                mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());
                mobileOrderInfo.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_No);
                //通知发货
                resp = confirmOrder(req, resp, mobileOrderInfo, iptvProduct);
                //记录表数据
                mobileOrderInfoSingleService.saveOrUpdate(mobileOrderInfo);
                if (BizConstant.Youku.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {

                    resp.put("vuid", mobileOrderInfo.getVuid());
                    resp.put("order_createtime", mobileOrderInfo.getInnerPayTime().getTime() / 1000);
                    resp.put("order_id", mobileOrderInfo.getOrderId());
                    resp.put("order_price", iptvProduct.getPrice());
                    resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                    Set products = new HashSet();
                    products.add(mobileOrderInfo.getThirdVippkg());
                    resp.put("service", JSON.toJSONString(products));
                    analyzEndTime(resp, vipInfo, true, iptvProduct);
                    // 更新订购关系表
                    doProcessMobileUserProductSingle(mobileOrderInfo.getUserid(),
                            mobileOrderInfo.getVuid(), iptvProduct.getProductCode(),
                            mobileOrderInfo.getIsAutopay(), mobileOrderInfo.getSource(),
                            mobileOrderInfo.getExtTraceno(), mobileOrderInfo.getOrderId(),
                            mobileOrderInfo.getTraceno(), BizConstant.Code.Order.Cooperator_Code_Youku);
                } else {
                    resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                    resp.put("msg", "订单发货成功");
                    resp.put("order_id", req.getString("ext_traceno"));
                    resp.put("vuid", req.getString("vuid"));
                    resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                    analyzEndTime(resp, vipInfo, false, iptvProduct);
                    //进入重发序列
                    addResentSingleOrderQueue(mobileOrderInfo, BizConstant.Code.Order.Cooperator_Code_Youku);
                }
                //放入redis
                JSONObject redis = new JSONObject();
                redis.put("order_id", resp.get("order_id"));
                redis.put("ext_traceno", req.get("ext_traceno"));
                redis.put("source", req.get("source"));
                redis.put("userid", req.get("userId"));
                redis.put("cooperator_code", BizConstant.Code.Order.Cooperator_Code_Youku);
                redis.put("notify_type", 2);
                redis.put("product_type", iptvProduct.getProductType());
                if (!BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
                    redis.put("vip_endtime_calculate", "");
                } else {
                    redis.put("vip_endtime_calculate", resp.get("vip_endtime"));
                }
                redis.put("vuid", resp.get("vuid"));
                redisCache.zAdd("SyncRightsTimer_SortSet", System.currentTimeMillis(),
                        redis.toJSONString());
                return resp;
            } else {
                //获取锁失败，存在线程正在处理
                resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                resp.put("msg", "订单发货成功");
                resp.put("order_id", req.getString("ext_traceno"));
                resp.put("vuid", req.getString("vuid"));
                resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                resp.put("vip_endtime", System.currentTimeMillis() / 1000 + 31 * 24 * 60 * 60);
                return resp;
            }
        } catch (BusinessException e) {
            log.error("【confirmOrder】处理订单数据出错：" + e.getMessage());
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return resp;
        } catch (Exception e) {
            log.error("【confirmOrder】处理订单数据出错：" + JSONObject.toJSONString(req) + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
            resp.put("msg", "订单发货成功");
            resp.put("order_id", req.getString("ext_traceno"));
            resp.put("vuid", req.getString("vuid"));
            resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
            resp.put("vip_endtime", System.currentTimeMillis() / 1000 + 31 * 24 * 60 * 60);
            return resp;
        } finally {
            if (lock) {
                try {
                    redisCache.del("MobileOrderSingleLock_" + req.getString("source") + "_" + req.getString("userId"));
                } catch (Exception e) {
                    log.error("下单发货删除临时锁失败：【" + req.getString("ext_traceno") + "】" + e.getCause(), e);
                }
            }
        }
    }

    /**
     * 通知第三方发货
     *
     * @param req
     * @param resp
     * @param mobileOrderInfo
     * @param iptvProduct
     * @return
     */
    @Override
    public JSONObject confirmOrder(JSONObject req, JSONObject resp, MobileOrderInfoSingle mobileOrderInfo, IptvProduct iptvProduct) {
        try {
            resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
            resp.put("msg", "订单发货成功");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //组装发货参数
            ServerResponse<OttOpenapiResponse> responese = null;
            Map<String, Object> requestParams = new HashMap<String, Object>();
            requestParams.put("orderId", mobileOrderInfo.getTraceno());
            if (BizConstant.MobileUserProduct.IsAutoPay_Yes.contains(mobileOrderInfo.getIsAutopay())) {
                requestParams.put("productId", iptvProduct.getPProductCode());
            } else {
                String third_code = iptvProduct.getThirdCode();
                if (StringUtils.isBlank(third_code)) {
                    log.error("发货失败：非自动续费产品third_code为空");
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "发货失败：非自动续费产品third_code为空!");
                    mobileOrderInfo.setStatus(BizConstant.Youku.Order_Status_Pay_Fail);
                    mobileOrderInfo.setMsg(resp.getString("msg"));
                    return resp;
                }
                requestParams.put("productId", third_code);
            }
            Date syncTime = new Date();
            requestParams.put("syncTime", dateFormat.format(syncTime));
            //需修改，测试用
            requestParams.put("channelId", youKuRequstUtils.getChannelIdBySource(mobileOrderInfo.getSource()));
            //需修改，测试用
            requestParams.put("accountId", mobileOrderInfo.getVuid());
//			if(BizConstant.MobileUserProduct.IsAutoPay_No.equals(mobileOrderInfo.getIs_autopay())){
            requestParams.put("type", BizConstant.Code.Order.Order_Type_Youku_Online);
//			}else{
//				requestParams.put("type", BizConstant.Code.Order.Order_Type_Youku_Renew);
//			}
            //通知优酷发货
            try {
                //responese = youKuRequstUtils.doRequest("ott.kitty.commonorder.sync", requestParams);
                responese = youKuRequstUtils.doRequest(sysConfig.getYOUKU_CONFIRM_ORDER_INTERFACE(), requestParams);
            } catch (Exception e) {
                log.error("请求优酷确认订单接口异常：" + e.getCause(), e);
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求优酷确认订单接口失败!");
                mobileOrderInfo.setStatus(BizConstant.Youku.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
                //进入重发序列
                addResentSingleOrderQueue(mobileOrderInfo, BizConstant.Code.Order.Cooperator_Code_Youku);
                return resp;
            }
            //判断是否发货成功
            if (Constant.ServerResponseStatus.FAIL == responese.getStatus()) {
                log.error("请求优酷确认订单接口失败");
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "失败原因,[code:" + responese.getData().getSubCode() + "],[msg:" + responese.getData().getSubMsg() + "]");
                mobileOrderInfo.setStatus(BizConstant.Youku.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
                return resp;
            }
            resp.put("order_confirmtime", syncTime.getTime() / 1000);
            mobileOrderInfo.setOrderCreatetime(syncTime);
            mobileOrderInfo.setStatus(BizConstant.Youku.Order_Status_Pay_Success);
            mobileOrderInfo.setOrderId(mobileOrderInfo.getTraceno());
            return resp;
        } catch (Exception e) {
            log.error("系统出错：创建订单出错！" + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "系统出错：确认订单出错！");
            mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Create_Fail);
            mobileOrderInfo.setMsg(resp.getString("msg"));
        }
        return resp;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        ConfirmOrderSingleProcessFactory.create(BizConstant.Code.Order.Cooperator_Code_Youku, this);
    }
}
