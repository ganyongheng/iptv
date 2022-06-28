package iptv.modules.tx.process;

import com.alibaba.fastjson.JSONObject;
import iptv.common.BusinessException;
import iptv.config.redis.RedisCache;
import iptv.modules.base.entity.db.IptvProduct;
import iptv.modules.base.entity.db.MobileOrderInfoSingle;
import iptv.modules.base.factory.confirmordersingle.ConfirmOrderSingleProcessFactory;
import iptv.modules.base.factory.vipinfo.VipInfoProcessFactory;
import iptv.modules.base.process.ConfirmOrderSingleProcess;
import iptv.modules.base.process.VipInfoProcess;
import iptv.modules.base.service.impl.IptvProductServiceImpl;
import iptv.util.BizConstant;
import iptv.util.HttpUtils;
import iptv.util.SysBaseUtil;
import iptv.util.SysConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author wyq
 * @create 2022/3/18 14:59
 */
@Component
public class TxConfirmOrderSingleProcess extends ConfirmOrderSingleProcess implements InitializingBean {


    /**
     * 发货具体逻辑
     *
     * @param req
     * @return
     */
    @Override
    public JSONObject confirmOrderSingle(JSONObject req, JSONObject resp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        boolean lock = false;
        try {
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
                // 校验产品配置
                String[] product_code_list = req.getString("vippkg").split("\\+");
                List<IptvProduct> iptvProductList = iptvProductService.getIptvProduct(product_code_list,
                        req.getString("source"));
                if (null == iptvProductList || iptvProductList.size() < 1) {
                    log.error("产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
                    return resp;
                }
                //根据订单号和source获取订单信息
                MobileOrderInfoSingle mobileOrderInfo = mobileOrderInfoSingleService.getMobileOrderInfoSingleByExtTraceno(req.getString("ext_traceno"),
                        req.getString("source"));
                if (null != mobileOrderInfo) {
                    //存在
                    //是否发货成功
                    if (BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
                        //返回结果
                        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                        resp.put("msg", "订单发货成功");
                        resp.put("vuid", mobileOrderInfo.getVuid());
                        resp.put("order_confirmtime", mobileOrderInfo.getOrderConfirmtime().getTime() / 1000);
                        resp.put("order_createtime", mobileOrderInfo.getOrderCreatetime().getTime() / 1000);
                        resp.put("order_id", mobileOrderInfo.getOrderId());
                        resp.put("order_price", mobileOrderInfo.getOrderPrice());
                        resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                        resp.put("service", mobileOrderInfo.getService());
                        resp.put("vip_endtime", vipInfo.get("end"));
                        return resp;
                    } else {

//				resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
//				resp.put("msg", "请求腾讯发货返回失败!");
                        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                        resp.put("msg", "订单发货成功");
                        resp.put("order_id", req.getString("ext_traceno"));
                        resp.put("vuid", req.getString("vuid"));
                        resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                        //计算权益时间，并设置resp
                        resp = super.analyzEndTime(resp, vipInfo, false, iptvProductList.get(0));
                        return resp;
                    }
                } else {
                    //不存在
                    //创建single订单对象
                    mobileOrderInfo = new MobileOrderInfoSingle();
                }
                mobileOrderInfo.setExtTraceno(req.getString("ext_traceno"));
                mobileOrderInfo.setUserid(req.getString("userId"));
                mobileOrderInfo.setVuid(req.getString("vuid"));
                mobileOrderInfo.setVippkg(req.getString("vippkg"));
                mobileOrderInfo.setInnerPayTime(dateFormat.parse(req.getString("inner_pay_time")));
                mobileOrderInfo.setSource(req.getString("source"));
                mobileOrderInfo.setStatus(BizConstant.Code.Result_Code_Success_Num_0);
                mobileOrderInfo.setOrderType(BizConstant.Tencent.Order_Type_Bill);
                mobileOrderInfo.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_No);
                mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());

                StringBuffer third_code = new StringBuffer();
                for (int i = 0; i < iptvProductList.size(); i++) {
                    String third_vippkg = "";
                    third_vippkg = iptvProductList.get(i).getThirdCode();
                    if (i == 0) {
                        third_code.append(third_vippkg);
                    } else {
                        third_code.append("+" + third_vippkg);
                    }
                }
                mobileOrderInfo.setThirdVippkg(third_code.toString());
                mobileOrderInfo.setCooperatorCode(BizConstant.Code.Order.Cooperator_Code_Tencent);
                mobileOrderInfo.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_No);

                //创建订单
                resp = confirmOrder(req, resp, mobileOrderInfo, iptvProductList.get(0));
                //记录库表数据
                mobileOrderInfoSingleService.save(mobileOrderInfo);

