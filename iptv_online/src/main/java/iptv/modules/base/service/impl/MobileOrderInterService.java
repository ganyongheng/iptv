package iptv.modules.base.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.youku.ott.openapi.sdk.OttOpenapiResponse;
import iptv.common.BusinessException;
import iptv.config.redis.RedisCache;
import iptv.modules.aiqiyi.entity.db.MobileUserProductAiqiyi;
import iptv.modules.aiqiyi.service.impl.MobileUserProductAiqiyiServiceImpl;
import iptv.modules.base.entity.db.*;
import iptv.modules.base.entity.vo.MobileOrderInfoBase;
import iptv.modules.base.entity.vo.MobileUserProductBase;
import iptv.modules.base.factory.confirmordersingle.ConfirmOrderSingleProcessFactory;
import iptv.modules.base.process.ConfirmOrderSingleProcess;
import iptv.modules.tx.business.BaseBusiness;
import iptv.modules.tx.entity.db.MobileOrderInfo;
import iptv.modules.tx.entity.db.MobileUserProduct;
import iptv.modules.tx.factory.BaseBusinessFactory;
import iptv.modules.tx.service.impl.MobileInterService;
import iptv.modules.tx.service.impl.MobileOrderInfoServiceImpl;
import iptv.modules.tx.service.impl.MobileUserProductServiceImpl;
import iptv.modules.youku.entity.db.MobileOrderInfoYouku;
import iptv.modules.youku.entity.db.MobileUserProductYouku;
import iptv.modules.youku.service.impl.MobileOrderInfoYoukuServiceImpl;
import iptv.modules.youku.service.impl.MobileUserProductYoukuServiceImpl;
import iptv.util.BizConstant;
import iptv.util.HttpUtilsException;
import iptv.util.SysBaseUtil;
import iptv.util.DateUtil;
import iptv.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
@Component
public class MobileOrderInterService {

	private static Logger log = LoggerFactory.getLogger(MobileOrderInterService.class);
	private static Logger mobileConfirmOrderLogger = LoggerFactory.getLogger("mobileConfirmOrderLogger");
	private static Logger mobileOrderFailReSendTaskLogger = LoggerFactory.getLogger("mobileOrderFailReSendTaskLogger");
	private static Logger mobileOrderSingleFailReSendTaskLogger = LoggerFactory.getLogger("mobileOrderSingleFailReSendTaskLogger");
	private static Logger mobileReissueOrderLogger = LoggerFactory.getLogger("mobileReissueOrderLogger");

	@Autowired
	private MobileInterService mobileInterService;

	@Autowired
	private MobileOrderInfoDetServiceImpl mobileOrderInfoDetService;

	@Autowired
	private MobileOrderInfoYoukuServiceImpl mobileOrderInfoYoukuService;

	@Autowired
	private MobileAutopayConfigService mobileAutopayConfigService;

	@Autowired
	private SysConfig sysConfig;

	@Autowired
	private YouKuRequstUtils youKuRequstUtils;

	@Autowired
	private SysBaseUtil sysBaseUtil;

	@Autowired
	private RedisCache redisCache;

	@Autowired
	private HttpUtilsException httpUtilsException;

	@Autowired
	private MobileUserInterService mobileUserInterService;

	@Autowired
	private MobileUserProductServiceImpl mobileUserProductServiceimpl;

	@Autowired
	private MobileUserProductYoukuServiceImpl mobileUserProductYoukuService;

	@Autowired
	private MobileUserProductAiqiyiServiceImpl mobileUserProductAiqiyiService;

	@Autowired
	private MobileUserProductDetServiceImpl mobileUserProductDetService;

	@Autowired
	private IptvProductServiceImpl iptvProductService;

	@Autowired
	private MobileOrderInfoServiceImpl mobileOrderInfoService;

	@Autowired
	private MobileOrderFailServiceImpl mobileOrderFailService;

	@Autowired
    private IncidentPushServcie incidentPushServcie;

	@Autowired
	private MobileSourceComputeModeServiceImpl mobileSourceComputeModeService;

	@Autowired
	private MobileOrderInfoDetermineServiceImpl mobileOrderInfoDetermineService;

	@Autowired
    private MobileOrderInfoSingleServiceImpl mobileOrderInfoSingleServiceImpl;

	@Autowired
	private MobileUserProductServiceImpl mobileUserProductServiceImpl;

	@Autowired
	private MobileUserProductDetServiceImpl mobileUserProductDetServiceImpl;

    @Autowired
    private BaseBusinessFactory baseBusinessFactory;

    @Autowired
    private ConfirmOrderSingleProcessFactory confirmOrderSingleProcessFactory;


	public JSONObject createAndConfirmOrder(JSONObject req) throws Exception {

		JSONObject resp = new JSONObject();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		// 校验产品配置
		String[] product_code_list = req.getString("vippkg").split("\\+");
		List<IptvProduct> iptvProductList = iptvProductService.getIptvProduct(product_code_list,
				req.getString("source"));
		if (null == iptvProductList || iptvProductList.size() < 1) {
			resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
			resp.put("msg", "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
			return resp;
		}

		MobileOrderInfo mobileOrderInfo = mobileOrderInfoService.getMobileOrderInfoByExtTraceno(req.getString("ext_traceno"),
				req.getString("source"));
		if (null != mobileOrderInfo) {
			if (BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
				resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
				resp.put("msg", "订单发货成功");
				resp.put("vuid", mobileOrderInfo.getVuid());
				resp.put("order_confirmtime", mobileOrderInfo.getOrderConfirmtime().getTime() / 1000);
				resp.put("order_createtime", mobileOrderInfo.getOrderCreatetime().getTime() / 1000);
				resp.put("order_id", mobileOrderInfo.getOrderId());
				resp.put("order_price", mobileOrderInfo.getOrderPrice());
				resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
				resp.put("service", mobileOrderInfo.getService());
				return resp;
			} else {
				resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
				resp.put("msg", "请求腾讯发货返回失败!");
				return resp;
			}

		} else {
			mobileOrderInfo = new MobileOrderInfo();
		}

		if (StringUtils.isBlank(req.getString("is_autopay"))) {
			req.put("is_autopay", BizConstant.MobileUserProduct.IsAutoPay_No);
		}

		mobileOrderInfo.setExtTraceno(req.getString("ext_traceno"));
		mobileOrderInfo.setUserid(req.getString("userId"));
		mobileOrderInfo.setVuid(req.getString("vuid"));
		mobileOrderInfo.setVippkg(req.getString("vippkg"));
		mobileOrderInfo.setInnerPayTime(dateFormat.parse(req.getString("inner_pay_time")));
		mobileOrderInfo.setSource(req.getString("source"));
		mobileOrderInfo.setStatus(BizConstant.Code.Result_Code_Success_Num_0);
		mobileOrderInfo.setIsAutopay(req.getString("is_autopay"));
		mobileOrderInfo.setOrderType(BizConstant.Tencent.Order_Type_Bill);
		// mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());

		StringBuffer third_code = new StringBuffer();
		List<MobileOrderInfoDet> orderInfoDetList = new ArrayList<MobileOrderInfoDet>();
		for (int i = 0; i < iptvProductList.size(); i++) {
			String third_vippkg = "";
			if (BizConstant.MobileUserProduct.IsAutoPay_No.equals(req.getString("is_autopay"))) {
				third_vippkg = iptvProductList.get(i).getThirdCode();
				// 生成分批发货子订单
				orderInfoDetList.addAll(getOrderInfoDet(iptvProductList.get(i), mobileOrderInfo));
			} else {
				third_vippkg = iptvProductList.get(i).getPThirdCode();
			}
			if (i == 0) {
				third_code.append(third_vippkg);
			} else {
				third_code.append("+" + third_vippkg);
			}
		}
		mobileOrderInfo.setThirdVippkg(third_code.toString());

		String access_token = mobileInterService.getAccessToken();
		// 发货订单
		MobileOrderInfoBase shipOrderInfo = null;
		if (BizConstant.MobileUserProduct.IsAutoPay_No.equals(req.getString("is_autopay"))) {
			shipOrderInfo = orderInfoDetList.get(0);
		} else {
			mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());
			shipOrderInfo = mobileOrderInfo;
		}

		// 创建订单
		resp = createOrderByTencent(req, resp, shipOrderInfo, access_token);
		// resp.put("code", "0");
		if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
			// access_token失效，重新获取
			mobileConfirmOrderLogger.info("创建订单access_token失效，重新获取下单");
			access_token = mobileInterService.getAccessTokenFromTx();
			// 设置token的有效时长并存在redis中
			mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
			resp = createOrderByTencent(req, resp, shipOrderInfo, access_token);
		}

