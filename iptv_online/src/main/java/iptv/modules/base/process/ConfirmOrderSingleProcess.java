package iptv.modules.base.process;

import com.alibaba.fastjson.JSONObject;
import iptv.common.BusinessException;
import iptv.common.CheckUtils;
import iptv.config.redis.RedisCache;
import iptv.modules.aiqiyi.entity.db.MobileUserProductAiqiyi;
import iptv.modules.aiqiyi.service.impl.MobileUserProductAiqiyiServiceImpl;
import iptv.modules.base.entity.db.*;
import iptv.modules.base.entity.vo.MobileUserProductBase;
import iptv.modules.base.service.impl.*;
import iptv.modules.tx.entity.db.MobileUserProduct;
import iptv.modules.tx.service.impl.MobileInterService;
import iptv.modules.tx.service.impl.MobileUserProductServiceImpl;
import iptv.modules.youku.entity.db.MobileUserProductYouku;
import iptv.modules.youku.service.impl.MobileUserProductYoukuServiceImpl;
import iptv.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author wyq
 * @create 2022/3/18 11:48
 */
public abstract class ConfirmOrderSingleProcess {

    @Autowired
    protected MobileUserProductServiceImpl mobileUserProductService;

    @Autowired
    protected MobileUserProductYoukuServiceImpl mobileUserProductYoukuService;

    @Autowired
    protected MobileUserProductAiqiyiServiceImpl mobileUserProductAiqiyiService;

    @Autowired
    protected IptvProductServiceImpl iptvProductService;

    @Autowired
    protected MobileOrderInfoSingleServiceImpl mobileOrderInfoSingleService;

    @Autowired
    protected MobileInterService mobileInterService;

    @Autowired
    protected MobileUserProductDetServiceImpl mobileUserProductDetService;

    @Autowired
    protected MobileOrderInfoDetServiceImpl mobileOrderInfoDetService;

    @Autowired
    protected HttpUtilsException httpUtilsException;

    @Autowired
    protected SysConfig sysConfig;

    @Autowired
    protected RedisCache redisCache;

    @Autowired
    protected SysBaseUtil sysBaseUtil;

    @Autowired
    protected MobileOrderFailServiceImpl mobileOrderFailService;

    @Autowired
    private MobileSourceComputeModeServiceImpl mobileSourceComputeModeService;


    protected Logger logger = LoggerFactory.getLogger("ConfirmOrderSingleProcess");
    protected Logger log = LoggerFactory.getLogger(ConfirmOrderSingleProcess.class);


    /**
     * ??????????????????
     *
     * @param req
     * @return
     */
    public abstract JSONObject confirmOrderSingle(JSONObject req, JSONObject resp);

    /**
     * ?????????????????????
     * @param req
     * @param resp
     * @param mobileOrderInfo
     * @param iptvProduct
     * @return
     */
    public abstract JSONObject confirmOrder(JSONObject req, JSONObject resp, MobileOrderInfoSingle mobileOrderInfo, IptvProduct iptvProduct) throws Exception;

    /**
     * ????????????
     *
     * @param req
     * @throws Exception
     */
    public void checkBaseReparam(JSONObject req) throws Exception {
        CheckUtils.checkEmpty(req.getString("source"), "?????????????????????????????????-???????????????source???",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("vippkg"), "?????????????????????????????????-???????????????id???vippkg???",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("vuid"), "?????????????????????????????????-???????????????id???vuid???",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("userId"), "?????????????????????????????????-??????id???userId???",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("ext_traceno"), "?????????????????????????????????-?????????????????????ext_traceno???",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("inner_pay_time"), "?????????????????????????????????-?????????????????????inner_pay_time???",
                BizConstant.Code.Missing_Parameter);
    }

    /**
     * ?????????????????????????????????
     * @param req
     * @param resp
     * @return
     * @throws Exception
     */
    protected JSONObject getVipInfo(JSONObject req, JSONObject resp) throws Exception {
        //??????????????????????????????????????????????????????????????????????????????
        ignoreUserProduct(req);

        //?????????????????????????????????
        JSONObject vipInfo = localVipInfo(req);

        return vipInfo;
    }