                //兼容旧数据，该用户旧流程中入存在拆分订单，置上忽略标志
                mobileOrderInfoDetService.ignoreMobileOrderInfoDetsByUserId(req.getString("userId"), req.getString("source"));

                //判断是否发货成功
                if (!BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
                    //失败，放入redis重试队列
                    resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
                    resp.put("msg", "订单发货成功");
                    resp.put("order_id", req.getString("ext_traceno"));
                    resp.put("vuid", req.getString("vuid"));
                    resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                    // 下单出错，加入充实队列，并进行告警
                    super.addResentSingleOrderQueue(mobileOrderInfo, BizConstant.Code.Order.Cooperator_Code_Tencent);
                    //计算权益时间，并设置resp
                    resp = super.analyzEndTime(resp, vipInfo, false, iptvProductList.get(0));
                } else {
                    //成功，更新订购关系，放入redis权益修正队列
                    for (IptvProduct iptvProduct : iptvProductList) {
                        try {
                            // 更新订购关系表
                            super.doProcessMobileUserProductSingle(mobileOrderInfo.getUserid(),
                                    mobileOrderInfo.getVuid(), iptvProduct.getProductCode(),
                                    mobileOrderInfo.getIsAutopay(), mobileOrderInfo.getSource(),
                                    mobileOrderInfo.getExtTraceno(), mobileOrderInfo.getOrderId(),
                                    mobileOrderInfo.getTraceno(), BizConstant.Code.Order.Cooperator_Code_Tencent);

                            //计算权益时间，并设置resp
                            resp = analyzEndTime(resp, vipInfo, true, iptvProduct);
                            JSONObject redis = new JSONObject();
                            redis.put("order_id", resp.get("order_id"));
                            redis.put("ext_traceno", req.get("ext_traceno"));
                            redis.put("source", req.get("source"));
                            redis.put("userid", req.get("userId"));
                            redis.put("cooperator_code", BizConstant.Code.Order.Cooperator_Code_Tencent);
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
                        } catch (Exception e) {
                            log.error("下单发货成功，保存或更新订购关系表出错：" + e.getCause(), e);
                        }
                    }

                }
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
        return resp;
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
    public JSONObject confirmOrder(JSONObject req, JSONObject resp, MobileOrderInfoSingle mobileOrderInfo, IptvProduct iptvProduct) throws Exception {
        //获取token
        String access_token = mobileInterService.getAccessToken();
        resp = createOrderByTencent(req, resp, mobileOrderInfo, access_token);
        // resp.put("code", "0");
        if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
            // access_token失效，重新获取
            logger.info("创建订单access_token失效，重新获取下单");
            access_token = mobileInterService.getAccessTokenFromTx();
            // 设置token的有效时长并存在redis中
            mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
            resp = createOrderByTencent(req, resp, mobileOrderInfo, access_token);
        }