		// 通知发货
		if (BizConstant.Code.Result_Code_Success_Num_0.equals(resp.getString("code"))) {
			// 睡眠两百毫秒，防止下单腾讯还没处理完成
			Thread.sleep(200);

			resp = confirmOrderByTencent(req, resp, shipOrderInfo, access_token);
			// mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Success);
			// mobileOrderInfo.setOrder_status(BizConstant.Tencent.Tx_Order_Status_Shipped);
			if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
				// access_token失效，重新获取
				mobileConfirmOrderLogger.info("订单发货access_token失效，重新获取发货");
				access_token = mobileInterService.getAccessTokenFromTx();
				// 设置token的有效时长并存在redis中
				mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
				resp = confirmOrderByTencent(req, resp, shipOrderInfo, access_token);
			}
		}

		if (BizConstant.MobileUserProduct.IsAutoPay_No.equals(req.getString("is_autopay"))) {
			if (orderInfoDetList.size() > 1) {
				mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Pay_Shipping);
			} else {
				mobileOrderInfo.setStatus(shipOrderInfo.getStatus());
			}
		}

		// 记录库表数据
		mobileOrderInfoService.save(mobileOrderInfo);
		if (orderInfoDetList.size() > 0) {
			//dbCenter.insert(orderInfoDetList, MobileOrderInfoDet.class.getName());
			mobileOrderInfoDetService.saveBatch(orderInfoDetList);
		}

		// 如果请求腾讯下单失败，将该请求放到redis重试队列
		if (!BizConstant.Tencent.Order_Status_Pay_Success.equals(shipOrderInfo.getStatus())) {
			// 下单出错，加入充实队列，并进行告警
			addResentQueue(shipOrderInfo);

		} else {
			if (BizConstant.MobileUserProduct.IsAutoPay_No.equals(req.getString("is_autopay"))) {
				// 非自动续费
				try {
					// 更新订购关系表
					doProcessMobileUserProduct(shipOrderInfo.getUserid(),
							shipOrderInfo.getVuid(), shipOrderInfo.getVippkg(), shipOrderInfo.getIsAutopay(),
							shipOrderInfo.getSource(), shipOrderInfo.getExtTraceno(), shipOrderInfo.getOrderId(),
							shipOrderInfo.getTraceno(),BizConstant.Code.Order.Cooperator_Code_Tencent);
				} catch (Exception e) {
					log.error("下单发货成功，保存或更新订购关系表出错：" + e.getCause(), e);
				}
			} else {
				// 自动续费
				for (IptvProduct iptvProduct : iptvProductList) {
					try {
						// 更新订购关系表
						doProcessMobileUserProduct(mobileOrderInfo.getUserid(),
								mobileOrderInfo.getVuid(), iptvProduct.getProductCode(),
								mobileOrderInfo.getIsAutopay(), mobileOrderInfo.getSource(),
								mobileOrderInfo.getExtTraceno(), mobileOrderInfo.getOrderId(),
								mobileOrderInfo.getTraceno(),BizConstant.Code.Order.Cooperator_Code_Tencent);
					} catch (Exception e) {
						log.error("下单发货成功，保存或更新订购关系表出错：" + e.getCause(), e);
					}
				}
			}
		}

		return resp;

	}

    public JSONObject createAndConfirmOrderResend(JSONObject req) throws Exception {

        JSONObject resp = new JSONObject();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String MobileOrderResend_Max_Nums = sysConfig.getMobileOrderResend_Max_Nums();
        if (StringUtils.isBlank(MobileOrderResend_Max_Nums)) {
            MobileOrderResend_Max_Nums = "5";
        }

        // 如果请求腾讯下单失败，将该请求放到redis重试队列
        MobileOrderFail mobileOrderFail = getMobileOrderFailByExtTraceno(req.getString("extTraceno"),
                req.getString("source"));
        if (Integer.valueOf(MobileOrderResend_Max_Nums) <= req.getIntValue("nums")) {
            mobileOrderFailReSendTaskLogger.info("订单ext_traceno为[" + req.getString("extTraceno") + "]的数据大于重复发的次数");
            mobileOrderFail.setNums(req.getIntValue("nums"));
            mobileOrderFail.setStatus(BizConstant.Tencent.Resend.Mobile_Order_Status_Max_Nums);
            mobileOrderFail.setMemo("订单数据ext_traceno=" + req.getString("extTraceno") + "大于重复发的次数");
            mobileOrderFailService.updateById(mobileOrderFail);
            //dbCenter.update(mobileOrderFail, MobileOrderFail.class.getName());
            // 告警
            incidentPushServcie.incidentPush("订单ext_traceno为[" + req.getString("extTraceno") + "]渠道为[" + req.getString("source")
                            + "]的数据订单发货失败达到最大次数",
                    BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);

            return resp;
        }

        MobileOrderInfoBase mobileOrderInfo = null;
        if (BizConstant.MobileUserProduct.IsAutoPay_No.equals(req.getString("isAutopay"))) {
            mobileOrderInfo = getMobileOrderInfoDetByTraceno(req.getString("traceno"), req.getString("source"));
        } else {
            mobileOrderInfo = getMobileOrderInfoByExtTraceno(req.getString("extTraceno"), req.getString("source"));
        }

        if (null == mobileOrderInfo
                || BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
            log.info("订单重试下单时查询订单数据ext_traceno=" + req.getString("extTraceno") + "不存在或者已经支付完成。");
            throw new Exception("订单重试下单时查询订单数据ext_traceno=" + req.getString("extTraceno") + "不存在或者已经支付完成。");
        }

        String access_token = mobileInterService.getAccessToken();

        // 创建订单
        resp = createOrderByTencent(req, resp, mobileOrderInfo, access_token);
        if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
            // access_token失效，重新获取
            mobileConfirmOrderLogger.info("创建订单access_token失效，重新获取下单");
            access_token = mobileInterService.getAccessTokenFromTx();
            // 设置token的有效时长并存在redis中
            mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
            resp = createOrderByTencent(req, resp, mobileOrderInfo, access_token);
        }

        // 通知发货
        if (BizConstant.Code.Result_Code_Success_Num_0.equals(resp.getString("code"))) {
            // 睡眠两百毫秒，防止下单腾讯还没处理完成
            Thread.sleep(200);

            resp = confirmOrderByTencent(req, resp, mobileOrderInfo, access_token);
            if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
                // access_token失效，重新获取
                mobileConfirmOrderLogger.info("订单发货access_token失效，重新获取发货");
                access_token = mobileInterService.getAccessTokenFromTx();
                // 设置token的有效时长并存在redis中
                mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
                resp = confirmOrderByTencent(req, resp, mobileOrderInfo, access_token);
            }
        }

        if (!BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
            // 重试下单出错，进行告警
            httpUtilsException.httpUtilsIncidentPush(
                    "渠道【" + mobileOrderInfo.getSource() + "】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货失败："
                            + mobileOrderInfo.getMsg(),
                    BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            mobileOrderFailReSendTaskLogger.info("渠道【" + mobileOrderInfo.getSource() + "】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货失败："
                    + mobileOrderInfo.getMsg());
            try {
                // 将数据放到redis集合
                String interval = sysConfig.getMobileOrderResend_Interval();
                if (StringUtils.isBlank(interval)) {
                    // 30分钟 默认 单位为秒
                    interval = "600";
                }
                long time = Long.valueOf(interval) * 1000;
                Date afterDate = new Date(new Date().getTime() + time);
                req.put("sendtime", dateFormat.format(afterDate));
                req.put("traceno", mobileOrderInfo.getTraceno());
                req.put("nums", req.getInteger("nums") + 1);
                redisCache.putCache("MobileResendOrderData_" + mobileOrderFail.getExtTraceno(),
                        JSONObject.toJSONString(req));
                redisCache.zAdd("MobileOrderResendDequeueTask_SortSet", afterDate.getTime(),
                        mobileOrderFail.getExtTraceno());
            } catch (Exception e) {
                mobileOrderFailReSendTaskLogger.error("[setResendDataToRedisList] 失败订单重发数据放进redis队列出错:" + JSONObject.toJSONString(req));
            }
        } else {
            mobileOrderInfo.setMsg(null);
            if (BizConstant.MobileUserProduct.IsAutoPay_No.equals(req.getString("isAutopay"))) {
                MobileOrderInfoDet mobileOrderInfoDet = new MobileOrderInfoDet();
                BeanUtils.copyProperties(mobileOrderInfo,mobileOrderInfoDet);
                mobileOrderInfoDetService.updateById(mobileOrderInfoDet);
                //dbCenter.update(mobileOrderInfo, MobileOrderInfoDet.class.getName());

                String orderInfoStatus = getMobileOrderInfoStatus(mobileOrderInfo.getExtTraceno(),
                        mobileOrderInfo.getTraceno(), mobileOrderInfo.getSource(),
                        BizConstant.Tencent.OrderDet_Status_Pay_Success);
                MobileOrderInfo parentMobileOrderInfo = getMobileOrderInfoByExtTraceno(req.getString("extTraceno"),
                        req.getString("source"));
                parentMobileOrderInfo.setStatus(orderInfoStatus);
                mobileOrderInfoService.updateById(parentMobileOrderInfo);
                //dbCenter.update(parentMobileOrderInfo, MobileOrderInfo.class.getName());
            } else {
                MobileOrderInfo mobileOrderInfo1 = new MobileOrderInfo();
                BeanUtils.copyProperties(mobileOrderInfo,mobileOrderInfo1);
                mobileOrderInfoService.updateById(mobileOrderInfo1);
                // 记录库表数据
                //dbCenter.update(mobileOrderInfo, MobileOrderInfo.class.getName());
            }
            // 更改失败数据记录
            mobileOrderFail.setStatus(BizConstant.Tencent.Resend.Mobile_Order_Status_Success);
            mobileOrderFailService.updateById(mobileOrderFail);
            //dbCenter.update(mobileOrderFail, MobileOrderFail.class.getName());

            // 校验产品配置

            if (BizConstant.Code.Code_YES.equals(req.getString("isAutoAccount"))) {
                // 自动续费 出现的账单
                // 更新订购关系表
                doProcessMobileUserProductForAccount(mobileOrderFail, mobileOrderInfo);
            } else {
                // 普通下单 出现的账单
                String[] product_code_list = req.getString("vippkg").split("\\+");
                List<IptvProduct> iptvProductList = iptvProductService.getIptvProduct(product_code_list,
                        req.getString("source"));
                if (null == iptvProductList || iptvProductList.size() < 1) {
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg",
                            "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
                    return resp;
                }

                for (IptvProduct iptvProduct : iptvProductList) {
                    try {
                        // 更新订购关系表
                        doProcessMobileUserProduct(mobileOrderInfo.getUserid(),
                                mobileOrderInfo.getVuid(), iptvProduct.getProductCode(),
                                mobileOrderInfo.getIsAutopay(), mobileOrderInfo.getSource(),
                                mobileOrderInfo.getExtTraceno(), mobileOrderInfo.getOrderId(),
                                mobileOrderInfo.getTraceno(),BizConstant.Code.Order.Cooperator_Code_Tencent);
                    } catch (Exception e) {
                        log.error("下单发货成功，保存或更新订购关系表出错：" + e.getCause(), e);
                    }
                }
            }
            mobileOrderFailReSendTaskLogger.info("渠道【" + mobileOrderInfo.getSource() + "】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货成功！");
        }

        return resp;

    }

    public JSONObject createAndConfirmOrderSingleResend(JSONObject req) throws Exception {
        JSONObject resp = new JSONObject();
        String cooperatorCode = req.get("cooperatorCode").toString();
        BaseBusiness baseBusiness = baseBusinessFactory.creatBaseBusiness(cooperatorCode);
        JSONObject vipInfo = JSONObject.parseObject(baseBusiness.vipInfo(req));
        //获取本次订购前权益时间
        //JSONObject vipInfo=mobileUserInterService.vipInfo(req);

        if(vipInfo.containsKey("is_vip")&&Integer.valueOf(vipInfo.get("is_vip").toString())==1){
            return resp;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String MobileOrderResend_Max_Nums = sysConfig.getMobileOrderResend_Max_Nums();
        if (StringUtils.isBlank(MobileOrderResend_Max_Nums)) {
            MobileOrderResend_Max_Nums = "5";
        }

        MobileOrderInfoSingle mobileOrderInfo = getMobileOrderInfoSingleByExtTraceno(req.getString("extTraceno"), req.getString("source"));
        if (null == mobileOrderInfo
                || BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
            log.info("订单重试下单时查询订单数据ext_traceno=" + req.getString("extTraceno") + "不存在或者已经支付完成。");
            throw new Exception("订单重试下单时查询订单数据ext_traceno=" + req.getString("extTraceno") + "不存在或者已经支付完成。");
        }

        // 如果请求腾讯下单失败，将该请求放到redis重试队列
        MobileOrderFail mobileOrderFail = getMobileOrderFailByExtTraceno(req.getString("extTraceno"),
                req.getString("source"));
        if (Integer.valueOf(MobileOrderResend_Max_Nums) <= req.getIntValue("nums")) {
            mobileOrderSingleFailReSendTaskLogger.info("订单ext_traceno为[" + req.getString("extTraceno") + "]的数据大于重复发的次数");
            mobileOrderFail.setNums(req.getIntValue("nums"));
            mobileOrderFail.setStatus(BizConstant.Tencent.Resend.Mobile_Order_Status_Max_Nums);
            mobileOrderFail.setMemo("订单数据ext_traceno=" + req.getString("extTraceno") + "大于重复发的次数");
            mobileOrderFailService.updateById(mobileOrderFail);
            //dbCenter.update(mobileOrderFail, MobileOrderFail.class.getName());

            JSONObject redis = new JSONObject();
            redis.put("order_id", resp.get("order_id"));
            redis.put("ext_traceno", mobileOrderFail.getExtTraceno());
            redis.put("source", mobileOrderFail.getSource());
            redis.put("userid", mobileOrderFail.getUserId());
            redis.put("cooperator_code", mobileOrderInfo.getCooperatorCode());
            redis.put("notify_type", 1);
            IptvProduct iptvProduct = iptvProductService.getIptvProduct(mobileOrderFail.getVippkg(),
                    mobileOrderFail.getSource(), mobileOrderInfo.getCooperatorCode());
            redis.put("product_type", iptvProduct.getProductType());
            redis.put("vip_endtime_calculate", "");
            //修改从MobileOrderFail对象获取vuid原resp没有赋值
            redis.put("vuid", mobileOrderInfo.getVuid());
            redisCache.zAdd("SyncRightsTimer_SortSet", new Date().getTime(),
                    redis.toJSONString());

            // 告警
            incidentPushServcie.incidentPush("订单ext_traceno为[" + req.getString("extTraceno") + "]渠道为[" + req.getString("source")
                            + "]的数据订单发货失败达到最大次数",
                    BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);

            return resp;
        }


        if(BizConstant.Code.Order.Cooperator_Code_Tencent.equals(mobileOrderInfo.getCooperatorCode())){

            String access_token = mobileInterService.getAccessToken();

            // 创建订单
            resp = createOrderByTencent(req, resp, mobileOrderInfo, access_token);
            if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
                // access_token失效，重新获取
                mobileConfirmOrderLogger.info("创建订单access_token失效，重新获取下单");
                access_token = mobileInterService.getAccessTokenFromTx();
                // 设置token的有效时长并存在redis中
                mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
                resp = createOrderByTencent(req, resp, mobileOrderInfo, access_token);
            }

            // 通知发货
            if (BizConstant.Code.Result_Code_Success_Num_0.equals(resp.getString("code"))) {
                // 睡眠两百毫秒，防止下单腾讯还没处理完成
                Thread.sleep(200);

                resp = confirmOrderByTencent(req, resp, mobileOrderInfo, access_token);
                if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
                    // access_token失效，重新获取
                    mobileConfirmOrderLogger.info("订单发货access_token失效，重新获取发货");
                    access_token = mobileInterService.getAccessTokenFromTx();
                    // 设置token的有效时长并存在redis中
                    mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
                    resp = confirmOrderByTencent(req, resp, mobileOrderInfo, access_token);
                }
            }

            if (!BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
//				// 重试下单出错，进行告警
//				httpUtilsException.httpUtilsIncidentPush(
//						"渠道【" + mobileOrderInfo.getSource() + "】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货失败："
//								+ mobileOrderInfo.getMsg(),
//						BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);

                try {
                    // 将数据放到redis集合
                    String interval = sysConfig.getMobileOrderResend_Interval();
                    if (StringUtils.isBlank(interval)) {
                        // 30分钟 默认 单位为秒
                        interval = "600";
                    }
                    long time = Long.valueOf(interval) * 1000;
                    Date afterDate = new Date(new Date().getTime() + time);
                    req.put("sendtime", dateFormat.format(afterDate));
                    req.put("traceno", mobileOrderInfo.getTraceno());
                    req.put("nums", req.getInteger("nums") + 1);
                    redisCache.putCache("MobileResendSingleOrderData_" + mobileOrderFail.getExtTraceno(),
                            JSONObject.toJSONString(req));
                    redisCache.zAdd("MobileOrderSingleResendDequeueTask_SortSet", afterDate.getTime(),
                            mobileOrderFail.getExtTraceno());
                    mobileOrderSingleFailReSendTaskLogger.info("渠道【" + mobileOrderInfo.getSource() + "】第三方编码【"+ mobileOrderInfo.getCooperatorCode() +"】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货失败："
                            + mobileOrderInfo.getMsg());
                } catch (Exception e) {
                    log.error("[setResendDataToRedisList] 失败订单重发数据放进redis队列出错:" + JSONObject.toJSONString(req));
                }
            } else {
                mobileOrderInfo.setMsg(null);
                mobileOrderInfoSingleServiceImpl.updateById(mobileOrderInfo);
                // 记录库表数据
                //dbCenter.update(mobileOrderInfo, MobileOrderInfoSingle.class.getName());
                // 更改失败数据记录
                mobileOrderFail.setStatus(BizConstant.Tencent.Resend.Mobile_Order_Status_Success);
                mobileOrderFailService.updateById(mobileOrderFail);
                //dbCenter.update(mobileOrderFail, MobileOrderFail.class.getName());

                // 校验产品配置
                // 普通下单 出现的账单
                String[] product_code_list = req.getString("vippkg").split("\\+");
                List<IptvProduct> iptvProductList = iptvProductService.getIptvProduct(product_code_list,
                        req.getString("source"));
                if (null == iptvProductList || iptvProductList.size() < 1) {
                    resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                    resp.put("msg",
                            "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
                    return resp;
                }

                for (IptvProduct iptvProduct : iptvProductList) {
                    try {
                        //创建具体类
                        ConfirmOrderSingleProcess confirmOrderSingleProcess = confirmOrderSingleProcessFactory.creatBaseAction(cooperatorCode);

                        // 更新订购关系表
                        confirmOrderSingleProcess.doProcessMobileUserProductSingle(mobileOrderInfo.getUserid(),
                                mobileOrderInfo.getVuid(), iptvProduct.getProductCode(),
                                mobileOrderInfo.getIsAutopay(), mobileOrderInfo.getSource(),
                                mobileOrderInfo.getExtTraceno(), mobileOrderInfo.getOrderId(),
                                mobileOrderInfo.getTraceno(),BizConstant.Code.Order.Cooperator_Code_Tencent);

                        JSONObject redis = new JSONObject();
                        redis.put("order_id", resp.get("order_id"));
                        redis.put("ext_traceno", mobileOrderFail.getExtTraceno());
                        redis.put("source", mobileOrderFail.getSource());
                        redis.put("userid", mobileOrderFail.getUserId());
                        redis.put("cooperator_code", mobileOrderInfo.getCooperatorCode());
                        redis.put("notify_type", 1);
                        redis.put("product_type", iptvProductList.get(0).getProductType());
                        redis.put("vip_endtime_calculate", System.currentTimeMillis()/1000+31*24*60*60);
                        redis.put("vuid", resp.get("vuid"));
                        redisCache.zAdd("SyncRightsTimer_SortSet", new Date().getTime(),
                                redis.toJSONString());

                    } catch (Exception e) {
                        log.error("下单发货成功，保存或更新订购关系表出错：" + e.getCause(), e);
                    }
                }

//				JSONObject redis = new JSONObject();
//				redis.put("order_id", resp.get("order_id"));
//				redis.put("ext_traceno", mobileOrderFail.getExtTraceno());
//				redis.put("source", mobileOrderFail.getSource());
//				redis.put("userid", mobileOrderFail.getUserId());
//				redis.put("cooperator_code", mobileOrderInfo.getCooperatorCode());
//				redis.put("notify_type", 1);
//				redis.put("product_type", iptvProductList.get(0).getProductType());
//				redis.put("vip_endtime_calculate", System.currentTimeMillis()/1000+31*24*60*60);
//				redis.put("vuid", resp.get("vuid"));
//				redisCache.zAdd("SyncRightsTimer_SortSet", new Date().getTime(),
//						redis.toJSONString());
                mobileOrderSingleFailReSendTaskLogger.info("渠道【" + mobileOrderInfo.getSource() + "】第三方编码【"+ mobileOrderInfo.getCooperatorCode() +"】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货成功！");
            }
        }else if(BizConstant.Code.Order.Cooperator_Code_Youku.equals(mobileOrderInfo.getCooperatorCode())){
            IptvProduct iptvProduct = iptvProductService.getIptvProduct(req.getString("vippkg"),
                    req.getString("source"),BizConstant.Code.Order.Cooperator_Code_Youku);
            if (null == iptvProduct) {
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
                return resp;
            }
            if (StringUtils.isBlank(req.getString("is_autopay"))) {
                req.put("is_autopay", BizConstant.MobileUserProduct.IsAutoPay_No);
            }

            mobileOrderInfo.setExtTraceno(req.getString("extTraceno"));
            mobileOrderInfo.setUserid(req.getString("userId"));
            mobileOrderInfo.setVuid(req.getString("vuid"));
            mobileOrderInfo.setVippkg(req.getString("vippkg"));
            mobileOrderInfo.setInnerPayTime(new Date(Long.valueOf(req.getString("inner_pay_time").toString())));
            mobileOrderInfo.setSource(req.getString("source"));
            mobileOrderInfo.setStatus(BizConstant.Code.Result_Code_Success_Num_0);
            mobileOrderInfo.setIsAutopay(req.getString("is_autopay"));
            mobileOrderInfo.setOrderType(BizConstant.Tencent.Order_Type_Bill);
            mobileOrderInfo.setThirdVippkg(req.getString("vippkg"));
            mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());
            resp = confirmOrderByYouku(resp, mobileOrderInfo,iptvProduct);
            // 记录库表数据，这里更新数据，不是插入，底层封装是这样
            mobileOrderInfoSingleServiceImpl.saveOrUpdate(mobileOrderInfo);
            //dbCenter.save(mobileOrderInfo, MobileOrderInfoSingle.class.getName());
            if(BizConstant.Youku.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())){

                resp.put("vuid", mobileOrderInfo.getVuid());
                resp.put("order_createtime", mobileOrderInfo.getInnerPayTime().getTime() / 1000);
                resp.put("order_id", mobileOrderInfo.getOrderId());
                resp.put("order_price", iptvProduct.getPrice());
                resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                Set products = new HashSet();
                products.add(mobileOrderInfo.getThirdVippkg());
                resp.put("service", JSON.toJSONString(products));

                // 更新订购关系表
                doProcessMobileUserProduct(mobileOrderInfo.getUserid(),
                        mobileOrderInfo.getVuid(), iptvProduct.getProductCode(),
                        mobileOrderInfo.getIsAutopay(), mobileOrderInfo.getSource(),
                        mobileOrderInfo.getExtTraceno(), mobileOrderInfo.getOrderId(),
                        mobileOrderInfo.getTraceno(),BizConstant.Code.Order.Cooperator_Code_Youku);
                JSONObject redis = new JSONObject();
                redis.put("order_id", resp.get("order_id"));
                redis.put("ext_traceno", mobileOrderFail.getExtTraceno());
                redis.put("source", mobileOrderFail.getSource());
                redis.put("userid", mobileOrderFail.getUserId());
                redis.put("cooperator_code", mobileOrderInfo.getCooperatorCode());
                redis.put("notify_type", 1);
                redis.put("product_type", iptvProduct.getProductType());
                redis.put("vip_endtime_calculate", System.currentTimeMillis()/1000+31*24*60*60);
                redis.put("vuid", resp.get("vuid"));
                redisCache.zAdd("SyncRightsTimer_SortSet", new Date().getTime(),
                        redis.toJSONString());
                mobileOrderSingleFailReSendTaskLogger.info("渠道【" + mobileOrderInfo.getSource() + "】第三方编码【"+ mobileOrderInfo.getCooperatorCode() +"】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货成功！");
            }else{
                try {
                    // 将数据放到redis集合
                    String interval = sysConfig.getMobileOrderResend_Interval();
                    if (StringUtils.isBlank(interval)) {
                        // 30分钟 默认 单位为秒
                        interval = "600";
                    }
                    long time = Long.valueOf(interval) * 1000;
                    Date afterDate = new Date(new Date().getTime() + time);
                    req.put("sendtime", dateFormat.format(afterDate));
                    req.put("traceno", mobileOrderInfo.getTraceno());
                    req.put("nums", req.getInteger("nums") + 1);
                    redisCache.putCache("MobileResendSingleOrderData_" + mobileOrderFail.getExtTraceno(),
                            JSONObject.toJSONString(req));
                    redisCache.zAdd("MobileOrderSingleResendDequeueTask_SortSet", afterDate.getTime(),
                            mobileOrderFail.getExtTraceno());
                    mobileOrderSingleFailReSendTaskLogger.info("渠道【" + mobileOrderInfo.getSource() + "】第三方编码【"+ mobileOrderInfo.getCooperatorCode() +"】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货失败："
                            + mobileOrderInfo.getMsg());
                } catch (Exception e) {
                    log.error("[setResendDataToRedisList] 失败订单重发数据放进redis队列出错:" + JSONObject.toJSONString(req));
                }
            }

        } else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(mobileOrderInfo.getCooperatorCode())) {

            IptvProduct iptvProduct = iptvProductService.getIptvProduct(req.getString("vippkg"),
                    req.getString("source"),BizConstant.Code.Order.Cooperator_Code_Aiqiyi);
            if (null == iptvProduct) {
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
                return resp;
            }
            if (StringUtils.isBlank(req.getString("is_autopay"))) {
                req.put("is_autopay", BizConstant.MobileUserProduct.IsAutoPay_No);
            }

            mobileOrderInfo.setExtTraceno(req.getString("extTraceno"));
            mobileOrderInfo.setUserid(req.getString("userId"));
            mobileOrderInfo.setVuid(req.getString("vuid"));
            mobileOrderInfo.setVippkg(req.getString("vippkg"));
            mobileOrderInfo.setInnerPayTime(new Date(Long.valueOf(req.getString("inner_pay_time").toString())));
            mobileOrderInfo.setSource(req.getString("source"));
            mobileOrderInfo.setStatus(BizConstant.Code.Result_Code_Success_Num_0);
            mobileOrderInfo.setIsAutopay(req.getString("is_autopay"));
            mobileOrderInfo.setOrderType(BizConstant.Tencent.Order_Type_Bill);
            mobileOrderInfo.setThirdVippkg(req.getString("vippkg"));
            mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());
            //创建具体类
            ConfirmOrderSingleProcess confirmOrderSingleProcess = confirmOrderSingleProcessFactory.creatBaseAction(cooperatorCode);
            resp = confirmOrderSingleProcess.confirmOrder(req, resp, mobileOrderInfo,iptvProduct);
            //resp = confirmOrderByAiqiyi(req, resp, mobileOrderInfo,iptvProduct);
            // 记录库表数据，这里更新数据，不是插入，底层封装是这样
            mobileOrderInfoSingleServiceImpl.saveOrUpdate(mobileOrderInfo);
            //dbCenter.save(mobileOrderInfo, MobileOrderInfoSingle.class.getName());
            if(BizConstant.Aiqiyi.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())){

                resp.put("vuid", mobileOrderInfo.getVuid());
                resp.put("order_createtime", mobileOrderInfo.getInnerPayTime().getTime() / 1000);
                resp.put("order_id", mobileOrderInfo.getOrderId());
                resp.put("order_price", iptvProduct.getPrice());
                resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
                Set products = new HashSet();
                products.add(mobileOrderInfo.getThirdVippkg());
                resp.put("service", JSON.toJSONString(products));

                // 更新订购关系表
                doProcessMobileUserProduct(mobileOrderInfo.getUserid(),
                        mobileOrderInfo.getVuid(), iptvProduct.getProductCode(),
                        mobileOrderInfo.getIsAutopay(), mobileOrderInfo.getSource(),
                        mobileOrderInfo.getExtTraceno(), mobileOrderInfo.getOrderId(),
                        mobileOrderInfo.getTraceno(),BizConstant.Code.Order.Cooperator_Code_Aiqiyi);
                JSONObject redis = new JSONObject();
                redis.put("order_id", resp.get("order_id"));
                redis.put("ext_traceno", mobileOrderFail.getExtTraceno());
                redis.put("source", mobileOrderFail.getSource());
                redis.put("userid", mobileOrderFail.getUserId());
                redis.put("cooperator_code", mobileOrderInfo.getCooperatorCode());
                redis.put("notify_type", 1);
                redis.put("product_type", iptvProduct.getProductType());
                redis.put("vip_endtime_calculate", System.currentTimeMillis()/1000+31*24*60*60);
                redis.put("vuid", resp.get("vuid"));
                redisCache.zAdd("SyncRightsTimer_SortSet", new Date().getTime(),
                        redis.toJSONString());
                mobileOrderSingleFailReSendTaskLogger.info("渠道【" + mobileOrderInfo.getSource() + "】第三方编码【"+ mobileOrderInfo.getCooperatorCode() +"】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货成功！");
            }else{
                try {
                    // 将数据放到redis集合
                    String interval = sysConfig.getMobileOrderResend_Interval();
                    if (StringUtils.isBlank(interval)) {
                        // 30分钟 默认 单位为秒
                        interval = "600";
                    }
                    long time = Long.valueOf(interval) * 1000;
                    Date afterDate = new Date(new Date().getTime() + time);
                    req.put("sendtime", dateFormat.format(afterDate));
                    req.put("traceno", mobileOrderInfo.getTraceno());
                    req.put("nums", req.getInteger("nums") + 1);
                    redisCache.putCache("MobileResendSingleOrderData_" + mobileOrderFail.getExtTraceno(),
                            JSONObject.toJSONString(req));
                    redisCache.zAdd("MobileOrderSingleResendDequeueTask_SortSet", afterDate.getTime(),
                            mobileOrderFail.getExtTraceno());
                    mobileOrderSingleFailReSendTaskLogger.info("渠道【" + mobileOrderInfo.getSource() + "】第三方编码【"+ mobileOrderInfo.getCooperatorCode() +"】订单流水【" + mobileOrderInfo.getExtTraceno() + "】重发下单发货失败："
                            + mobileOrderInfo.getMsg());
                } catch (Exception e) {
                    log.error("[setResendDataToRedisList] 失败订单重发数据放进redis队列出错:" + JSONObject.toJSONString(req));
                }
            }


        }
        return resp;

    }

    /**
     * 自动续费出账重试机制中 更新订购关系
     * @param mobileOrderFail
     * @param
     * @return
     * @throws Exception
     */
    public JSONObject doProcessMobileUserProductForAccount(MobileOrderFail mobileOrderFail,MobileOrderInfoBase mobileOrderInfo) throws Exception{

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyyMM");

        IptvProduct iptvProduct=iptvProductService.getIptvProduct(mobileOrderFail.getVippkg(), mobileOrderFail.getSource(),BizConstant.Code.Order.Cooperator_Code_Tencent);
        if(null==iptvProduct){
            throw new Exception("续费重发,根据product_code【"+mobileOrderFail.getVippkg()+"】source【"+mobileOrderFail.getSource()+"】没有找到对应的配置产品");
        }

        // 支付成功，新增或者更新订购关系表
        MobileUserProduct mobileUserProduct =mobileUserProductServiceImpl.getMobileUserProduct(mobileOrderFail.getUserId(), mobileOrderFail.getVuid(),
               mobileOrderFail.getSource());
        if (null == mobileUserProduct) {
            throw new Exception("续费重发,根据userid【"+mobileOrderFail.getUserId()+"】product_code【"+mobileOrderFail.getProductCode()+"】source【"+mobileOrderFail.getSource()+"】没有找到对应的订购数据");
        }

        String feetime = "";
        Date dd_stime;
        mobileUserProduct.setPstatus(BizConstant.MobileUserProduct.Pstatus_Useid);
        mobileUserProduct.setThirdCode(mobileOrderFail.getThirdVippkg());
        Integer product_duration = Integer.valueOf(iptvProduct.getProductDuration());
        if(DateTimeUtil.DateTimeCompare(mobileUserProduct.getEtime(), date)<=0){
            Date d_stime=new Date(date.getTime());
            dd_stime=d_stime;
            mobileUserProduct.setStime(date);
            mobileUserProduct.setEtime(DateUtil.addDay(date, product_duration));
            feetime = dateFormat.format(DateUtil.addMonth(date, product_duration / 31));
            mobileUserProduct.setFeemonth(feetime.substring(0, 8).replace("-", ""));
        }else{
            Date d_stime=new Date(mobileUserProduct.getEtime().getTime());
            dd_stime=d_stime;
            mobileUserProduct.setEtime(DateUtil.addDay(mobileUserProduct.getEtime(), product_duration));
            mobileUserProduct.setFeemonth(dateFormat1.format(DateUtil.addMonth(dateFormat1.parse(mobileUserProduct.getFeemonth()), product_duration/31)));
        }
        mobileUserProduct.setUpdateTime(date);
        mobileUserProductServiceImpl.updateById(mobileUserProduct);
        // 记录库表数据
        //dbCenter.update(mobileUserProduct, MobileUserProduct.class.getName());

        //订购关系明细表
        MobileUserProductDet mobileUserProductDet=new MobileUserProductDet();
        mobileUserProductDet.setUserId(mobileOrderFail.getUserId());
        mobileUserProductDet.setVuid(mobileOrderFail.getVuid());
        mobileUserProductDet.setExtTraceno(mobileOrderFail.getExtTraceno());
        mobileUserProductDet.setOrderId(mobileOrderInfo.getOrderId());
        mobileUserProductDet.setProductCode(mobileOrderFail.getProductCode());
        mobileUserProductDet.setThirdCode(mobileUserProduct.getThirdCode());
        mobileUserProductDet.setpProductCode(iptvProduct.getPProductCode());
        mobileUserProductDet.setStime(dd_stime);
        mobileUserProductDet.setEtime(mobileUserProduct.getEtime());
        mobileUserProductDet.setFeemonth(mobileUserProduct.getFeemonth());
        mobileUserProductDet.setpProductCode(iptvProduct.getPProductCode());
        mobileUserProductDet.setCreateTime(date);
        mobileUserProductDet.setSource(mobileOrderFail.getSource());
        mobileUserProductDet.setUpdateTime(date);
        mobileUserProductDet.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_Yes);
        mobileUserProductDet.setTraceno(mobileOrderInfo.getTraceno());
        mobileUserProductDetServiceImpl.saveOrUpdate(mobileUserProductDet);
        // 记录库表数据
        //dbCenter.save(mobileUserProductDet, MobileUserProductDet.class.getName());


        return null;
    }

    /**
     * 根据ext_traceno 获取订单的数据
     *
     * @param ext_traceno
     * @return
     */
    public MobileOrderFail getMobileOrderFailByExtTraceno(String ext_traceno, String source) {
        MobileOrderFail mobileOrderFailByExtTraceno = mobileOrderFailService.getMobileOrderFailByExtTraceno(ext_traceno, source);
        return mobileOrderFailByExtTraceno;
    }

	/**
	 * 创建订单组件
	 *
	 * @param req
	 * @param resp
	 * @param mobileOrderInfo
	 * @return
	 * @throws Exception
	 */
	public JSONObject createOrderByTencent(JSONObject req, JSONObject resp, MobileOrderInfoBase mobileOrderInfo,
										   String access_token) {
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

                mobileConfirmOrderLogger.info("请求腾讯下单 createOrder_url：" + createOrder_url);
				try {
					createOrder_resp = HttpUtils.doGet(createOrder_url);
                    mobileConfirmOrderLogger.info("请求腾讯下单返回数据 createOrder_resp：" + createOrder_resp);
				} catch (Exception e) {
                    mobileConfirmOrderLogger.info("请求腾讯下单返回数据 createOrder_resp：" + createOrder_resp);
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

	/**
	 * 发货订单组件
	 *
	 * @param req
	 * @param resp
	 * @param mobileOrderInfo
	 * @return
	 */
	public JSONObject confirmOrderByTencent(JSONObject req, JSONObject resp, MobileOrderInfoBase mobileOrderInfo,
											String access_token) {
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

            mobileConfirmOrderLogger.info("请求腾讯发货confirmOrder_url：" + confirmOrder_url);
			String confirmOrder_resp = "";
			try {
				confirmOrder_resp = HttpUtils.doGet(confirmOrder_url);
                mobileConfirmOrderLogger.info("请求腾讯发货返回数据 confirmOrder_resp：" + confirmOrder_resp);
            } catch (Exception e) {
                mobileConfirmOrderLogger.info("请求腾讯发货返回数据 confirmOrder_resp：" + confirmOrder_resp);
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

				mobileConfirmOrderLogger.info("请求腾讯发货返回失败：" + confirmOrder_resp_result.getString("msg"));
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



	public JSONObject removeWarnBuyVippkg(JSONObject req) throws Exception {
		MobileSourceComputeMode computeMode = mobileSourceComputeModeService.getBySource(req.getString("source"));

		String[] product_code_list = req.getString("vippkg").split("\\+");
		List<IptvProduct> iptvProductList = iptvProductService.getIptvProduct(product_code_list,
				req.getString("source"));
		StringBuilder vippkg = new StringBuilder();
		StringBuilder warmVippkg = new StringBuilder();
		for (int i = 0; i < iptvProductList.size(); i++) {
			boolean warnBuy=redisCache.setnxWithExptime("MobileOrder_"+req.getString("source")+"_"+req.getString("userId")+"_"+iptvProductList.get(i).getProductType(), "1", computeMode==null?10*24*60*60:computeMode.getWarnBuyDays()*24*60*60);
			if (warnBuy) {
				vippkg.append(iptvProductList.get(i).getProductCode() + "+");
			}else{
				warmVippkg.append(iptvProductList.get(i).getProductCode() + "+");
			}
		}
		if (vippkg.length() > 0) {
			req.put("vippkg", vippkg.deleteCharAt(vippkg.length() - 1).toString());
		} else {
			req.put("vippkg", null);
		}
		//异常产品码，写入确认表中--hgl-2020/11/03
		if (warmVippkg.length() > 0) {
			saveMobileOrderDetermine(req, warmVippkg.deleteCharAt(warmVippkg.length() - 1).toString());
		}
		return req;
	}

	public void saveMobileOrderDetermine(JSONObject req,String vippkg) throws Exception{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		MobileOrderInfoDetermine determine = new MobileOrderInfoDetermine();
		determine.setSource(req.getString("source"));
		determine.setVippkg(vippkg);
		determine.setExtTraceno(req.getString("ext_traceno"));
		determine.setUserid(req.getString("userId"));
		determine.setVuid(req.getString("vuid"));
		determine.setInnerPayTime(dateFormat.parse(req.getString("inner_pay_time")));
		determine.setIsAutopay(req.getString("is_autopay"));
		determine.setOrderType(BizConstant.Tencent.Order_Type_Bill);
		// mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());

		StringBuffer third_code = new StringBuffer();

		String[] product_code_list = vippkg.split("\\+");
		List<IptvProduct> iptvProductList = iptvProductService.getIptvProduct(product_code_list,
				req.getString("source"));
		if (null == iptvProductList || iptvProductList.size() < 1) {
			throw new BusinessException("产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！",BizConstant.Code.Result_Code_Fail_Num_1);
		}

		for (int i = 0; i < iptvProductList.size(); i++) {
			String third_vippkg = "";
			if (BizConstant.MobileUserProduct.IsAutoPay_No.equals(req.getString("is_autopay"))) {
				third_vippkg = iptvProductList.get(i).getThirdCode();
			} else {
				third_vippkg = iptvProductList.get(i).getPThirdCode();
			}
			if (i == 0) {
				third_code.append(third_vippkg);
			} else {
				third_code.append("+" + third_vippkg);
			}
		}
		determine.setThirdVippkg(third_code.toString());
		mobileOrderInfoDetermineService.save(determine);
	}

	public List<MobileOrderInfoDet> getOrderInfoDet(IptvProduct product, MobileOrderInfo orderInfo) throws Exception {
		// <--获取运营方权益计算规则
		MobileSourceComputeMode computeMode = mobileSourceComputeModeService.getBySource(orderInfo.getSource());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		List<MobileOrderInfoDet> list = new ArrayList<MobileOrderInfoDet>();
		Date feetime = orderInfo.getInnerPayTime();
		for (int i = 0; i < product.getMonths(); i++) {
			MobileOrderInfoDet orderInfoDet = new MobileOrderInfoDet();
			BeanUtils.copyProperties(orderInfo, orderInfoDet);
			orderInfoDet.setThirdVippkg(product.getTxThirdCode());
			orderInfoDet.setTraceno(mobileInterService.getLocalTraceno());
			orderInfoDet.setFeetime(feetime);
			orderInfoDet.setStatus(BizConstant.Tencent.OrderDet_Status_Create_Prepare);
			list.add(orderInfoDet);
			// 默认为自然月，否则为固定天数
			if (computeMode != null && BizConstant.Code.Order.Compute_Mode_Days.equals(computeMode.getComputeMode())) {
				feetime = DateUtil.addDay(feetime, computeMode.getDays());
			} else {
/*				List<Map> result = dbCenter.sqlListMap(
						"select ADDDATE('" + sdf.format(feetime) + "',INTERVAL 1 month) third_etime from dual", null);
				if (result.size() > 0) {
					feetime = sdf.parse(result.get(0).get("third_etime").toString());
				}*/
				feetime = DateUtil.addMonth(feetime, 1);
			}
		}
			return list;
	}


	/**
	 * 获取需要发货的子订单数据
	 * @param current_time
	 * @param nums
	 * @return
	 */
	public List<MobileOrderInfoDet> getMobileOrderInfoDetList(String current_time, int nums) {
		return mobileOrderInfoDetService.getDeliverGoodsList(current_time,nums);
	}

	/**
	 *
	 * @param mobileOrderInfoDet
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public JSONObject doAccountForMobileOrderInfoDet(MobileOrderInfoDet mobileOrderInfoDet) throws Exception {
		JSONObject resp = new JSONObject();

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
		SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		JSONObject req = new JSONObject();
		Date date = new Date();

		req.put("source", mobileOrderInfoDet.getSource());
		req.put("vippkg", mobileOrderInfoDet.getThirdVippkg());
		req.put("vuid", mobileOrderInfoDet.getVuid());
		req.put("userId", mobileOrderInfoDet.getUserid());
		req.put("ext_traceno", mobileOrderInfoDet.getTraceno());
		req.put("inner_pay_time", dateFormat1.format(date));

		String access_token = mobileInterService.getAccessToken();
        // 创建订单
		resp = createOrderByTencent(req, resp, mobileOrderInfoDet, access_token);

		if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
			// access_token失效，重新获取
			mobileConfirmOrderLogger.info("创建订单access_token失效，重新获取下单");
			access_token = mobileInterService.getAccessTokenFromTx();
			// 设置token的有效时长并存在redis中
			mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
			resp = createOrderByTencent(req, resp, mobileOrderInfoDet, access_token);
		}

		// 通知发货
		if (BizConstant.Code.Result_Code_Success_Num_0.equals(resp.getString("code"))) {
			// 睡眠两百毫秒，防止下单腾讯还没处理完成
			Thread.sleep(200);

			resp = confirmOrderByTencent(req, resp, mobileOrderInfoDet, access_token);

			if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
				// access_token失效，重新获取
				mobileConfirmOrderLogger.info("订单发货access_token失效，重新获取发货");
				access_token = mobileInterService.getAccessTokenFromTx();
				// 设置token的有效时长并存在redis中
				mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
				resp = confirmOrderByTencent(req, resp, mobileOrderInfoDet, access_token);
			}
		}
		// 记录库表数据
		mobileOrderInfoDetService.updateById(mobileOrderInfoDet);
		String successFlag = BizConstant.Tencent.OrderDet_Status_Pay_Success;
		if (!BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfoDet.getStatus())) {
			successFlag = BizConstant.Tencent.OrderDet_Status_Pay_Fail;
			// 下单出错，加入充实队列，并进行告警
			addResentQueue(mobileOrderInfoDet);
		}
		String orderInfoStatus = getMobileOrderInfoStatus(mobileOrderInfoDet.getExtTraceno(),
				mobileOrderInfoDet.getTraceno(), mobileOrderInfoDet.getSource(), successFlag);
		MobileOrderInfo parentMobileOrderInfo = getMobileOrderInfoByExtTraceno(mobileOrderInfoDet.getExtTraceno(),
				mobileOrderInfoDet.getSource());
		parentMobileOrderInfo.setStatus(orderInfoStatus);
        // 记录库表数据
		mobileOrderInfoService.updateById(parentMobileOrderInfo);

		// 更新订购关系表
		if (BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfoDet.getStatus())) {
			try {
				IptvProduct nextProduct = iptvProductService.getIptvProduct(mobileOrderInfoDet.getVippkg(),
						mobileOrderInfoDet.getSource(),BizConstant.Code.Order.Cooperator_Code_Tencent);
				MobileUserProduct mobileUserProduct = mobileUserProductServiceImpl.getMobileUserProductByProductType(
						mobileOrderInfoDet.getUserid(), mobileOrderInfoDet.getVuid(), nextProduct.getProductType(),
						mobileOrderInfoDet.getSource());
				Date d_stime = new Date(mobileUserProduct.getEtime().getTime());

				Integer product_duration = Integer.parseInt(nextProduct.getProductDuration());
				mobileUserProduct.setProductCode(nextProduct.getProductCode());
				mobileUserProduct.setEtime(DateUtil.addDay(mobileUserProduct.getEtime(), product_duration));
				mobileUserProduct = setThirdEtime(mobileUserProduct, mobileUserProduct.getThirdEtime(),
						mobileUserProduct.getSource());
				mobileUserProduct.setUpdateTime(new Date());
				mobileUserProduct.setFeemonth(dateFormat1.format(
						DateUtil.addMonth(dateFormat.parse(mobileUserProduct.getFeemonth()), product_duration / 31))
						.substring(0, 8).replace("-", ""));
				mobileUserProduct.setThirdCode(nextProduct.getThirdCode());
				//更新
				mobileUserProductServiceImpl.updateById(mobileUserProduct);
				//dbCenter.update(mobileUserProduct, MobileUserProduct.class.getName());

				// 订购关系明细表
				MobileUserProductDet mobileUserProductDet = new MobileUserProductDet();
				mobileUserProductDet.setUserId(mobileOrderInfoDet.getUserid());
				mobileUserProductDet.setVuid(mobileOrderInfoDet.getVuid());
				mobileUserProductDet.setExtTraceno(mobileOrderInfoDet.getExtTraceno());
				mobileUserProductDet.setProductType(mobileUserProduct.getProductType());
				mobileUserProductDet.setProductCode(mobileUserProduct.getProductCode());
				mobileUserProductDet.setpProductCode(mobileUserProduct.getPProductCode());
				mobileUserProductDet.setOrderId(mobileOrderInfoDet.getOrderId());
				mobileUserProductDet.setThirdCode(nextProduct.getThirdCode());
				mobileUserProductDet.setStime(d_stime);
				mobileUserProductDet.setEtime(mobileUserProduct.getEtime());
				mobileUserProductDet.setFeemonth(mobileUserProduct.getFeemonth());
				mobileUserProductDet.setCreateTime(date);
				mobileUserProductDet.setUpdateTime(date);
				mobileUserProductDet.setSource(mobileUserProduct.getSource());
				mobileUserProductDet.setIsAutopay(mobileUserProduct.getIsAutopay());
				mobileUserProductDet.setTraceno(mobileOrderInfoDet.getTraceno());
				// 记录库表数据
				mobileUserProductDetServiceImpl.save(mobileUserProductDet);
			}catch (Exception e){
				e.printStackTrace();
				log.error("发货成功，更新订购关系表出错：" + JSONObject.toJSONString(mobileOrderInfoDet) + e.getCause(), e);
			}
		}else{
			// 自动续费下单出错，进行告警
			httpUtilsException.httpUtilsIncidentPush(
					"渠道【" + mobileOrderInfoDet.getSource() + "】用户id【" + mobileOrderInfoDet.getUserid() + "】产品编码【"
							+ mobileOrderInfoDet.getThirdVippkg()+ "】自动续费下单发货失败",
					BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
            // 记录失败数据到库表
			MobileOrderFail mobileOrderFail = new MobileOrderFail();
			mobileOrderFail.setExtTraceno(mobileOrderInfoDet.getTraceno());
			mobileOrderFail.setInnerPayTime(mobileOrderInfoDet.getInnerPayTime());
			mobileOrderFail.setSource(mobileOrderInfoDet.getSource());
			mobileOrderFail.setIsAutopay(mobileOrderInfoDet.getIsAutopay());
			mobileOrderFail.setUserId(mobileOrderInfoDet.getUserid());
			mobileOrderFail.setVippkg(mobileOrderInfoDet.getVippkg());
			mobileOrderFail.setThirdVippkg(mobileOrderInfoDet.getThirdVippkg());
			mobileOrderFail.setVuid(mobileOrderInfoDet.getVuid());
			mobileOrderFail.setNums(1);
			mobileOrderFail.setCreateTime(date);
			mobileOrderFail.setUpdateTime(date);
			mobileOrderFail.setProductCode(mobileOrderInfoDet.getVippkg());
			mobileOrderFail.setIsAutoAccount(BizConstant.Code.Code_YES);
			mobileOrderFail.setTraceno(mobileOrderInfoDet.getTraceno());

			JSONObject resend_json = JSONObject.parseObject(JSONObject.toJSONString(mobileOrderFail));

			try {
				// 记录库表数据
				mobileOrderFailService.save(mobileOrderFail);
			} catch (Exception e) {
				log.error("如果请求腾讯下单失败,记录库表（mobile_order_fail）数据出错：" + resend_json + e.getCause(), e);
			}
			try {
				// 将数据放到redis集合
				String interval = sysConfig.getMobileOrderResend_Interval();
				if (StringUtils.isBlank(interval)) {
					// 10分钟 默认 单位为秒
					interval = "600";
				}
				long time = Long.valueOf(interval) * 1000;
				Date afterDate = new Date(new Date().getTime() + time);
				resend_json.put("sendtime", dateFormat.format(afterDate));
				resend_json.put("traceno", mobileOrderInfoDet.getTraceno());
				redisCache.putCache("MobileResendOrderData_" + mobileOrderFail.getExtTraceno(),
						JSONObject.toJSONString(resend_json));
				redisCache.zAdd("MobileOrderResendDequeueTask_SortSet", afterDate.getTime(),
						mobileOrderFail.getExtTraceno());
			} catch (Exception e) {
				log.error("[setResendDataToRedisList] 失败订单重发数据放进redis队列出错:" + JSONObject.toJSONString(resend_json));
			}
		}

		return resp;
	}

	/**
	 * 重新加入队列
	 * @param shipOrderInfo
	 */
	public void addResentQueue(MobileOrderInfoBase shipOrderInfo){
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// 下单出错，进行告警
		httpUtilsException.httpUtilsIncidentPush(
				"渠道【" + shipOrderInfo.getSource() + "】订单流水【" + shipOrderInfo.getExtTraceno() + "】下单发货失败："
						+ shipOrderInfo.getMsg(),
				BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);

		Date date = new Date();

		// 记录失败数据到库表
		MobileOrderFail mobileOrderFail = new MobileOrderFail();
		mobileOrderFail.setExtTraceno(shipOrderInfo.getExtTraceno());
		mobileOrderFail.setTraceno(shipOrderInfo.getTraceno());
		mobileOrderFail.setInnerPayTime(shipOrderInfo.getInnerPayTime());
		mobileOrderFail.setSource(shipOrderInfo.getSource());
		mobileOrderFail.setIsAutopay(shipOrderInfo.getIsAutopay());
		mobileOrderFail.setUserId(shipOrderInfo.getUserid());
		mobileOrderFail.setVippkg(shipOrderInfo.getVippkg());
		mobileOrderFail.setThirdVippkg(shipOrderInfo.getThirdVippkg());
		mobileOrderFail.setVuid(shipOrderInfo.getVuid());
		mobileOrderFail.setNums(1);
		mobileOrderFail.setCreateTime(date);
		mobileOrderFail.setUpdateTime(date);

		JSONObject resend_json = JSONObject.parseObject(JSONObject.toJSONString(mobileOrderFail));

		try {
			// 记录库表数据
			mobileOrderFailService.save(mobileOrderFail);
		} catch (Exception e) {
			log.error("如果请求腾讯下单失败,记录库表（mobile_order_fail）数据出错：" + resend_json + e.getCause(), e);
		}

		try {
            // 将数据放到redis集合
			String interval = sysConfig.getMobileOrderResend_Interval();
			if (StringUtils.isBlank(interval)) {
				// 30分钟 默认 单位为秒
				interval = "600";
			}
			long time = Long.valueOf(interval) * 1000;
			Date afterDate = new Date(new Date().getTime() + time);
			resend_json.put("sendtime", dateFormat.format(afterDate));
			redisCache.putCache("MobileResendOrderData_" + mobileOrderFail.getExtTraceno(),
					JSONObject.toJSONString(resend_json));
			redisCache.zAdd("MobileOrderResendDequeueTask_SortSet", afterDate.getTime(),
					mobileOrderFail.getExtTraceno());
		}catch (Exception e) {
			log.error("[setResendDataToRedisList] 失败订单重发数据放进redis队列出错:" + JSONObject.toJSONString(resend_json));
		}
	}


	/**
	 * 当前一笔子订单完成的前提下，确认父订单的发货状态。
	 * @param ext_traceno
	 * @param traceno
	 * @param source
	 * @param successFlag
	 * @return
	 */
	public String getMobileOrderInfoStatus(String ext_traceno, String traceno, String source, String successFlag) {
		List<MobileOrderInfoDet> mobileOrderInfoList = mobileOrderInfoDetService.getMobileOrderInfoStatus(ext_traceno,traceno,source);
		String status = successFlag;
		for (int i = 0; i < mobileOrderInfoList.size(); i++) {
			if (BizConstant.Tencent.OrderDet_Status_Create_Prepare.equals(mobileOrderInfoList.get(i).getStatus())) {
				status = BizConstant.Tencent.Order_Status_Pay_Shipping;
				break;
			}
			if (BizConstant.Tencent.OrderDet_Status_Create_Fail.equals(mobileOrderInfoList.get(i).getStatus())
					|| BizConstant.Tencent.OrderDet_Status_Pay_Fail.equals(mobileOrderInfoList.get(i).getStatus())) {
				status = BizConstant.Tencent.Order_Status_Pay_Fail;
			}
		}
		return status;
	}

    /**
     * 根据ext_traceno 获取订单的数据
     *
     * @param traceno
     * @return
     */
    public MobileOrderInfoDet getMobileOrderInfoDetByTraceno(String traceno, String source) {
        MobileOrderInfoDet orderInfoDet = mobileOrderInfoDetService.getMobileOrderInfoDetByTraceno(traceno,source);
        if (null != orderInfoDet ) {
            return orderInfoDet;
        } else {
            return null;
        }
    }

    /**
     * 根据ext_traceno 获取新流程订单的数据
     *
     * @param ext_traceno
     * @return
     */
    public MobileOrderInfoSingle getMobileOrderInfoSingleByExtTraceno(String ext_traceno, String source) {
        MobileOrderInfoSingle orderInfoSingle = mobileOrderInfoSingleServiceImpl.getMobileOrderInfoSingleByExtTraceno(ext_traceno,source);
        if (null != orderInfoSingle ) {
            return orderInfoSingle;
        } else {
            return null;
        }
    }

	/**
	 * 根据ext_traceno 与 source 获取订单的数据
	 * @param ext_traceno
	 * @param source
	 * @return
	 */
	public MobileOrderInfo getMobileOrderInfoByExtTraceno(String ext_traceno, String source) {
		MobileOrderInfo orderInfo = mobileOrderInfoService.getMobileOrderInfoByExtTraceno(ext_traceno,source);
		if (null != orderInfo ) {
			return orderInfo;
		} else {
			return null;
		}
	}

	public MobileUserProduct setThirdEtime(MobileUserProduct mobileUserProduct, Date sTime, String source)
			throws Exception {
		// <--获取运营方权益计算规则
		Map query = new HashMap();
		query.put("source", source);
		MobileSourceComputeMode computeMode = mobileSourceComputeModeService.getBySource(source);
		// 默认为自然月，否则为固定天数
		if (computeMode != null && BizConstant.Code.Order.Compute_Mode_Days.equals(computeMode.getComputeMode())) {
			mobileUserProduct.setThirdEtime(DateUtil.addDay(sTime, computeMode.getDays()));
		} else {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			log.info("传入原third_etime为：" + sdf.format(sTime));
			Date monthDate = DateUtil.addMonth(sTime,1);
			mobileUserProduct.setThirdEtime(monthDate);
		}
		return mobileUserProduct;
		// -->
	}


	@Transactional
	public JSONObject doAccountForMobileUserProduct(MobileUserProduct mobileUserProduct) throws Exception {
		JSONObject resp = new JSONObject();

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
		SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		IptvProduct nextProduct = iptvProductService.getIptvProduct(mobileUserProduct.getPProductCode(),
				mobileUserProduct.getSource(),BizConstant.Code.Order.Cooperator_Code_Tencent);
		if (null == nextProduct) {
			throw new Exception("Product_code为" + nextProduct.getProductCode() + "的续费产品不存在！");
		}

		JSONObject req = new JSONObject();
		Date date = new Date();
		String ext_traceno = RandomCode.getUUID();

		req.put("source", mobileUserProduct.getSource());
		req.put("vippkg", nextProduct.getProductCode());
		req.put("vuid", mobileUserProduct.getVuid());
		req.put("userId", mobileUserProduct.getUserId());
		req.put("ext_traceno", ext_traceno);
		req.put("inner_pay_time", dateFormat1.format(date));

		MobileOrderInfo mobileOrderInfo = new MobileOrderInfo();
		mobileOrderInfo.setExtTraceno(req.getString("ext_traceno"));
		mobileOrderInfo.setUserid(req.getString("userId"));
		mobileOrderInfo.setVuid(req.getString("vuid"));
		mobileOrderInfo.setVippkg(req.getString("vippkg"));
		mobileOrderInfo.setInnerPayTime(date);
		mobileOrderInfo.setSource(req.getString("source"));
		mobileOrderInfo.setStatus(BizConstant.Code.Result_Code_Success_Num_0);
		mobileOrderInfo.setThirdVippkg(nextProduct.getThirdCode());
		mobileOrderInfo.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_Yes);
		mobileOrderInfo.setOrderType(BizConstant.Tencent.Order_Type_Payment);
		mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());

		String access_token = mobileInterService.getAccessToken();
		// 创建订单
		resp = createOrderByTencent(req, resp, mobileOrderInfo, access_token);
		if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
			// access_token失效，重新获取
			mobileConfirmOrderLogger.info("创建订单access_token失效，重新获取下单");
			access_token = mobileInterService.getAccessTokenFromTx();
			// 设置token的有效时长并存在redis中
			mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
			resp = createOrderByTencent(req, resp, mobileOrderInfo, access_token);
		}

		// 通知发货
		if (BizConstant.Code.Result_Code_Success_Num_0.equals(resp.getString("code"))) {
			// 睡眠两百毫秒，防止下单腾讯还没处理完成
			Thread.sleep(200);
			resp = confirmOrderByTencent(req, resp, mobileOrderInfo, access_token);
			if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
				// access_token失效，重新获取
				mobileConfirmOrderLogger.info("订单发货access_token失效，重新获取发货");
				access_token = mobileInterService.getAccessTokenFromTx();
				// 设置token的有效时长并存在redis中
				mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
				resp = confirmOrderByTencent(req, resp, mobileOrderInfo, access_token);
			}
		}

		// 记录库表数据
		mobileOrderInfoService.save(mobileOrderInfo);

		// 更新订购关系表
		if (BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
			try {
				Date d_stime = new Date(mobileUserProduct.getEtime().getTime());
				Integer product_duration = Integer.parseInt(nextProduct.getProductDuration());
				mobileUserProduct.setProductCode(nextProduct.getProductCode());
				mobileUserProduct.setEtime(DateUtil.addDay(mobileUserProduct.getEtime(), product_duration));
				mobileUserProduct = setThirdEtime(mobileUserProduct, mobileUserProduct.getThirdEtime(),
						mobileUserProduct.getSource());
				mobileUserProduct.setUpdateTime(new Date());
				mobileUserProduct.setFeemonth(dateFormat1.format(
						DateUtil.addMonth(dateFormat.parse(mobileUserProduct.getFeemonth()), product_duration / 31))
						.substring(0, 8).replace("-", ""));
				mobileUserProduct.setThirdCode(nextProduct.getThirdCode());

				//更新
				mobileUserProductServiceImpl.updateById(mobileUserProduct);


				// 订购关系明细表
				MobileUserProductDet mobileUserProductDet = new MobileUserProductDet();
				mobileUserProductDet.setUserId(mobileOrderInfo.getUserid());
				mobileUserProductDet.setVuid(mobileOrderInfo.getVuid());
				mobileUserProductDet.setExtTraceno(ext_traceno);
				mobileUserProductDet.setProductType(mobileUserProduct.getProductType());
				mobileUserProductDet.setProductCode(mobileUserProduct.getProductCode());
				mobileUserProductDet.setpProductCode(mobileUserProduct.getPProductCode());
				mobileUserProductDet.setOrderId(mobileOrderInfo.getOrderId());
				mobileUserProductDet.setThirdCode(nextProduct.getThirdCode());
				mobileUserProductDet.setStime(d_stime);
				mobileUserProductDet.setEtime(mobileUserProduct.getEtime());
				mobileUserProductDet.setFeemonth(mobileUserProduct.getFeemonth());
				mobileUserProductDet.setCreateTime(date);
				mobileUserProductDet.setUpdateTime(date);
				mobileUserProductDet.setSource(mobileUserProduct.getSource());
				mobileUserProductDet.setIsAutopay(mobileUserProduct.getIsAutopay());
				mobileUserProductDet.setTraceno(mobileOrderInfo.getTraceno());

				// 记录库表数据
				mobileUserProductDetServiceImpl.save(mobileUserProductDet);

			}catch (Exception e){
				log.error("发货成功，更新订购关系表出错：" + JSONObject.toJSONString(mobileUserProduct) + e.getCause(), e);
			}
		} else {
			// 自动续费下单出错，进行告警
			addResentQueue(mobileOrderInfo);
		}
		return resp;

	}

	/**
	 * 新增或者续费产品
	 * @param userid
	 * @param vuid
	 * @param product_code
	 * @param source
	 * @return
	 * @throws Exception
	 */
	public JSONObject doProcessMobileUserProduct(String userid, String vuid, String product_code,
												 String is_autopay, String source, String ext_traceno, String order_id, String traceno, String cooperatorCode) throws Exception{

		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyyMM");

		IptvProduct iptvProduct=iptvProductService.getIptvProduct(product_code, source,cooperatorCode);
		if(null==iptvProduct){
			throw new Exception("根据product_code【"+product_code+"】没有找到对应的配置产品");
		}

		// 支付成功，新增或者更新订购关系表
//		MobileUserProduct mobileUserProduct =getMobileUserProduct(userid, vuid, product_code,source);
		MobileUserProductBase mobileUserProduct = null;
		if(BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)){
			mobileUserProduct =mobileUserProductServiceimpl.getMobileUserProductByProductType(userid, vuid, iptvProduct.getProductType(),source);
		}
		else if(BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)){
			mobileUserProduct =mobileUserProductYoukuService.getMobileUserProductByProductTypeYouku(userid, vuid, iptvProduct.getProductType(),source);
		} else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(cooperatorCode)) {
			mobileUserProduct =mobileUserProductAiqiyiService.getMobileUserProductByProductTypeAiqiyi(userid, vuid, iptvProduct.getProductType(),source);
		}else {
			return null;
		}
		if (null == mobileUserProduct) {
			try {
				// 第一次，新增
				if(BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)){
					mobileUserProduct = new MobileUserProduct();
				}
				else if(BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)){
					mobileUserProduct = new MobileUserProductYouku();
				} else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(cooperatorCode)) {
					mobileUserProduct = new MobileUserProductAiqiyi();
				}
				mobileUserProduct.setUserId(userid);
				mobileUserProduct.setCooperatorCode(cooperatorCode);
				mobileUserProduct.setVuid(vuid);
				mobileUserProduct.setProductType(iptvProduct.getProductType());
				mobileUserProduct.setProductCode(iptvProduct.getProductCode());
				if(BizConstant.MobileUserProduct.IsAutoPay_Yes.equals(is_autopay)){
					mobileUserProduct.setThirdCode(iptvProduct.getPThirdCode());
					mobileUserProduct.setPProductCode(iptvProduct.getPProductCode());
				}else{
					mobileUserProduct.setThirdCode(iptvProduct.getThirdCode());
				}
				mobileUserProduct.setSource(source);
				mobileUserProduct.setPstatus(BizConstant.MobileUserProduct.Pstatus_Useid);
				mobileUserProduct.setStime(date);
				Integer product_duration = Integer.valueOf(iptvProduct.getProductDuration());
				mobileUserProduct.setEtime(DateUtil.addDay(mobileUserProduct.getStime(), product_duration));
				String feetime = "";
				if (BizConstant.MobileUserProduct.IsAutoPay_Yes.equals(is_autopay)) {
					feetime = dateFormat.format(DateUtil.addMonth(date, product_duration / 31));
				} else {
					feetime = dateFormat.format(date);
				}
				mobileUserProduct.setFeemonth(feetime.substring(0, 8).replace("-", ""));
				mobileUserProduct.setIsAutopay(is_autopay);
				mobileUserProduct.setCreateTime(date);
				mobileUserProduct.setUpdateTime(date);

				//<--获取运营方权益计算规则
				mobileUserProduct.setThirdEtime(getThirdEtime(mobileUserProduct,mobileUserProduct.getStime(),source));
				//-->
				// 记录库表数据
				//dbCenter.save(mobileUserProduct, mobileUserProduct.getClass().getName());
				//TODO
				if(BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)){
					mobileUserProductYoukuService.save((MobileUserProductYouku) mobileUserProduct);
				} else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(cooperatorCode)) {
					mobileUserProductAiqiyiService.save((MobileUserProductAiqiyi) mobileUserProduct);
				} else {
					//腾讯
					mobileUserProductServiceimpl.save((MobileUserProduct)mobileUserProduct);
				}
				//订购关系明细表
				MobileUserProductDet mobileUserProductDet=new MobileUserProductDet();
				mobileUserProductDet.setUserId(userid);
				mobileUserProductDet.setVuid(vuid);
				mobileUserProductDet.setExtTraceno(ext_traceno);
				mobileUserProductDet.setOrderId(order_id);
				mobileUserProductDet.setProductCode(product_code);
				mobileUserProductDet.setProductType(iptvProduct.getProductType());
				mobileUserProductDet.setThirdCode(mobileUserProduct.getThirdCode());
				mobileUserProductDet.setpProductCode(mobileUserProduct.getPProductCode());
				mobileUserProductDet.setStime(mobileUserProduct.getStime());
				mobileUserProductDet.setEtime(mobileUserProduct.getEtime());
				mobileUserProductDet.setFeemonth(mobileUserProduct.getFeemonth());
				mobileUserProductDet.setCreateTime(date);
				mobileUserProductDet.setUpdateTime(date);
				mobileUserProductDet.setSource(source);
				mobileUserProductDet.setIsAutopay(mobileUserProduct.getIsAutopay());
				mobileUserProductDet.setTraceno(traceno);
				// 记录库表数据
				//dbCenter.save(mobileUserProductDet, MobileUserProductDet.class.getName());
				mobileUserProductDetService.save(mobileUserProductDet);

			} catch (Exception e) {
				log.error("下单发货保存订购关系表出错：" + JSONObject.toJSONString(mobileUserProduct) + e.getCause(), e);
			}
		} else {
			try {
				String feetime = "";
				Date dd_stime;
				mobileUserProduct.setProductCode(iptvProduct.getProductCode());
				mobileUserProduct.setPstatus(BizConstant.MobileUserProduct.Pstatus_Useid);
				if(BizConstant.MobileUserProduct.IsAutoPay_Yes.equals(is_autopay)){
					mobileUserProduct.setThirdCode(iptvProduct.getPThirdCode());
					mobileUserProduct.setIsAutopay(is_autopay);
					mobileUserProduct.setPProductCode(iptvProduct.getPProductCode());
				}else{
					mobileUserProduct.setThirdCode(iptvProduct.getThirdCode());
				}
				Integer product_duration = Integer.valueOf(iptvProduct.getProductDuration());
				if(DateTimeUtil.DateTimeCompare(mobileUserProduct.getEtime(), date)<=0){
					Date d_stime=new Date(date.getTime());
					dd_stime=d_stime;
					mobileUserProduct.setStime(date);
					mobileUserProduct.setEtime(DateUtil.addDay(date, product_duration));
					mobileUserProduct.setThirdEtime(getThirdEtime(mobileUserProduct,mobileUserProduct.getStime(),source));
					if (BizConstant.MobileUserProduct.IsAutoPay_Yes.equals(is_autopay)) {
						feetime = dateFormat.format(DateUtil.addMonth(date, product_duration / 31));
					} else {
						feetime = dateFormat.format(date);
					}
					mobileUserProduct.setFeemonth(feetime.substring(0, 8).replace("-", ""));
				}else{
					Date d_stime=new Date(mobileUserProduct.getEtime().getTime());
					dd_stime=d_stime;
					mobileUserProduct.setEtime(DateUtil.addDay(mobileUserProduct.getEtime(), product_duration));
					mobileUserProduct.setFeemonth(dateFormat1.format(DateUtil.addMonth(dateFormat1.parse(mobileUserProduct.getFeemonth()), product_duration/31)));
					mobileUserProduct.setThirdEtime(getThirdEtime(mobileUserProduct,mobileUserProduct.getThirdEtime(),source));
				}
				mobileUserProduct.setUpdateTime(date);
				// 记录库表数据
				//dbCenter.update(mobileUserProduct, mobileUserProduct.getClass().getName());
				//TODO
				//mobileUserProductService.saveOrUpdate((MobileUserProduct) mobileUserProduct);
				if(BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)){
					mobileUserProductYoukuService.save((MobileUserProductYouku) mobileUserProduct);
				} else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(cooperatorCode)) {
					mobileUserProductAiqiyiService.save((MobileUserProductAiqiyi) mobileUserProduct);
				} else {
					//腾讯
					mobileUserProductServiceimpl.save((MobileUserProduct)mobileUserProduct);
				}

				//订购关系明细表
				MobileUserProductDet mobileUserProductDet=new MobileUserProductDet();
				mobileUserProductDet.setUserId(userid);
				mobileUserProductDet.setVuid(vuid);
				mobileUserProductDet.setExtTraceno(ext_traceno);
				mobileUserProductDet.setOrderId(order_id);
				mobileUserProductDet.setProductCode(product_code);
				mobileUserProductDet.setProductType(iptvProduct.getProductType());
				mobileUserProductDet.setThirdCode(mobileUserProduct.getThirdCode());
				mobileUserProductDet.setpProductCode(mobileUserProduct.getPProductCode());
				mobileUserProductDet.setStime(dd_stime);
				mobileUserProductDet.setEtime(mobileUserProduct.getEtime());
				mobileUserProductDet.setFeemonth(mobileUserProduct.getFeemonth());
				mobileUserProductDet.setCreateTime(date);
				mobileUserProductDet.setSource(source);
				mobileUserProductDet.setUpdateTime(date);
				mobileUserProductDet.setIsAutopay(is_autopay);
				mobileUserProductDet.setTraceno(traceno);

				// 记录库表数据
				mobileUserProductDetService.save(mobileUserProductDet);

			} catch (Exception e) {
				log.error("下单发货更新订购关系表出错：" + JSONObject.toJSONString(mobileUserProduct) + e.getCause(), e);
			}

		}
		//移到接口进来的时候上锁