    /**
     * ?????????????????????????????????
     *
     * @param req
     * @return
     */
    protected JSONObject localVipInfo(JSONObject req) throws Exception {
        JSONObject resp = new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");
        resp.put("vuid", req.getString("vuid"));
        IptvProduct iptvProduct = iptvProductService.getIptvProduct(req.getString("vippkg"),
                req.getString("source"), req.getString("cooperatorCode"));
        if (null == iptvProduct) {
            throw new BusinessException(req.getString("cooperatorCode")+"??????product_code???" + req.getString("vippkg") + "???, souce???"+req.getString("source")+"????????????????????????????????????");
        }
        Date maxEtime = null;
        if (BizConstant.Code.Order.Cooperator_Code_Tencent.equals(req.getString("cooperatorCode"))) {
            maxEtime = mobileUserProductService.getVuidEtime(req.getString("vuid"), req.getString("source"), iptvProduct.getProductType());
        } else if (BizConstant.Code.Order.Cooperator_Code_Youku.equals(req.getString("cooperatorCode"))) {
            maxEtime = mobileUserProductYoukuService.getVuidEtime(req.getString("vuid"), req.getString("source"), iptvProduct.getProductType());
        } else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(req.getString("cooperatorCode"))) {
            maxEtime = mobileUserProductAiqiyiService.getVuidEtime(req.getString("vuid"), req.getString("source"), iptvProduct.getProductType());
        } else {
            return null;
        }
        /**
         * ???????????????????????????????????????????????????????????????23:59:59???????????????????????????????????????
         */
        if (maxEtime != null) {
            if (BizConstant.Code.Order.Cooperator_Code_Tencent.equals(req.getString("cooperatorCode"))
                    || BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(req.getString("cooperatorCode"))) {
                if (DateUtil.getEndOfDate(maxEtime.getTime()) < (new Date()).getTime() / 1000) {
                    resp.put("is_vip", 0);
                } else {
                    resp.put("is_vip", 1);
                    resp.put("end", maxEtime.getTime() / 1000);
                }
            } else if (BizConstant.Code.Order.Cooperator_Code_Youku.equals(req.getString("cooperatorCode"))) {
                if (maxEtime.getTime() / 1000 < (new Date()).getTime() / 1000) {
                    resp.put("is_vip", 0);
                } else {
                    resp.put("is_vip", 1);
                    resp.put("end", maxEtime.getTime() / 1000);
                }
            }

        } else {
            resp.put("is_vip", 0);
        }

        return resp;
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????
     *
     * @param req
     */
    protected void ignoreUserProduct(JSONObject req) {
        MobileUserProduct mobileUserProduct = mobileUserProductService.getMobileUserProductByVuid(req.getString("vuid"), req.getString("vippkg"), req.getString("source"));
        if (mobileUserProduct != null) {
            mobileUserProduct.setIgnoreFlag(1);
            mobileUserProductService.updateEntity(mobileUserProduct);
        }
    }

    /**
     * ??????????????????????????????resp
     *
     * @param resp
     * @param vipInfo
     * @param orderSuccess
     * @param iptvProduct
     * @return
     */
    protected JSONObject analyzEndTime(JSONObject resp, JSONObject vipInfo, boolean orderSuccess, IptvProduct iptvProduct) {
        if (orderSuccess) {
            if (vipInfo.containsKey("is_vip") && Integer.valueOf(vipInfo.get("is_vip").toString()) == 1) {
                Long oldEndTime = Long.valueOf(vipInfo.get("end").toString());
                Long newEndTime = oldEndTime + Integer.parseInt(iptvProduct.getProductDuration()) * 24 * 60 * 60;
                resp.put("vip_endtime", newEndTime);
                return resp;
            } else {
                Long newEndTime = System.currentTimeMillis() / 1000 + Integer.parseInt(iptvProduct.getProductDuration()) * 24 * 60 * 60;
                resp.put("vip_endtime", newEndTime);
                return resp;
            }
        } else {
            if (vipInfo.containsKey("is_vip") && Integer.valueOf(vipInfo.get("is_vip").toString()) == 1) {
                resp.put("vip_endtime", vipInfo.get("end"));
                return resp;
            } else {
                resp.put("vip_endtime", System.currentTimeMillis() / 1000 + Integer.parseInt(iptvProduct.getProductDuration()) * 24 * 60 * 60);
                return resp;
            }
        }
    }

