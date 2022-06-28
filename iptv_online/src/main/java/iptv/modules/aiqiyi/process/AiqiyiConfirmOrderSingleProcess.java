package iptv.modules.aiqiyi.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import iptv.common.BusinessException;
import iptv.modules.aiqiyi.entity.db.MobileUserProductAiqiyi;
import iptv.modules.base.entity.db.IptvProduct;
import iptv.modules.base.entity.db.MobileOrderInfoSingle;
import iptv.modules.base.factory.confirmordersingle.ConfirmOrderSingleProcessFactory;
import iptv.modules.base.process.ConfirmOrderSingleProcess;
import iptv.util.AiqiyiAuthenticationUtil;
import iptv.util.BizConstant;
import iptv.util.DateUtil;
import iptv.util.HttpUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author wyq
 * @create 2022/3/21 16:27
 */
@Component
public class AiqiyiConfirmOrderSingleProcess extends ConfirmOrderSingleProcess implements InitializingBean {
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
            //成功
            if (lock) {
                //判断产品是否存在
                IptvProduct iptvProduct = iptvProductService.getIptvProduct(req.getString("vippkg"),
                        req.getString("source"), BizConstant.Code.Order.Cooperator_Code_Aiqiyi);
                if (null == iptvProduct) {
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
                    //计算权益时间，并设置resp
//			analyzEndTime(resp,vipInfo,false);
                    return resp;
                }
                //判断是否已经发货
                MobileOrderInfoSingle mobileOrderInfo = mobileOrderInfoSingleService.getMobileOrderInfoSingleByExtTraceno(req.getString("ext_traceno"),
                        req.getString("source"));
                //已发货，返回
                if (null != mobileOrderInfo) {
                    if (BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
                        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                        resp.put("msg", "订单发货成功");
                        resp.put("order_id", req.getString("ext_traceno"));
                        resp.put("vuid", req.getString("vuid"));
                        resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                        analyzEndTime(resp, vipInfo, false, iptvProduct);
                        return resp;
                    } else {
                        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                        resp.put("msg", "订单发货成功");
                        resp.put("order_id", req.getString("ext_traceno"));
                        resp.put("vuid", req.getString("vuid"));
                        resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                        analyzEndTime(resp, vipInfo, false, iptvProduct);
                        return resp;
                    }

                } else {
                    //未发货，生成实体类
                    mobileOrderInfo = new MobileOrderInfoSingle();
                    mobileOrderInfo.setOrderCreatetime(new Date());
                }
                mobileOrderInfo.setCooperatorCode(BizConstant.Code.Order.Cooperator_Code_Aiqiyi);
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
                mobileOrderInfo.setOrderPrice(iptvProduct.getPrice());

                //通知发货
                resp = confirmOrder(req, resp, mobileOrderInfo, iptvProduct);
                // 记录库表数据
                mobileOrderInfoSingleService.save(mobileOrderInfo);
                if (BizConstant.Aiqiyi.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {

                    resp.put("vuid", mobileOrderInfo.getVuid());
                    resp.put("order_createtime", mobileOrderInfo.getInnerPayTime().getTime() / 1000);
                    resp.put("order_id", mobileOrderInfo.getOrderId());
                    //resp.put("order_price", iptvProduct.getPrice());
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
                            mobileOrderInfo.getTraceno(), BizConstant.Code.Order.Cooperator_Code_Aiqiyi);
                } else {
                    resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                    resp.put("msg", "订单发货成功");
                    resp.put("order_id", req.getString("ext_traceno"));
                    resp.put("vuid", req.getString("vuid"));
                    resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                    analyzEndTime(resp, vipInfo, false, iptvProduct);
                    //进入重发序列
                    addResentSingleOrderQueue(mobileOrderInfo, BizConstant.Code.Order.Cooperator_Code_Aiqiyi);
                }
                //记录redis
                JSONObject redis = new JSONObject();
                redis.put("order_id", resp.get("order_id"));
                redis.put("ext_traceno", req.get("ext_traceno"));
                redis.put("source", req.get("source"));
                redis.put("userid", req.get("userId"));
                redis.put("cooperator_code", BizConstant.Code.Order.Cooperator_Code_Aiqiyi);
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
//			resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
//			resp.put("msg", "【confirmOrder】处理订单数据出错:"+e.getMessage());
//			return JSONObject.toJSONString(resp);
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
            String confirmOrder_url = sysConfig.getAIQIYI_CONFIRM_ORDER_URL();
            if (StringUtils.isBlank(confirmOrder_url)) {
                log.error("请求爱奇艺订单发货的url地址未配置！");
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求爱奇艺订单发货的url地址未配置！");
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
                return resp;
            }

            //组装请求爱奇艺发货的参数
            TreeMap<String, Object> aiqiyiReq = new TreeMap<>();
            String productType = "";
            String status = "";
            //订单失效时间，包月自动续订用户默认为：2099-12-31 23:59:59 其它情况根据实际情况填写
            String expired_time = "";
            //订单生效时间
            String effective_time = "";
            //获取订单生效时间
            MobileUserProductAiqiyi mobileUserProduct = mobileUserProductAiqiyiService.getMobileUserProductByProductTypeAiqiyi(req.getString("userId"), req.getString("vuid"),
                    iptvProduct.getProductType(), req.getString("source"));
            Date vipETime = null;
            Date now = new Date(System.currentTimeMillis());
            if (mobileUserProduct == null) {
                vipETime = now;
            } else {
                vipETime = mobileUserProduct.getEtime();
            }

            if (now.after(vipETime)) {
                effective_time = dateFormat.format(now);
            } else {
                effective_time = dateFormat.format(vipETime);
            }

            productType = "cycle_payment";
            status = "orderSuccess";

            Date eDate = DateUtil.addDay(dateFormat.parse(effective_time), Integer.parseInt(iptvProduct.getProductDuration()));
            eDate = new Date(DateUtil.getEndOfDate(eDate.getTime()) * 1000);
            expired_time = dateFormat.format(eDate);

            String reqId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
            //产品类型
            aiqiyiReq.put("product_type", productType);
            //订单支付时间
            aiqiyiReq.put("pay_time", req.get("inner_pay_time"));
            //订单生效时间
            aiqiyiReq.put("effective_time", effective_time);
            //订单失效时间
            aiqiyiReq.put("expired_time", expired_time);
            //订单状态
            aiqiyiReq.put("status", status);
            //设备号
            aiqiyiReq.put("device_id", req.get("userId"));//
            //产品编号
            aiqiyiReq.put("product_no", iptvProduct.getThirdCode());
            //产品数量
            aiqiyiReq.put("product_count", 1);
            //version
            aiqiyiReq.put("version", sysConfig.getAIQIYI_SYNCORDER_VERSION());
            //每次请求的唯一标识
            aiqiyiReq.put("reqId", reqId);
            //订单编号
            aiqiyiReq.put("order_code", req.get("ext_traceno"));
            //订单费用
            aiqiyiReq.put("order_fee", iptvProduct.getPrice().toString());
            //合作方编号
            aiqiyiReq.put("partner_no", sysConfig.getAIQIYI_PARTNER_NO());
            //用户身份标识
            aiqiyiReq.put("open_id", req.get("vuid"));
            //生成鉴权编码
            String sign = AiqiyiAuthenticationUtil.authentication(aiqiyiReq);
            sign = StringUtils.lowerCase(sign);
            aiqiyiReq.put("sign", sign);

            String confirmOrder_resp;
            JSONObject aiqiyiReqJson = new JSONObject(aiqiyiReq);
            //向爱奇艺发货
            try {
                confirmOrder_resp = HttpUtils.doPost(confirmOrder_url, aiqiyiReqJson);
//                confirmOrder_resp = null;
//				confirmOrder_resp = "{\"code\":\"A00000\",\"msg\":\"成功\",\"timestamp\":\"20211102103153\"}";
            } catch (Exception e) {
                log.error("ext_traceno" + req.get("ext_traceno") + "请求爱奇艺订单发货出错：" + e.getCause(), e);
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求爱奇艺订单发货出错!");
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
                return resp;
            }
            //判断是否发货成功
            if (StringUtils.isBlank(confirmOrder_resp)) {
                log.error("请求爱奇艺订单发货接口返回为空 ext_traceno:" + req.get("ext_traceno"));
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求爱奇艺订单发货接口返回为空");
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
                return resp;
            }
            JSONObject confirmOrder_resp_json = JSONObject.parseObject(confirmOrder_resp);
            String code = confirmOrder_resp_json.getString("code");
            if (BizConstant.Aiqiyi.SuccessCode.equals(code)) {
                //发货成功
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Success);
                mobileOrderInfo.setOrderStatus(BizConstant.Tencent.Tx_Order_Status_Shipped);
                mobileOrderInfo.setOrderConfirmtime(dateFormat.parse(effective_time));
                mobileOrderInfo.setOrderId(mobileOrderInfo.getTraceno());
                // 组装返回数据给
                resp.put("vuid", mobileOrderInfo.getVuid());
                resp.put("order_confirmtime", dateFormat.parse(effective_time).getTime() / 1000);
                resp.put("order_createtime", mobileOrderInfo.getInnerPayTime().getTime() / 1000);
                resp.put("order_id", mobileOrderInfo.getOrderId());
                //resp.put("order_price", mobileOrderInfo.getOrder_price());
                resp.put("order_status", mobileOrderInfo.getOrderStatus());
                resp.put("service", mobileOrderInfo.getService());
                return resp;
            } else {
                //发货失败
                if (BizConstant.Aiqiyi.SignatureErrorCode.equals(code)) {
                    logger.info("爱奇艺发货sign生成错误：" + confirmOrder_resp.toString());
                    resp.put("code", BizConstant.Aiqiyi.SignatureErrorCode);
                    resp.put("msg", "爱奇艺发货sign生成错误");
                    return resp;
                }
                logger.info("请求爱奇艺发货返回失败：" + confirmOrder_resp.toString());
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求爱奇艺发货返回失败!");
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
                mobileOrderInfo.setOrderId(mobileOrderInfo.getTraceno());
                return resp;
            }
        } catch (Exception e) {
            log.error("系统出错：订单发货出错！" + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "系统出错：订单发货出错！");
            mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Fail);
            mobileOrderInfo.setMsg(resp.getString("msg"));
            return resp;
        }
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        ConfirmOrderSingleProcessFactory.create(BizConstant.Code.Order.Cooperator_Code_Aiqiyi, this);
    }
}