//		Map query = new HashMap();
//		query.put("source", source);
//		MobileSourceComputeMode computeMode = (MobileSourceComputeMode)dbCenter.queryOne(MobileSourceComputeMode.class.getName(), query);
//		redisCache.putCacheWithExpireTime("MobileOrder_"+source+"_"+userid+"_"+product_code, "1", computeMode==null?1*24*60*60:computeMode.getWarnBuyDays()*24*60*60);
		return null;
	}

	public Date getThirdEtime(MobileUserProductBase mobileUserProduct,Date sTime, String source) throws ParseException {
		//<--获取运营方权益计算规则
		MobileSourceComputeMode computeMode = mobileSourceComputeModeService.getBySource(source);

		//默认为自然月，否则为固定天数
		if(computeMode!=null&&BizConstant.Code.Order.Compute_Mode_Days.equals(computeMode.getComputeMode())){
			return DateUtil.addDay(sTime, computeMode.getDays());
		}else{
/*            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            List<Map> result = dbCenter.sqlListMap("select ADDDATE('" + sdf.format(sTime) + "',INTERVAL 1 month) third_etime from dual",null);
            if(result.size()>0){
                return sdf.parse(result.get(0).get("third_etime").toString());
            }*/
			return DateUtil.addMonth(sTime, 1);
		}
		//-->
	}

	public JSONObject cancelAccount(JSONObject req) throws Exception {

		JSONObject resp = new JSONObject();
		resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
		resp.put("msg", "取消续费成功！");
		String[] products = req.getString("vippkg").split("\\+");
		for (String product_code : products) {
			IptvProduct iptvProduct = iptvProductService.getIptvProduct(product_code, req.getString("source"), req.getString("cooperatorCode"));
			if (null == iptvProduct) {
				log.error("没有找到产品编码为" + product_code + "的产品包记录");
				throw new Exception("没有找到产品编码为" + product_code + "的产品包记录");
			}
			String cooperatorCode = req.getString("cooperatorCode");//tencent--腾讯 youku--优酷    不传则默认获取腾讯用户信息
			MobileUserProductBase mobileUserProduct = null;
			if (BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)) {
				mobileUserProduct = mobileUserProductServiceimpl.getMobileUserProductByProductType(req.getString("userId"), req.getString("vuid"),
						iptvProduct.getProductType(), req.getString("source"));
			} else if (BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)) {
				mobileUserProduct = mobileUserProductYoukuService.getMobileUserProductByProductTypeYouku(req.getString("userId"), req.getString("vuid"),
						iptvProduct.getProductType(), req.getString("source"));
			} else {
				throw new Exception("不支持的第三方视频平台编码：" + cooperatorCode);
			}

			if (null == mobileUserProduct) {
				log.error("没有找到用户id为" + req.getString("userId") + "，vuid为" + req.getString("vuid") + "正在续费的自动续费产品包"
						+ product_code + "的记录！");
				throw new Exception("没有找到用户id为" + req.getString("userId") + "，vuid为" + req.getString("vuid") + "正在续费的自动续费产品包"
						+ product_code + "的记录！");
			}

			if (BizConstant.MobileUserProduct.IsAutoPay_No.equals(mobileUserProduct.getIsAutopay())) {
				resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
				resp.put("msg", "取消续费成功！");
				return resp;
			}


			if (StringUtils.isNotBlank(cooperatorCode) && "youku".equals(cooperatorCode.trim())) {//只有优酷 才走的逻辑
				//优酷 文档规定产品包只能有一个，所以外循环只会循环一次
				String ext_traceno = req.getString("ext_traceno");//cooperatorCode=youku时必填，值为开通自动续费时的订单号
				if (StringUtils.isBlank(ext_traceno)) {
					resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
					resp.put("msg", "运营商平台订单号为空!");
					return resp;
				}

				MobileOrderInfoYouku mobileOrderInfo = mobileOrderInfoYoukuService.getProductByExtTraceno(ext_traceno);

				if (mobileOrderInfo == null) {
					resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
					resp.put("msg", "运营商平台订单不存在!");
					return resp;
				}
				Date date = new Date();//优酷的 取消续费时间
				String refundTime = DateUtil.DateToString(date, DateUtil.YYYY_MM_DD_HH_MM_SS);
				String orderId = mobileOrderInfo.getOrderId();//传给优酷的参数 订单号
				//String methodName = "ott.kitty.commonorder.sync";
                String methodName = sysConfig.getYOUKU_CONFIRM_ORDER_INTERFACE();
				Map<String, Object> requestParams = new HashMap<>();
				requestParams.put("orderId", orderId);//运营商订单id,最好是16位及以上唯一ID,比如 2019909023232320
				//充值的商品id（此商品需要事先给到优酷，并把商品的业务逻辑确定下来，比如是连续包月还是单月/单季/单年)
				if (BizConstant.MobileUserProduct.IsAutoPay_Yes.contains(mobileOrderInfo.getIsAutopay())) {
					requestParams.put("productId", iptvProduct.getPThirdCode());
				} else {
					requestParams.put("productId", iptvProduct.getThirdCode());
				}
				requestParams.put("syncTime", refundTime);//同步时间 格式yyyy-MM-dd HH:mm:ss 说明：如果是线上或线下订单此时间是用户支付成功时间，如果是退订则是退订时间
				requestParams.put("channelId", youKuRequstUtils.getChannelIdBySource(mobileOrderInfo.getSource()));//运营商渠道（需要找优酷方确认）
				requestParams.put("accountId", req.getString("vuid"));//运营商用户账号账号id
				requestParams.put("type", BizConstant.Code.Order.Order_Type_Youku_CancelRenew);//订单类型 1:线上支付订单(线上应用内购买), 2:线下支付订单(比如营业厅订单), 3:连续包取消续订, 4:全额退款(立即终止权益,不分产品包,不计财务), 5:续费(运营商侧发起时才使用),6:非连续包退订(按未使用天数退款)
//					requestParams.put("extInfo", "");//扩展字段，根据需要，约定具体的字段，json格式 如传节目信息 {"showId":"jjuiuisd"}
				ServerResponse<OttOpenapiResponse> result = youKuRequstUtils.doRequest(methodName, requestParams);
				log.info("******result*******" + result);
				int status = result.getStatus();
				if (status != 0) {
					resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
					resp.put("msg", "失败原因,[code:" + result.getData().getSubCode() + "],[msg:" + result.getData().getSubMsg() + "]");
					return resp;
				}
				//添加一条取消续费记录
				mobileUserProduct.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_No);
				mobileUserProduct.setUpdateTime(new Date());

				//TODO
				mobileUserProductYoukuService.saveOrUpdate((MobileUserProductYouku) mobileUserProduct);
				return resp;

			} else { //只有腾讯走的逻辑
//					Date thirdEtime = getThirdEtime(mobileUserProduct,mobileUserProduct.getThird_etime(),mobileUserProduct.getSource());
				//先计算运营商扣费时间
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
/*                Map query = new HashMap();
                query.put("source", req.getString("source"));
                MobileSourceComputeMode computeMode = (MobileSourceComputeMode)dbCenter.queryOne(MobileSourceComputeMode.class.getName(), query);*/
				MobileSourceComputeMode computeMode = mobileSourceComputeModeService.getBySource(req.getString("source"));

				Date rightsEtime = null;
				if (req.getString("rightsEtime") != null) {
					rightsEtime = sdfDateTime.parse(req.getString("rightsEtime").toString());
					//如取消自动续费时间点大于当前运营商扣费时间，则补发一个月权益
					if (DateTimeUtil.DateCompare(mobileUserProduct.getThirdEtime(), rightsEtime) < 0) {
/*                        Map queryAuto = new HashMap();
                        queryAuto.put("source", req.getString("source"));
                        MobileAutopayConfig config = (MobileAutopayConfig)dbCenter.queryOne(MobileAutopayConfig.class.getName(), query);*/
						MobileAutopayConfig config = mobileAutopayConfigService.getConfigBySource(req.getString("source"));

						if (mobileUserProduct.getIgnoreFlag() == null) {
							if ((config == null) || (config != null && config.getStatus() != 2)) {
								mobileReissueOrderLogger.info("开始取消自动续费补发，用户userId:" + mobileUserProduct.getUserId() + ",补发前运营商权益到期时间为:" + sdfDateTime.format(mobileUserProduct.getThirdEtime()) + ",传入权益到期时间:" + sdfDateTime.format(rightsEtime));
								mobileUserProduct = replenishmentDelivery(mobileUserProduct);
								mobileReissueOrderLogger.info("取消自动续费补发成功，用户userId:" + mobileUserProduct.getUserId() + ",补发后运营商权益到期时间为:" + sdfDateTime.format(mobileUserProduct.getThirdEtime()));
							}
						}
					} else {
						mobileReissueOrderLogger.info("无需取消自动续费补发，用户userId:" + mobileUserProduct.getUserId() + ",补发前运营商权益到期时间为:" + sdfDateTime.format(mobileUserProduct.getThirdEtime()) + ",取消自动续费时间:" + sdfDateTime.format(rightsEtime));
					}

					mobileUserProduct.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_No);
					mobileUserProduct.setUpdateTime(new Date());
					//dbCenter.update(mobileUserProduct, mobileUserProduct.getClass().getName());
					mobileUserProductServiceimpl.saveOrUpdate((MobileUserProduct) mobileUserProduct);
				} else {
//						rightsEtime = mobileUserProduct.getThird_etime();
//						String etimeStr = sdf.format(DateTimeUtil.addNday(rightsEtime, -1));
//						String deductionTimeStr = null;
//						if(computeMode.getDeductionTime()==null){
//							deductionTimeStr = "00:00:00";
//						}else{
//							deductionTimeStr = computeMode.getDeductionTime();
//						}
//						String deductionDateTimeStr =etimeStr+" "+deductionTimeStr;
					//如取消自动续费时间点大于当前运营商扣费时间，则补发一个月权益
					Date cancelDate = new Date();
					if (DateTimeUtil.DateCompare(mobileUserProduct.getThirdEtime(), cancelDate) < 0) {
/*                        Map queryAuto = new HashMap();
                        queryAuto.put("source", req.getString("source"));
                        MobileAutopayConfig config = (MobileAutopayConfig)dbCenter.queryOne(MobileAutopayConfig.class.getName(), query);*/
						MobileAutopayConfig config = mobileAutopayConfigService.getConfigBySource(req.getString("source"));

						if (mobileUserProduct.getIgnoreFlag() == null) {
							if ((config == null) || (config != null && config.getStatus() != 2)) {
								mobileReissueOrderLogger.info("开始取消自动续费补发，用户userId:" + mobileUserProduct.getUserId() + ",补发前运营商权益到期时间为:" + sdfDateTime.format(mobileUserProduct.getThirdEtime()) + ",取消自动续费时间:" + sdfDateTime.format(cancelDate));
								mobileUserProduct = replenishmentDelivery(mobileUserProduct);
								mobileReissueOrderLogger.info("取消自动续费补发成功，用户userId:" + mobileUserProduct.getUserId() + ",补发后运营商权益到期时间为:" + sdfDateTime.format(mobileUserProduct.getThirdEtime()));
							}
						}
					} else {
						mobileReissueOrderLogger.info("无需取消自动续费补发，用户userId:" + mobileUserProduct.getUserId() + ",补发前运营商权益到期时间为:" + sdfDateTime.format(mobileUserProduct.getThirdEtime()) + ",取消自动续费时间:" + sdfDateTime.format(cancelDate));
					}

					mobileUserProduct.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_No);
					mobileUserProduct.setUpdateTime(new Date());
					//dbCenter.update(mobileUserProduct, mobileUserProduct.getClass().getName());
					mobileUserProductServiceimpl.saveOrUpdate((MobileUserProduct) mobileUserProduct);
				}
			}

		}
		return resp;
	}

	//取消自动续费，补发货一个月
	public MobileUserProductBase replenishmentDelivery(MobileUserProductBase mobileUserProduct) throws Exception{
		JSONObject resp = new JSONObject();
		//运营商权益时间，大于腾讯权益时间，则需要补发一个月权益

		IptvProduct nextProduct = iptvProductService.getIptvProduct(mobileUserProduct.getPProductCode(),
				mobileUserProduct.getSource(),BizConstant.Code.Order.Cooperator_Code_Tencent);
		if (null == nextProduct) {
			throw new Exception("Product_code为" + nextProduct.getProductCode() + "的续费产品不存在！");
		}
		JSONObject param = new JSONObject();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
		SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String ext_traceno = RandomCode.getUUID();

		param.put("source", mobileUserProduct.getSource());
		param.put("vippkg", nextProduct.getProductCode());
		param.put("vuid", mobileUserProduct.getVuid());
		param.put("userId", mobileUserProduct.getUserId());
		param.put("ext_traceno", ext_traceno);
		param.put("inner_pay_time", dateFormat1.format(date));

		MobileOrderInfo mobileOrderInfo = new MobileOrderInfo();
		mobileOrderInfo.setExtTraceno(param.getString("ext_traceno"));
		mobileOrderInfo.setUserid(param.getString("userId"));
		mobileOrderInfo.setVuid(param.getString("vuid"));
		mobileOrderInfo.setVippkg(param.getString("vippkg"));
		mobileOrderInfo.setInnerPayTime(date);
		mobileOrderInfo.setSource(param.getString("source"));
		mobileOrderInfo.setStatus(BizConstant.Code.Result_Code_Success_Num_0);
		mobileOrderInfo.setThirdVippkg(nextProduct.getThirdCode());
		mobileOrderInfo.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_Yes);
		mobileOrderInfo.setOrderType(BizConstant.Tencent.Order_Type_Payment);
		mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());
		// 创建订单
		String access_token = mobileInterService.getAccessToken();
		resp = createOrderByTencent(param, resp, mobileOrderInfo, access_token);
		if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
			// access_token失效，重新获取
			mobileConfirmOrderLogger.info("取消续订补发，创建订单access_token失效，重新获取下单");
			access_token = mobileInterService.getAccessTokenFromTx();
			// 设置token的有效时长并存在redis中
			mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
			resp = createOrderByTencent(param, resp, mobileOrderInfo, access_token);
		}

		// 通知发货
		if (BizConstant.Code.Result_Code_Success_Num_0.equals(resp.getString("code"))) {
			// 睡眠两百毫秒，防止下单腾讯还没处理完成
			Thread.sleep(200);

			resp = confirmOrderByTencent(param, resp, mobileOrderInfo, access_token);
			if (BizConstant.Tencent.AccessToken_Invalid_Code.equals(resp.getString("code"))) {
				// access_token失效，重新获取
				mobileConfirmOrderLogger.info("取消续订补发，订单发货access_token失效，重新获取发货");
				access_token = mobileInterService.getAccessTokenFromTx();
				// 设置token的有效时长并存在redis中
				mobileInterService.resetTokenExpireTimeByRedis("TX_ACCESS_TOKEN", access_token);
				resp = confirmOrderByTencent(param, resp, mobileOrderInfo, access_token);
			}
		}

		// 更新订购关系表
		if (BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
			try {
				Date d_stime=new Date(mobileUserProduct.getEtime().getTime());
				Integer product_duration = Integer.parseInt(nextProduct.getProductDuration());
				mobileUserProduct.setProductCode(nextProduct.getProductCode());
				mobileUserProduct.setEtime(DateUtil.addDay(mobileUserProduct.getEtime(), product_duration));
				mobileUserProduct.setThirdEtime(getThirdEtime(mobileUserProduct,mobileUserProduct.getThirdEtime(),mobileUserProduct.getSource()));
				mobileUserProduct.setUpdateTime(new Date());
				mobileUserProduct.setFeemonth(dateFormat1.format(
								DateUtil.addMonth(dateFormat.parse(mobileUserProduct.getFeemonth()), product_duration / 31))
						.substring(0, 8).replace("-", ""));
				mobileUserProduct.setThirdCode(nextProduct.getThirdCode());

				//订购关系明细表
				MobileUserProductDet mobileUserProductDet=new MobileUserProductDet();
				mobileUserProductDet.setUserId(mobileOrderInfo.getUserid());
				mobileUserProductDet.setVuid(mobileOrderInfo.getVuid());
				mobileUserProductDet.setExtTraceno(ext_traceno);
				mobileUserProductDet.setProductType(mobileUserProduct.getProductType());
				mobileUserProductDet.setProductCode(mobileUserProduct.getProductCode());
				mobileUserProductDet.setpProductCode(mobileUserProduct.getPProductCode());
				mobileUserProductDet.setOrderId(mobileOrderInfo.getOrderId());
				mobileUserProductDet.setThirdCode(nextProduct.getThirdCode());
				mobileUserProductDet.setStime(d_stime);
				mobileUserProductDet.setEtime(mobileUserProduct.getEtime());
				mobileUserProductDet.setFeemonth(mobileUserProduct.getFeemonth());
				mobileUserProductDet.setCreateTime(date);
				mobileUserProductDet.setUpdateTime(date);
				mobileUserProductDet.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_No);
				mobileUserProductDet.setSource(mobileUserProduct.getSource());
				mobileUserProductDet.setTraceno(mobileOrderInfo.getTraceno());
				// 记录库表数据
				//dbCenter.save(mobileUserProductDet, MobileUserProductDet.class.getName());
				mobileUserProductDetService.saveOrUpdate(mobileUserProductDet);

				//dbCenter.save(mobileOrderInfo, MobileOrderInfo.class.getName());
				mobileOrderInfoService.saveOrUpdate(mobileOrderInfo);

			} catch (Exception e) {
				log.error("发货成功，更新订购关系表出错：" + JSONObject.toJSONString(mobileUserProduct)+e.getCause(),e);
			}
		}
		return mobileUserProduct;

	}

	public JSONObject youkuRefundOrder(JSONObject req) throws Exception {
		JSONObject resp = new JSONObject();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		IptvProduct iptvProduct = iptvProductService.getIptvProduct(req.getString("vippkg"),
				req.getString("source"),BizConstant.Code.Order.Cooperator_Code_Youku);
		if (null == iptvProduct) {
			resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
			resp.put("msg", "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
			return resp;
		}
		MobileOrderInfoYouku mobileOrderInfo = mobileOrderInfoYoukuService.getMobileOrderInfoYoukuByExtTraceno(req.getString("ext_traceno"),
				req.getString("source"));
		if(null == mobileOrderInfo){
			resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
			resp.put("msg", "运营商平台订单号为【" + req.getString("ext_traceno") + "】，渠道为【" + req.getString("source") + "】的订单不存在！");
			return resp;
		}

		resp = refundOrderByYouku(resp, mobileOrderInfo, iptvProduct);
		// 记录库表数据
		mobileOrderInfoYoukuService.saveOrUpdate(mobileOrderInfo);
		if(BizConstant.Youku.Order_Status_Pay_Refund.equals(mobileOrderInfo.getStatus())){
			// 更新订购关系表
//			mobileUserProductService.doProcessMobileUserProduct(mobileOrderInfo.getUserid(),
//					mobileOrderInfo.getVuid(), iptvProduct.getProduct_code(),
//					mobileOrderInfo.getIs_autopay(), mobileOrderInfo.getSource(),
//					mobileOrderInfo.getExt_traceno(), mobileOrderInfo.getOrder_id(),
//					mobileOrderInfo.getTraceno(),BizConstant.Code.Order.Cooperator_Code_Youku);
			MobileUserProductYouku mobileUserProduct =mobileUserProductYoukuService.getMobileUserProductByProductTypeYouku(mobileOrderInfo.getUserid(), mobileOrderInfo.getVuid(),
					iptvProduct.getProductType(),mobileOrderInfo.getSource());
			mobileUserProduct.setIsAutopay(BizConstant.MobileUserProduct.IsAutoPay_No);
			mobileUserProductYoukuService.saveOrUpdate(mobileUserProduct);
		}
		return resp;
	}

	public JSONObject refundOrderByYouku(JSONObject resp,MobileOrderInfoYouku mobileOrderInfo,IptvProduct product) throws Exception{
		try {
			resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
			resp.put("msg", "申请退款成功");

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			ServerResponse<OttOpenapiResponse> responese = null;
			Map<String,Object> requestParams = new HashMap<String,Object>();
			requestParams.put("orderId", mobileOrderInfo.getTraceno());
			if(BizConstant.MobileUserProduct.IsAutoPay_Yes.contains(mobileOrderInfo.getIsAutopay())){
				requestParams.put("productId", product.getPThirdCode());
			}else{
				requestParams.put("productId", product.getThirdCode());
			}
			Date syncTime = new Date();
			requestParams.put("syncTime", dateFormat.format(syncTime));
			//需修改，测试用
			requestParams.put("channelId", youKuRequstUtils.getChannelIdBySource(mobileOrderInfo.getSource()));
			requestParams.put("accountId", mobileOrderInfo.getVuid());
			requestParams.put("type", BizConstant.Code.Order.Order_Type_Youku_RefundRenew);
			try {
				//responese = youKuRequstUtils.doRequest("ott.kitty.commonorder.sync", requestParams);
                responese = youKuRequstUtils.doRequest(sysConfig.getYOUKU_CONFIRM_ORDER_INTERFACE(), requestParams);
			} catch (Exception e) {
				log.error("请求优酷确认订单接口失败：" + e.getCause(), e);
				resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
				resp.put("msg", "请求优酷确认订单接口失败!");
//				mobileOrderInfo.setStatus(BizConstant.Youku.Order_Status_Pay_Refund);
//				mobileOrderInfo.setMsg(resp.getString("msg"));
				return resp;
			}

			if (Constant.ServerResponseStatus.FAIL==responese.getStatus()) {
				log.error("请求优酷确认订单接口失败");
				resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
				resp.put("msg", "失败原因,[code:" + responese.getData().getSubCode() + "],[msg:" + responese.getData().getSubMsg() + "]");
//				mobileOrderInfo.setStatus(BizConstant.Youku.Order_Status_Pay_Fail);
//				mobileOrderInfo.setMsg(resp.getString("msg"));
				return resp;
			}
			mobileOrderInfo.setInnerRefundTime(syncTime);
			mobileOrderInfo.setStatus(BizConstant.Youku.Order_Status_Pay_Refund);
			mobileOrderInfo.setOrderId(mobileOrderInfo.getTraceno());
			return resp;

		} catch (Exception e) {
			log.error("系统出错：申请退款出错！" + e.getCause(), e);
			resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
			resp.put("msg", "系统出错：申请退款出错！");
			mobileOrderInfo.setStatus(BizConstant.Tencent.Order_Status_Create_Fail);
			mobileOrderInfo.setMsg(resp.getString("msg"));
		}
		return resp;
	}

	public JSONObject stopShip(String extTraceno, String source) {
		JSONObject resp = new JSONObject();
		// 判断是否是可以停止发货的订单
		/*String sql = "select * from mobile_order_info where  ext_traceno=? and source=?";*/
		MobileOrderInfo mobileOrderInfo = mobileOrderInfoService.getMobileOrderInfoByExtTraceno(extTraceno, source);
		if (mobileOrderInfo == null ) {
			resp.put("code", BizConstant.Code.Result_Order_Fail_999);
			resp.put("msg", "订单号不存在:" + extTraceno);
			log.info("*********stopShip*********订单号不存在:" + extTraceno);
			return resp;
		}
		String status = mobileOrderInfo.getStatus();
		if (StringUtils.isBlank(status)) {
			resp.put("code", BizConstant.Code.Result_Order_Fail_999);
			resp.put("msg", "订单状态为空!");
			log.info("*********stopShip*********订单状态为空!");
			return resp;
		}
		// 订单处理状态，0下单失败，1下单成功发货失败，2发货完成，3发货中,4停止发货
		if (!"3".equals(status.trim())) {// 状态为发货中才可以停止发货
			resp.put("code", BizConstant.Code.Result_Order_Fail_999);
			resp.put("msg", "该订单不能操作停止发货!");
			log.info("*********stopShip*********该订单不能操作停止发货!");
			return resp;
		}
		// 判断子订单是否存在未发货的
		// 订单处理状态，-1未发货，0下单失败，1下单成功发货失败，2发货完成，3不发货
		/*sql = "select * from mobile_order_info_det where  ext_traceno=? and source=? and status='-1' ";*/
		QueryWrapper<MobileOrderInfoDet> queryWrapper=new QueryWrapper();
		queryWrapper.eq("status",-1);
		queryWrapper.eq("source",source);
		queryWrapper.eq("ext_traceno",extTraceno);
		MobileOrderInfoDet mobileOrderInfoDet = mobileOrderInfoDetService.selectOne(queryWrapper);
		if (mobileOrderInfoDet == null ) {
			resp.put("code", BizConstant.Code.Result_Order_Fail_999);
			resp.put("msg", "该订单不存在未发货状态的子订单");
			log.info("*********stopShip*********该订单不存在未发货状态的子订单");
			return resp;
		}

		// 设置子订单的状态为不发货
		/*sql = "update mobile_order_info_det set status = '3' where status='-1' and ext_traceno=? and source=?";*/
		MobileOrderInfoDet entity=new MobileOrderInfoDet();
		entity.setStatus("3");
		UpdateWrapper<MobileOrderInfoDet> warp=new UpdateWrapper<>();
		warp.eq("ext_traceno",extTraceno);
		warp.eq("source",source);
		warp.eq("status","-1");
		int update = mobileOrderInfoDetService.updateByWrapper(entity, warp);
		/*	int update = dbCenter.sqlUpdate(MobileOrderInfoDet.class.getName(), sql, extTraceno, source);*/
		log.info("*********stopShip*********停止发货成功,mobile_order_info_det更新条数:" + update);

		// 设置订单的状态为停止发货
		/*sql = "update mobile_order_info set status = '4' where status='3' and ext_traceno=? and source=?";*/// 停止发货
		MobileOrderInfo entityMobileOrderInfo=new MobileOrderInfo();
		entityMobileOrderInfo.setStatus("4");
		UpdateWrapper<MobileOrderInfo> wrapper=new UpdateWrapper();
		wrapper.eq("status","3");
		wrapper.eq("ext_traceno",extTraceno);
		wrapper.eq("source",source);
		int moCount = mobileOrderInfoService.updateByWrapper(entityMobileOrderInfo, wrapper);
		/*int moCount = dbCenter.sqlUpdate(MobileOrderInfo.class.getName(), sql, extTraceno, source);*/
		log.info("*********stopShip*********停止发货成功,mobile_order_info更新条数:" + moCount);

		resp.put("code", BizConstant.Code.Result_Order_Success_000);
		resp.put("msg", "停止发货成功");
		return resp;
	}

	public JSONObject youkuConfirmOrder(JSONObject req) throws Exception {
		JSONObject resp = new JSONObject();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		IptvProduct iptvProduct = iptvProductService.getIptvProduct(req.getString("vippkg"),
				req.getString("source"),BizConstant.Code.Order.Cooperator_Code_Youku);
		if (null == iptvProduct) {
			resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
			resp.put("msg", "产品编码为【" + req.getString("vippkg") + "】，渠道为【" + req.getString("source") + "】的产品不存在！");
			return resp;
		}
		MobileOrderInfoYouku mobileOrderInfo = mobileOrderInfoYoukuService.getMobileOrderInfoYoukuByExtTraceno(req.getString("ext_traceno"),
				req.getString("source"));

		if (null != mobileOrderInfo) {
			if (BizConstant.Tencent.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())) {
				resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
				resp.put("msg", "订单发货成功");
				return resp;
			} else {
				resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
				resp.put("msg", "请求优酷发货返回失败!");
				return resp;
			}

		} else {
			mobileOrderInfo = new MobileOrderInfoYouku();
		}
		if (StringUtils.isBlank(req.getString("is_autopay"))) {
			req.put("is_autopay", BizConstant.MobileUserProduct.IsAutoPay_No);
		}

		mobileOrderInfo.setExtTraceno(req.getString("ext_traceno"));
		mobileOrderInfo.setUserid(req.getString("userId"));
		mobileOrderInfo.setVuid(req.getString("vuid"));
		mobileOrderInfo.setVippkg(req.getString("vippkg"));
		mobileOrderInfo.setInnerPayTime(dateFormat.parse(req.getString("inner_pay_time")));
		mobileOrderInfo.setSource(req.getString("source"));
		mobileOrderInfo.setStatus(BizConstant.Code.Result_Code_Success_Num_0);
		mobileOrderInfo.setIsAutopay(req.getString("is_autopay"));
		mobileOrderInfo.setOrderType(BizConstant.Tencent.Order_Type_Bill);
		mobileOrderInfo.setThirdVippkg(req.getString("vippkg"));
		mobileOrderInfo.setTraceno(mobileInterService.getLocalTraceno());
		resp = confirmOrderByYouku(resp, mobileOrderInfo,iptvProduct);
		// 记录库表数据
		//dbCenter.save(mobileOrderInfo, MobileOrderInfoYouku.class.getName());
		mobileOrderInfoYoukuService.saveOrUpdate(mobileOrderInfo);

		if(BizConstant.Youku.Order_Status_Pay_Success.equals(mobileOrderInfo.getStatus())){

			resp.put("vuid", mobileOrderInfo.getVuid());
			resp.put("order_createtime", mobileOrderInfo.getInnerPayTime().getTime() / 1000);
			resp.put("order_id", mobileOrderInfo.getOrderId());
			resp.put("order_price", iptvProduct.getPrice());
			resp.put("order_status", BizConstant.Tencent.Tx_Order_Status_Shipped);
			Set products = new HashSet();
			products.add(mobileOrderInfo.getThirdVippkg());
			resp.put("service", JSON.toJSONString(products));

			// 更新订购关系表
			doProcessMobileUserProduct(mobileOrderInfo.getUserid(),
					mobileOrderInfo.getVuid(), iptvProduct.getProductCode(),
					mobileOrderInfo.getIsAutopay(), mobileOrderInfo.getSource(),
					mobileOrderInfo.getExtTraceno(), mobileOrderInfo.getOrderId(),
					mobileOrderInfo.getTraceno(),BizConstant.Code.Order.Cooperator_Code_Youku);
		}else{
			addResentQueue(mobileOrderInfo);
		}
		return resp;
	}

	public JSONObject confirmOrderByYouku(JSONObject resp,MobileOrderInfoBase mobileOrderInfo,IptvProduct product) throws Exception{
		try {
			resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
			resp.put("msg", "订单发货成功");

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			ServerResponse<OttOpenapiResponse> responese = null;
			Map<String,Object> requestParams = new HashMap<String,Object>();
			requestParams.put("orderId", mobileOrderInfo.getTraceno());
			if(BizConstant.MobileUserProduct.IsAutoPay_Yes.contains(mobileOrderInfo.getIsAutopay())){
				requestParams.put("productId", product.getPThirdCode());
			}else{
				String third_code = product.getThirdCode();
				if(StringUtils.isBlank(third_code)){
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
				addResentSingleOrderQueue(mobileOrderInfo,BizConstant.Code.Order.Cooperator_Code_Youku);
				return resp;
			}

			if (Constant.ServerResponseStatus.FAIL==responese.getStatus()) {
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

    /**
     * 获取当天最后一秒的时间
     * @param current
     * @return
     */
    public long getEndOfDate(Long current){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(new Date(current));
        dateStr = dateStr.subSequence(0, 10)+" 23:59:59";
        try {
            return sdf.parse(dateStr).getTime()/1000;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return current/1000;
        }
    }

	public void addResentSingleOrderQueue(MobileOrderInfoBase shipOrderInfo,String source){
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// 下单出错，进行告警
		httpUtilsException.httpUtilsIncidentPush(
				"渠道【" + shipOrderInfo.getSource() + "】订单流水【" + shipOrderInfo.getExtTraceno() + "】下单发货失败："
						+ shipOrderInfo.getMsg(),
				BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);

		Date date = new Date();
		// 记录失败数据到库表
		MobileOrderFail mobileOrderFail = new MobileOrderFail();
		mobileOrderFail.setExtTraceno(shipOrderInfo.getExtTraceno());
		mobileOrderFail.setTraceno(shipOrderInfo.getTraceno());
		mobileOrderFail.setInnerPayTime(shipOrderInfo.getInnerPayTime());
		mobileOrderFail.setSource(shipOrderInfo.getSource());
		mobileOrderFail.setIsAutopay(shipOrderInfo.getIsAutopay());
		mobileOrderFail.setUserId(shipOrderInfo.getUserid());
		mobileOrderFail.setVippkg(shipOrderInfo.getVippkg());
		mobileOrderFail.setThirdVippkg(shipOrderInfo.getThirdVippkg());
		mobileOrderFail.setVuid(shipOrderInfo.getVuid());
		mobileOrderFail.setNums(1);
		mobileOrderFail.setCreateTime(date);
		mobileOrderFail.setUpdateTime(date);

		JSONObject resend_json = JSONObject.parseObject(JSONObject.toJSONString(mobileOrderFail));

		try {
			// 记录库表数据
			//dbCenter.save(mobileOrderFail, MobileOrderFail.class.getName());
			mobileOrderFailService.saveOrUpdate(mobileOrderFail);
		} catch (Exception e) {
			log.error("如果请求腾讯下单失败,记录库表（mobile_order_fail）数据出错：" + resend_json + e.getCause(), e);
		}

		try {
			// 将数据放到redis集合
			String interval = sysConfig.getMobileOrderResend_Interval();
			if (StringUtils.isBlank(interval)) {
				// 30分钟 默认 单位为秒
				interval = "600";
			}
			long time = Long.valueOf(interval) * 1000;
			Date afterDate = new Date(new Date().getTime() + time);
			resend_json.put("sendtime", dateFormat.format(afterDate));
			if(BizConstant.Code.Order.Cooperator_Code_Youku.equals(source)){
				resend_json.put("cooperatorCode", "youku");
			}
			if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(source)){
				resend_json.put("cooperatorCode", "aiqiyi");
			}
			redisCache.putCache("MobileResendSingleOrderData_" + mobileOrderFail.getExtTraceno(),
					JSONObject.toJSONString(resend_json));
			redisCache.zAdd("MobileOrderSingleResendDequeueTask_SortSet", afterDate.getTime(),
					mobileOrderFail.getExtTraceno());
		} catch (Exception e) {
			log.error("[setSingleOrderResendDataToRedisList] 失败单包月订单重发数据放进redis队列出错:" + JSONObject.toJSONString(resend_json));
		}
	}

}