    /**
     * ????????????????????????????????????
     * @param shipOrderInfo
     * @param source
     */
    protected void addResentSingleOrderQueue(MobileOrderInfoSingle shipOrderInfo, String source) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // ???????????????????????????
        httpUtilsException.httpUtilsIncidentPush(
                "?????????" + shipOrderInfo.getSource() + "??????????????????" + shipOrderInfo.getExtTraceno() + "????????????????????????"
                        + shipOrderInfo.getMsg(),
                BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);

        Date date = new Date();
        // ???????????????????????????
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
            // ??????????????????
            mobileOrderFailService.save(mobileOrderFail);
        } catch (Exception e) {
            log.error("??????????????????????????????,???????????????mobile_order_fail??????????????????" + resend_json + e.getCause(), e);
        }

        try {
            // ???????????????redis??????
            String interval = sysConfig.getMobileOrderResend_Interval();
            if (StringUtils.isBlank(interval)) {
                // 30?????? ?????? ????????????
                interval = "600";
            }
            long time = Long.valueOf(interval) * 1000;
            Date afterDate = new Date(System.currentTimeMillis() + time);
            resend_json.put("sendtime", dateFormat.format(afterDate));
            if (BizConstant.Code.Order.Cooperator_Code_Tencent.equals(source)) {
                resend_json.put("cooperatorCode", "tencent");
            }
            if (BizConstant.Code.Order.Cooperator_Code_Youku.equals(source)) {
                resend_json.put("cooperatorCode", "youku");
            }
            if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(source)) {
                resend_json.put("cooperatorCode", "aiqiyi");
            }
            redisCache.putCache("MobileResendSingleOrderData_" + mobileOrderFail.getExtTraceno(),
                    JSONObject.toJSONString(resend_json));
            redisCache.zAdd("MobileOrderSingleResendDequeueTask_SortSet", afterDate.getTime(),
                    mobileOrderFail.getExtTraceno());
        } catch (Exception e) {
            log.error("[setSingleOrderResendDataToRedisList] ???????????????????????????????????????redis????????????:" + JSONObject.toJSONString(resend_json));
        }
    }


    public void doProcessMobileUserProductSingle(String userid, String vuid, String productCode, String isAutopay, String source, String extTraceno, String orderId, String traceno, String cooperatorCode) throws Exception {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyyMM");

        IptvProduct iptvProduct = iptvProductService.getIptvProduct(productCode, source, cooperatorCode);
        if (null == iptvProduct) {
            throw new BusinessException("??????product_code???" + productCode + "???, souce???"+source+"????????????????????????????????????");
        }

        // ????????????????????????????????????????????????
//		MobileUserProduct mobileUserProduct =getMobileUserProduct(userid, vuid, product_code,source);
        MobileUserProductBase mobileUserProduct = null;
        if (BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)) {
            mobileUserProduct = mobileUserProductService.getMobileUserProductByProductType(userid, vuid, iptvProduct.getProductType(), source);
        } else if (BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)) {
            mobileUserProduct = mobileUserProductYoukuService.getMobileUserProductByProductTypeYouku(userid, vuid, iptvProduct.getProductType(), source);
        } else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(cooperatorCode)) {
            mobileUserProduct = mobileUserProductAiqiyiService.getMobileUserProductByProductTypeAiqiyi(userid, vuid, iptvProduct.getProductType(), source);
        } else {
            return;
        }
        if (null == mobileUserProduct) {
            try {
                // ??????????????????
                if (BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)) {
                    mobileUserProduct = new MobileUserProduct();
                } else if (BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)) {
                    mobileUserProduct = new MobileUserProductYouku();
                } else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(cooperatorCode)) {
                    mobileUserProduct = new MobileUserProductAiqiyi();
                }
                //???????????????
                mobileUserProduct.setIgnoreFlag(1);
                mobileUserProduct.setUserId(userid);
                mobileUserProduct.setCooperatorCode(cooperatorCode);
                mobileUserProduct.setVuid(vuid);
                mobileUserProduct.setProductType(iptvProduct.getProductType());
                mobileUserProduct.setProductCode(iptvProduct.getProductCode());
                if (BizConstant.MobileUserProduct.IsAutoPay_Yes.equals(isAutopay)) {
                    mobileUserProduct.setThirdCode(iptvProduct.getPThirdCode());
                    mobileUserProduct.setPProductCode(iptvProduct.getPProductCode());
                } else {
                    mobileUserProduct.setThirdCode(iptvProduct.getThirdCode());
                }
                mobileUserProduct.setSource(source);
                mobileUserProduct.setPstatus(BizConstant.MobileUserProduct.Pstatus_Useid);
                mobileUserProduct.setStime(date);
                Integer product_duration = Integer.valueOf(iptvProduct.getProductDuration());
                mobileUserProduct.setEtime(DateUtil.addDay(mobileUserProduct.getStime(), product_duration));
                String feetime = "";
                if (BizConstant.MobileUserProduct.IsAutoPay_Yes.equals(isAutopay)) {
                    feetime = dateFormat.format(DateUtil.addMonth(date, product_duration / 31));
                } else {
                    feetime = dateFormat.format(date);
                }
                mobileUserProduct.setFeemonth(feetime.substring(0, 8).replace("-", ""));
                mobileUserProduct.setIsAutopay(isAutopay);
                mobileUserProduct.setCreateTime(date);
                mobileUserProduct.setUpdateTime(date);

                //<--?????????????????????????????????
                mobileUserProduct.setThirdEtime(getThirdEtime(mobileUserProduct, mobileUserProduct.getStime(), source));
                //-->
                // ??????????????????
                if (BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)) {
                    mobileUserProductService.save((MobileUserProduct) mobileUserProduct);
                } else if (BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)) {
                    mobileUserProductYoukuService.save((MobileUserProductYouku) mobileUserProduct);
                } else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(cooperatorCode)) {
                    mobileUserProductAiqiyiService.save((MobileUserProductAiqiyi) mobileUserProduct);
                }

                //?????????????????????
                MobileUserProductDet mobileUserProductDet = new MobileUserProductDet();
                mobileUserProductDet.setUserId(userid);
                mobileUserProductDet.setVuid(vuid);
                mobileUserProductDet.setExtTraceno(extTraceno);
                mobileUserProductDet.setOrderId(orderId);
                mobileUserProductDet.setProductCode(productCode);
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
                // ??????????????????
                mobileUserProductDetService.save(mobileUserProductDet);

            } catch (Exception e) {
                log.error("??????????????????????????????????????????" + JSONObject.toJSONString(mobileUserProduct) + e.getCause(), e);
            }
        } else {
            try {
                String feetime = "";
                Date dd_stime;
                //???????????????
                mobileUserProduct.setIgnoreFlag(1);
                mobileUserProduct.setProductCode(iptvProduct.getProductCode());
                mobileUserProduct.setPstatus(BizConstant.MobileUserProduct.Pstatus_Useid);
                if (BizConstant.MobileUserProduct.IsAutoPay_Yes.equals(isAutopay)) {
                    mobileUserProduct.setThirdCode(iptvProduct.getPProductCode());
                    mobileUserProduct.setIsAutopay(isAutopay);
                    mobileUserProduct.setPProductCode(iptvProduct.getPProductCode());
                } else {
                    mobileUserProduct.setThirdCode(iptvProduct.getThirdCode());
                }
                Integer product_duration = Integer.valueOf(iptvProduct.getProductDuration());
                if (DateTimeUtil.DateTimeCompare(mobileUserProduct.getEtime(), date) <= 0) {
                    Date d_stime = new Date(date.getTime());
                    dd_stime = d_stime;
                    mobileUserProduct.setStime(date);
                    mobileUserProduct.setEtime(DateUtil.addDay(date, product_duration));
                    mobileUserProduct.setThirdEtime(getThirdEtime(mobileUserProduct, mobileUserProduct.getStime(), source));
                    if (BizConstant.MobileUserProduct.IsAutoPay_Yes.equals(isAutopay)) {
                        feetime = dateFormat.format(DateUtil.addMonth(date, product_duration / 31));
                    } else {
                        feetime = dateFormat.format(date);
                    }
                    mobileUserProduct.setFeemonth(feetime.substring(0, 8).replace("-", ""));
                } else {
                    Date d_stime = new Date(mobileUserProduct.getEtime().getTime());
                    dd_stime = d_stime;
                    mobileUserProduct.setEtime(DateUtil.addDay(mobileUserProduct.getEtime(), product_duration));
                    mobileUserProduct.setFeemonth(dateFormat1.format(DateUtil.addMonth(dateFormat1.parse(mobileUserProduct.getFeemonth()), product_duration / 31)));
                    mobileUserProduct.setThirdEtime(getThirdEtime(mobileUserProduct, mobileUserProduct.getThirdEtime(), source));
                }
                mobileUserProduct.setUpdateTime(date);
                // ??????????????????
                if (BizConstant.Code.Order.Cooperator_Code_Tencent.equals(cooperatorCode)) {
                    mobileUserProductService.saveOrUpdate((MobileUserProduct) mobileUserProduct);
                } else if (BizConstant.Code.Order.Cooperator_Code_Youku.equals(cooperatorCode)) {
                    mobileUserProductYoukuService.saveOrUpdate((MobileUserProductYouku) mobileUserProduct);
                } else if (BizConstant.Code.Order.Cooperator_Code_Aiqiyi.equals(cooperatorCode)) {
                    mobileUserProductAiqiyiService.saveOrUpdate((MobileUserProductAiqiyi) mobileUserProduct);
                }

                //?????????????????????
                MobileUserProductDet mobileUserProductDet = new MobileUserProductDet();
                mobileUserProductDet.setUserId(userid);
                mobileUserProductDet.setVuid(vuid);
                mobileUserProductDet.setExtTraceno(extTraceno);
                mobileUserProductDet.setOrderId(orderId);
                mobileUserProductDet.setProductCode(productCode);
                mobileUserProductDet.setProductType(iptvProduct.getProductType());
                mobileUserProductDet.setThirdCode(mobileUserProduct.getThirdCode());
                mobileUserProductDet.setpProductCode(mobileUserProduct.getPProductCode());
                mobileUserProductDet.setStime(dd_stime);
                mobileUserProductDet.setEtime(mobileUserProduct.getEtime());
                mobileUserProductDet.setFeemonth(mobileUserProduct.getFeemonth());
                mobileUserProductDet.setCreateTime(date);
                mobileUserProductDet.setSource(source);
                mobileUserProductDet.setUpdateTime(date);
                mobileUserProductDet.setIsAutopay(isAutopay);
                mobileUserProductDet.setTraceno(traceno);

                // ??????????????????
                mobileUserProductDetService.save(mobileUserProductDet);

            } catch (Exception e) {
                log.error("??????????????????????????????????????????" + JSONObject.toJSONString(mobileUserProduct) + e.getCause(), e);
            }

        }
        //?????????????????????????????????
//		Map query = new HashMap();
//		query.put("source", source);
//		MobileSourceComputeMode computeMode = (MobileSourceComputeMode)dbCenter.queryOne(MobileSourceComputeMode.class.getName(), query);
//		redisCache.putCacheWithExpireTime("MobileOrder_"+source+"_"+userid+"_"+product_code, "1", computeMode==null?1*24*60*60:computeMode.getWarnBuyDays()*24*60*60);
    }

    /**
     * ?????????????????????????????????????????????
     * @param mobileUserProduct
     * @param sTime
     * @param source
     * @return
     */
    protected Date getThirdEtime(MobileUserProductBase mobileUserProduct, Date sTime, String source) {
        //<--?????????????????????????????????
        MobileSourceComputeMode computeMode = mobileSourceComputeModeService.getBySource(source);
        //??????????????????????????????????????????
        if (computeMode != null && BizConstant.Code.Order.Compute_Mode_Days.equals(computeMode.getComputeMode())) {
            return DateUtil.addDay(sTime, computeMode.getDays());
        } else {
            return DateTimeUtil.accountDate(sTime, 1, Constant.TimeUnit.MONTH);
        }
        //-->
    }
}