        //通知发货
        if (BizConstant.Code.Result_Code_Success_Num_0.equals(resp.getString("code"))) {
            // 睡眠两百毫秒，防止下单腾讯还没处理完成
            Thread.sleep(200);
            resp = confirmOrderByTencent(req, resp, mobileOrderInfo, access_token);
            // mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Success);
            // mobileOrderInfo.setOrder_status(BizConstant.Tencent.Tx_Order_Status_Shipped);
            if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
                // access_token失效，重新获取
                logger.info("订单发货access_token失效，重新获取发货");
                access_token = mobileInterService.getAccessTokenFromTx();
                // 设置token的有效时长并存在redis中
                mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
                resp = confirmOrderByTencent(req, resp, mobileOrderInfo, access_token);
            }
        }
        return resp;
    }


    /**
     * 腾讯通知发货
     *
     * @param req
     * @param resp
     * @param mobileOrderInfo
     * @param access_token
     * @return
     */
    private JSONObject confirmOrderByTencent(JSONObject req, JSONObject resp, MobileOrderInfoSingle mobileOrderInfo, String access_token) {
        try {
            resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
            resp.put("msg", "订单发货成功");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // 组装参数请求腾讯发货接口
            String confirmOrder_url = sysConfig.getTX_CONFIRM_ORDER_URL();
            if (StringUtils.isBlank(confirmOrder_url)) {
                log.error("请求腾讯订单发货的url地址未配置！");
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求腾讯订单发货的url地址未配置！");
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
                return resp;
            }

            confirmOrder_url = confirmOrder_url + "&access_token=" + access_token + "&order_id="
                    + mobileOrderInfo.getOrderId() + "&user_type=0&vuserid=" + mobileOrderInfo.getVuid()
                    + "&ext_reserved=" + mobileOrderInfo.getExtReserved();

            String confirmOrder_resp;
            try {
                confirmOrder_resp = HttpUtils.doGet(confirmOrder_url);
            } catch (Exception e) {
                log.error("请求腾讯订单发货出错：" + e.getCause(), e);
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求腾讯订单发货出错!");
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
                return resp;
            }

            if (StringUtils.isBlank(confirmOrder_resp)) {
                log.error("请求腾讯订单发货接口返回为空");
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求腾讯订单发货接口返回为空");
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
                return resp;
            }

            JSONObject confirmOrder_resp_json = JSONObject.parseObject(confirmOrder_resp);
            JSONObject confirmOrder_resp_result = JSONObject.parseObject(confirmOrder_resp_json.getString("result"));
            JSONObject confirmOrder_resp_data = JSONObject.parseObject(confirmOrder_resp_json.getString("data"));
            if (BizConstant.Code.Result_Code_Success_Num_0.equals(confirmOrder_resp_result.getString("code"))) {
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Success);
                mobileOrderInfo.setOrderStatus(BizConstant.Tencent.Tx_Order_Status_Shipped);
                String order_confirmtime = dateFormat
                        .format(confirmOrder_resp_data.getLong("order_confirmtime") * 1000);
                mobileOrderInfo.setOrderConfirmtime(dateFormat.parse(order_confirmtime));
                mobileOrderInfo.setService(confirmOrder_resp_data.getString("service"));

                // 组装返回数据给
                resp.put("vuid", mobileOrderInfo.getVuid());
                resp.put("order_confirmtime", confirmOrder_resp_data.getLong("order_confirmtime"));
                resp.put("order_createtime", confirmOrder_resp_data.getLong("order_createtime"));
                resp.put("order_id", mobileOrderInfo.getOrderId());
                resp.put("order_price", mobileOrderInfo.getOrderPrice());
                resp.put("order_status", mobileOrderInfo.getOrderStatus());
                resp.put("service", mobileOrderInfo.getService());

                return resp;
            } else {
                if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(confirmOrder_resp_result.getString("code"))) {
                    resp.put("code", BizConstant.Tencent.AccessToken_Invalid_Code);
                    resp.put("msg", "非法的access_token");
                    return resp;
                }

                logger.info("请求腾讯发货返回失败：" + confirmOrder_resp_result.getString("msg"));
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求腾讯发货返回失败!");
                mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Fail);
                mobileOrderInfo.setMsg(resp.getString("msg"));
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

    /**
     * 腾讯创建订单
     *
     * @param req
     * @param resp
     * @param mobileOrderInfo
     * @param access_token
     * @return
     */
    private JSONObject createOrderByTencent(JSONObject req, JSONObject resp, MobileOrderInfoSingle mobileOrderInfo, String access_token) {
        try {
            resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
            resp.put("msg", "订单创建成功");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            String createOrderTxRedisData = redisCache.getCache(BizConstant.RedisKey.TxCreateOrder
                    + mobileOrderInfo.getSource() + "_" + mobileOrderInfo.getTraceno(), String.class);

            String createOrder_resp = "";

            // 判断redis缓存的腾讯创建订单数据是否为空,不为空取redis的数据, 为空请求接口
            if (StringUtils.isBlank(createOrderTxRedisData)) {
                // 还没创建过订单
                String createOrder_url = sysConfig.getTX_CREATE_ORDER_URL();
                if (StringUtils.isBlank(createOrder_url)) {
                    log.error("请求腾讯创建订单的url地址未配置！");
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "请求腾讯创建订单的url地址未配置！");
                    mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Create_Fail);
                    mobileOrderInfo.setMsg(resp.getString("msg"));
                    return resp;
                }

                String Q_UA = sysBaseUtil.getSysBaseParam(mobileOrderInfo.getSource(), "OPERATOR_CHANNEL");
                if (StringUtils.isBlank(Q_UA)) {
                    log.error("请求腾讯创建订单的Q-UA未配置！");
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "请求腾讯创建订单的Q-UA未配置！");
                    mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Create_Fail);
                    mobileOrderInfo.setMsg(resp.getString("msg"));
                    return resp;
                }

                // 产品包拼接中间用+号分割 ,URL 中+的转义编码为 %2B
                createOrder_url = createOrder_url + "&access_token=" + access_token + "&vippkg="
                        + mobileOrderInfo.getThirdVippkg().replace("+", "%2B") + "&user_type=0&vuserid="
                        + req.getString("vuid") + "&ext_traceno=" + mobileOrderInfo.getTraceno() + "&Q-UA=" + Q_UA;

                try {
                    createOrder_resp = HttpUtils.doGet(createOrder_url);
                } catch (Exception e) {
                    log.error("请求腾讯创建订单出错：" + e.getCause(), e);
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "请求腾讯创建订单出错!");
                    mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Create_Fail);
                    mobileOrderInfo.setMsg(resp.getString("msg"));
                    return resp;
                }

                if (StringUtils.isBlank(createOrder_resp)) {
                    log.error("请求腾讯创建订单接口返回为空");
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", "请求腾讯创建订单接口返回为空");
                    mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Create_Fail);
                    mobileOrderInfo.setMsg(resp.getString("msg"));
                    return resp;
                }
            } else {
                // 取已经缓存的redis的订单数据
                createOrder_resp = createOrderTxRedisData;
            }

            JSONObject createOrder_resp_json = JSONObject.parseObject(createOrder_resp);
            JSONObject createOrder_resp_result = JSONObject.parseObject(createOrder_resp_json.getString("result"));
            JSONObject createOrder_resp_data = JSONObject.parseObject(createOrder_resp_json.getString("data"));
            if (BizConstant.Code.Result_Code_Success_Num_0.equals(createOrder_resp_result.getString("code"))) {
                // 请求创建订单成功, 将腾讯返回的信息缓存到redis中 缓存时间是7天
                if (redisCache.setnxWithExptime(BizConstant.RedisKey.TxCreateOrder + mobileOrderInfo.getSource() + "_"
                        + mobileOrderInfo.getTraceno(), createOrder_resp, 60 * 60 * 24 * 7)) {
                    mobileOrderInfo.setOrderId(createOrder_resp_data.getString("order_id"));
                    String order_createtime = dateFormat
                            .format(createOrder_resp_data.getLong("order_createtime") * 1000);
                    mobileOrderInfo.setOrderCreatetime(dateFormat.parse(order_createtime));
                    mobileOrderInfo.setOrderPrice(createOrder_resp_data.getInteger("order_price"));
                    mobileOrderInfo.setOrderStatus(BizConstant.Tencent.Tx_Order_Status_TobeShipped);
                    mobileOrderInfo.setExtReserved(createOrder_resp_data.getString("ext_reserved"));
                    return resp;
                } else {
                    // 如果已经存在, 取redis的数据
                    String existsCreateOrderRedisData = redisCache.getCache(BizConstant.RedisKey.TxCreateOrder
                            + mobileOrderInfo.getSource() + "_" + mobileOrderInfo.getTraceno(), String.class);

                    if (StringUtils.isNotBlank(existsCreateOrderRedisData)) {
                        createOrder_resp_json = JSONObject.parseObject(createOrder_resp);
                        createOrder_resp_result = JSONObject.parseObject(createOrder_resp_json.getString("result"));
                        createOrder_resp_data = JSONObject.parseObject(createOrder_resp_json.getString("data"));

                        mobileOrderInfo.setOrderId(createOrder_resp_data.getString("order_id"));
                        String order_createtime = dateFormat
                                .format(createOrder_resp_data.getLong("order_createtime") * 1000);
                        mobileOrderInfo.setOrderCreatetime(dateFormat.parse(order_createtime));
                        mobileOrderInfo.setOrderPrice(createOrder_resp_data.getInteger("order_price"));
                        mobileOrderInfo.setOrderStatus(BizConstant.Tencent.Tx_Order_Status_TobeShipped);
                        mobileOrderInfo.setExtReserved(createOrder_resp_data.getString("ext_reserved"));
                        return resp;
                    } else {
                        resp.put("code", createOrder_resp_result.getString("code"));
                        resp.put("msg", "请求腾讯创建订单失败,获取redis已经存在的create_order失败");
                        return resp;
                    }
                }
            } else {
                if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(createOrder_resp_result.getString("code"))) {
                    resp.put("code", createOrder_resp_result.getString("code"));
                    resp.put("msg", "请求腾讯创建订单失败：" + createOrder_resp_result.getString("msg"));
                    return resp;
                } else {
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg", createOrder_resp_result.getString("msg"));
                    mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Create_Fail);
                    mobileOrderInfo.setMsg(createOrder_resp_result.getString("msg"));
                    return resp;
                }
            }
        } catch (Exception e) {
            log.error("系统出错：创建订单出错！" + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "系统出错：创建订单出错！");
            mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Create_Fail);
            mobileOrderInfo.setMsg(resp.getString("msg"));
            return resp;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ConfirmOrderSingleProcessFactory.create(BizConstant.Code.Order.Cooperator_Code_Tencent, this);
    }
}
