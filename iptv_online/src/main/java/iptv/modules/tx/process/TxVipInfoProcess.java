package iptv.modules.tx.process;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import iptv.common.BusinessException;
import iptv.modules.base.entity.db.IptvProduct;
import iptv.modules.base.process.VipInfoProcess;
import iptv.modules.base.service.impl.IptvProductServiceImpl;
import iptv.modules.base.factory.vipinfo.VipInfoProcessFactory;
import iptv.modules.tx.service.impl.MobileInterService;
import iptv.util.BizConstant;
import iptv.util.HttpUtils;
import iptv.util.SysConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wyq
 * @date
 */
@Component
public class TxVipInfoProcess extends VipInfoProcess implements InitializingBean {

    @Autowired
    private SysConfig sysConfig;

    @Autowired
    private IptvProductServiceImpl iptvProductService;

    @Autowired
    private MobileInterService mobileInterService;


    /**
     * 查询权益时间
     *
     * @param req 请求参数
     * @return 返回结果
     * @throws Exception
     */
    @Override
    public JSONObject queryTime(JSONObject req) throws Exception {

        JSONObject resp = new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");
        //获取产品包类型
        String product_type = req.getString("product_type");
        if (StringUtils.isBlank(product_type)) {
            //默认查询增值包时间
            product_type = "basic";
        }

        String vippkg = req.getString("vippkg");
        if (vippkg != null || StringUtils.isNotBlank(vippkg)) {
            //说明是重试逻辑，从数据库获取产品类型
            // 校验产品配置
            String product_code = vippkg;
            IptvProduct iptvProduct = iptvProductService.getIptvProduct(product_code, req.getString("source"),BizConstant.Code.Order.Cooperator_Code_Tencent);
            product_type = iptvProduct.getProductType();
        }

        //获取accesstoken
        String accesstoken = mobileInterService.getAccessToken();
        //组装参数请求腾讯
        Map<String, Object> tx_req = new HashMap<String, Object>();
        tx_req.put("version", "2.0");
        tx_req.put("format", "json");
//			tx_req.put("access_token", "XQUYsbrmH3V6xAcWtVZjJntYfo190srPJatMaMeN2plM66Qxazhfo8wATQc42A/SwSqV1/76MNg6w/eJe6Zh6tLz2Ys6M9EM");
        tx_req.put("access_token", accesstoken);
        tx_req.put("user_type", "0");
        tx_req.put("vuserid", req.getString("vuid"));
        tx_req.put("vendor_platform", sysConfig.getVENDOR_PLATFORM());

        String url = sysConfig.getTX_VIP_INFO_URL();
        String result;
        try {
            result = HttpUtils.doGet(url, tx_req);
        } catch (Exception e) {
            log.error("【TxVipInfoProcess】请求腾讯获取会员信息出错!" + e.getCause(), e);
            logger.error("【TxVipInfoProcess】请求腾讯获取会员信息出错!" + e.getCause(), e);
            throw new BusinessException("请求腾讯获取会员信息出错!");
        }

        if (StringUtils.isBlank(result)) {
            log.error("【TxVipInfoProcess】请求腾讯获取会员信息接口返回为空");
            logger.error("【TxVipInfoProcess】请求腾讯获取会员信息接口返回为空");
            throw new BusinessException("请求腾讯获取会员信息接口返回为空!");
        }
        JSONObject resultJson = JSONObject.parseObject(result);
        JSONObject resultcode = JSONObject.parseObject(resultJson.getString("result"));
        if (BizConstant.Code.Result_Code_Success_Num_0.equals(resultcode.getString("code"))) {
            JSONObject data = JSONObject.parseObject(resultJson.getString("data"));
            JSONArray vipInfos = data.getJSONArray("vipInfos");
            //默认获取增值包时间
            Integer vip_bid = 115;
            HashMap<String, String> Vip_Bid = getVipBid();
            if (Vip_Bid != null) {
                if (StringUtils.isNotBlank(Vip_Bid.get(product_type))) {
                    vip_bid = Integer.valueOf(Vip_Bid.get(product_type));
                } else if (StringUtils.isNotBlank(Vip_Bid.get("basic"))) {
                    vip_bid = Integer.valueOf(Vip_Bid.get("basic"));
                    log.error("【TxVipInfoProcess】查询vuid为:" + req.getString("vuid") + "的腾讯权益出错。产品类型为:" + req.getString("product_type") + "暂未配置对应vipBid值,查询默认basic权益");
                    logger.error("【TxVipInfoProcess】查询vuid为:" + req.getString("vuid") + "的腾讯权益出错。产品类型为:" + req.getString("product_type") + "暂未配置对应vipBid值,查询默认basic权益");
                } else {
                    log.error("【TxVipInfoProcess】查询vuid为:" + req.getString("vuid") + "的腾讯权益出错。产品类型为:" + req.getString("product_type") + "暂未配置对应vipBid值,查询默认115权益");
                    logger.error("【TxVipInfoProcess】查询vuid为:" + req.getString("vuid") + "的腾讯权益出错。产品类型为:" + req.getString("product_type") + "暂未配置对应vipBid值,查询默认115权益");
                }
            } else {
                log.error("【TxVipInfoProcess】查询vuid为:" + req.getString("vuid") + "的腾讯权益出错。暂未配置vipBid值,查询默认115权益");
                logger.error("【TxVipInfoProcess】查询vuid为:" + req.getString("vuid") + "的腾讯权益出错。暂未配置vipBid值,查询默认115权益");
            }
            if (null != vipInfos && vipInfos.size() > 0) {
                for (int i = 0; i < vipInfos.size(); i++) {
                    JSONObject job = vipInfos.getJSONObject(i);
                    //if(BizConstant.Tencent.VipInfo.vip_bid_cinema==job.getInteger("vip_bid")){
                    if (vip_bid.equals(job.getInteger("vip_bid"))) {
                        resp.put("is_vip", job.getInteger("vip"));
                        resp.put("start", job.getInteger("start"));
                        resp.put("end", job.getInteger("end"));
                        resp.put("vip_bid", job.getInteger("vip_bid"));
                        break;
                    }
                }
            } else {
                //0 非会员
                resp.put("is_vip", 0);
            }

        } else {
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求腾讯获取会员信息接口返回失败");
            return resp;
        }
        resp.put("vuid", req.getString("vuid"));
        return resp;
    }

    private HashMap<String, String> getVipBid() {
        HashMap<String, String> Vip_Bid = new HashMap<>();
        try {
            String vipBid = sysConfig.getVip_Bid();
            if (null == vipBid) {
                log.error("【TxVipInfoProcess】同步微信权益时vip_bid值选择暂未配置！！！");
                logger.error("【TxVipInfoProcess】同步微信权益时vip_bid值选择暂未配置！！！");
            } else {
                String[] split = vipBid.split("&&");
                for (String str : split) {
                    String[] split2 = str.split("#");
                    Vip_Bid.put(split2[0], split2[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("【TxVipInfoProcess】同步微信权益时vip_bid值选择获取失败！！！");
            logger.error("【TxVipInfoProcess】同步微信权益时vip_bid值选择获取失败！！！");
        }
        return Vip_Bid;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        VipInfoProcessFactory.create("tencent", this);
    }
}
