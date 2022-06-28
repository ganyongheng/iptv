package iptv.modules.music.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import iptv.common.BusinessException;
import iptv.common.CheckUtils;
import iptv.config.redis.RedisCache;
import iptv.modules.music.entity.db.*;
import iptv.modules.music.service.impl.ZnyxLoginUserServiceImpl;
import iptv.modules.music.service.impl.ZnyxOrderInfoServiceImpl;
import iptv.modules.music.service.impl.ZnyxProductServiceImpl;
import iptv.modules.music.service.impl.ZnyxSyncAccountServiceImpl;
import iptv.modules.music.service.impl.ZnyxUserLoginRecordServiceImpl;
import iptv.modules.music.strategy.pay.PayStrategy;
import iptv.modules.music.strategy.pay.PayStrategyService;
import iptv.util.BizConstant;
import iptv.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.*;

@Component
public class MusicPayProcess {

    @Autowired
    private ZnyxProductServiceImpl znyxProductService;

    @Autowired
    private ZnyxLoginUserServiceImpl znyxLoginUserService;

    @Autowired
    private ZnyxUserLoginRecordServiceImpl znyxUserLoginRecordService;

    @Autowired
    private ZnyxSyncAccountServiceImpl znyxSyncAccountService;

    @Autowired
    private ZnyxOrderInfoServiceImpl znyxOrderInfoService;

    @Autowired
    private PayStrategy payStrategy;

    @Autowired
    private RedisCache redisCache;

    private Logger log = LoggerFactory.getLogger(MusicPayProcess.class);
    private static Logger loginSuccessLogger = LoggerFactory.getLogger("loginSuccessLogger");
    //private static Logger getProductLogger = LoggerFactory.getLogger("getProductLogger");

    @Value("${ZNYX.GET_MSG_URL}")
    private String  getmsg_url;
    @Value("${ZNYX.CHECK_MSG_URL}")
    private String  checkmsg_url;

    public  String getMsg(JSONObject req){
        JSONObject resp = new JSONObject();
        resp.put("code",0);
        resp.put("msg","成功");
        try {
            CheckUtils.checkEmpty(req.getString("phone"), "请求失败：缺少请求参数-【phone】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-【source】",
                    BizConstant.Code.Missing_Parameter);

        } catch (BusinessException e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }
        /**
         * 发送请求，获取验证码
         */
        String s = HttpUtils.doPost(getmsg_url, req);
        return JSONObject.toJSONString(resp);
    }
    public String checkMsg(JSONObject  req) {
        JSONObject resp = new JSONObject();
        resp.put("code",0);
        resp.put("msg","成功");
        try {
            CheckUtils.checkEmpty(req.getString("phone"), "请求失败：缺少请求参数-【phone】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-【source】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("random_code"), "请求失败：缺少请求参数-【random_code】",
                    BizConstant.Code.Missing_Parameter);
        } catch (BusinessException e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }

        /**
         * 发送请求，获取验证码
         */
        String s = HttpUtils.doPost(checkmsg_url, req);
        return JSONObject.toJSONString(resp);
    }

    @Transactional
    public String loginSuccess(JSONObject req) {
        JSONObject resp = new JSONObject();
        resp.put("code",0);
        resp.put("msg","成功");
        resp.put("phone", req.getString("phone"));
        try {
            CheckUtils.checkEmpty(req.getString("phone"), "请求失败：缺少请求参数-【phone】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-【source】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("login_date"), "请求失败：缺少请求参数-【login_date】",
                    BizConstant.Code.Missing_Parameter);
        } catch (BusinessException e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }
        String phone = req.getString("phone");
        String source = req.getString("source");
        String loginDate = req.getString("login_date");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //分布式锁
        boolean lock = false;
        try {
            try {
                //给锁设置超时时间 默认30秒
                lock = redisCache.setnxWithExptime("LoginSuccessLock_" + phone, phone, 30);
            } catch (Exception e) {
                log.error("同步登录用户获取临时锁失败：【" + phone + "】" + e.getCause(), e);
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "同步登录用户获取临时锁失败");
                resp.put("phone", phone);
                return JSONObject.toJSONString(resp);
            }
            if (lock) {
                //用户是否已经登录过
                ZnyxLoginUser user = znyxLoginUserService.getUserByPhone(phone);
                if (user == null) {
                    //同步用户信息
                    ZnyxLoginUser znyxLoginUser = new ZnyxLoginUser();
                    znyxLoginUser.setPhone(phone);
                    znyxLoginUser.setSource(source);
                    znyxLoginUser.setLoginDate(sdf.parse(loginDate));
                    znyxLoginUserService.save(znyxLoginUser);
                    loginSuccessLogger.info("登录用户信息：" + znyxLoginUser);
                }
                //int i = 1/0;//考虑事务
                //用户登录记录
                ZnyxUserLoginRecord znyxUserLoginRecord = new ZnyxUserLoginRecord();
                znyxUserLoginRecord.setPhone(phone);
                znyxUserLoginRecord.setSource(source);
                znyxUserLoginRecord.setLoginDate(sdf.parse(loginDate));
                znyxUserLoginRecordService.save(znyxUserLoginRecord);
                loginSuccessLogger.info("用户登录记录：" + znyxUserLoginRecord);
                //Thread.sleep(1000*20);
            } else {
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "存在用户手机号为" + phone + "的请求正在处理");
                resp.put("phone", phone);
                return JSONObject.toJSONString(resp);
            }
        } catch (Exception e) {
            log.error("【loginSuccess】同步用户登录出错：" + JSONObject.toJSONString(req) + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "【loginSuccess】同步用户登录出错:" + e.getMessage());
            resp.put("phone", phone);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return JSONObject.toJSONString(resp);
        } finally {
            if (lock) {
                try {
                    redisCache.del("LoginSuccessLock_" + phone);
                } catch (Exception e) {
                    log.error("同步用户登录删除临时锁失败：【" + phone + "】" + e.getCause(), e);
                }
            }
        }
        return JSONObject.toJSONString(resp);
    }

    public String getProduct(JSONObject req) {
        JSONObject resp = new JSONObject();
        JSONArray array = new JSONArray();
        resp.put("code", 0);
        resp.put("msg", "成功");
        resp.put("result", array);
        try {
            CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-【source】",
                    BizConstant.Code.Missing_Parameter);
        } catch (BusinessException e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }
        try {
            //TODO
            req.remove("sign");
            req.remove("transactionID");
            Map map = JSON.parseObject(JSON.toJSONString(req));
            //int i = 1/0;
            //获取产品列表
            List<ZnyxProduct> list = znyxProductService.getZnyxProduct(map);
            //list转json
            array = JSONArray.parseArray(JSONObject.toJSONString(list));
            resp.put("result", array);
        } catch (Exception e) {
            log.error("获取产品列表出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "获取产品列表出错");
            return JSONObject.toJSONString(resp);
        }
        return JSONObject.toJSONString(resp);
    }

    /**
     * 下单支付
     * @param req
     * @return
     */
    public String getOrder(JSONObject req){
        JSONObject resp = new JSONObject();
        resp.put("code", 0);
        resp.put("msg", "成功");
        try {
            CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-【source】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("seqno"), "请求失败：缺少请求参数-【seqno】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("login_id"), "请求失败：缺少请求参数-【login_id】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("login_type"), "请求失败：缺少请求参数-【login_type】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("vippkg"), "请求失败：缺少请求参数-【vippkg】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("vippkg_name"), "请求失败：缺少请求参数-【vippkg_name】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("total"), "请求失败：缺少请求参数-【total】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("is_auto"), "请求失败：缺少请求参数-【is_auto】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("pay_type"), "请求失败：缺少请求参数-【pay_type】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("is_auto"), "请求失败：缺少请求参数-【is_auto】",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("content_code"), "请求失败：缺少请求参数-【content_code】",
                    BizConstant.Code.Missing_Parameter);

        } catch (BusinessException e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【getMsg】校验请求参数出错：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }
        String seqno = req.getString("seqno");
        //判断订单是否重复
        ZnyxOrderInfo zo = znyxOrderInfoService.getZnyxOrderInfoBySeqno(seqno);
        if(zo != null) {
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "【getOrder】下单支付失败：已经存在流水号【"+seqno+"】的订单！");
            return resp.toJSONString();
        }
        // 根据编码查询产品
        ZnyxProduct product = znyxProductService.getZnyxProductByProductCode(req.getString("vippkg"));
        if(product == null) {
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "【getOrder】下单支付失败：产品编码为"+req.getString("vippkg")+"的产品配置找不到！");
            return resp.toJSONString();
        }
        if("3".equals(req.getString("pay_type"))) {
            String traceno = nextSn("CA",seqno);
            req.put("traceno",traceno);
        }
        //创建订单
        ZnyxOrderInfo znyxOrderInfo = new ZnyxOrderInfo();
        znyxOrderInfo = JSONObject.toJavaObject(req,ZnyxOrderInfo.class);
        znyxOrderInfo.setStatus(BizConstant.Code.ZnyxOrder.Order_Pay_Status_Notpay);
        znyxOrderInfoService.save(znyxOrderInfo);
        /*znyxOrderInfo.setSource(req.getString("source"));
        znyxOrderInfo.setSeqno(req.getString("seqno"));
        znyxOrderInfo.setLoginId(req.getString("login_id"));
        znyxOrderInfo.setLoginType(req.getString("login_type"));
        znyxOrderInfo.setVippkg(req.getString("vippkg"));
        znyxOrderInfo.setVippkgName(req.getString("vippkg_name"));
        znyxOrderInfo.setTotal(req.getInteger("total"));
        znyxOrderInfo.setIsAuto(req.getInteger("is_auto"));
        znyxOrderInfo.setBuyNum(req.getInteger("buy_num"));*/
        PayStrategyService payStrategyService = payStrategy.getSource(req.getString("pay_type"));
        resp = payStrategyService.pay(req,znyxOrderInfo);
        return resp.toJSONString();
    }

    /**
     * 生产订单号方法
     * @param prefix
     * @return
     */
    private String nextSn(String prefix,String seqno){

        String hashCodeStr=seqno.hashCode()+"";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String dateStr = sdf.format(new Date());

        Random r=new Random();
        int rand=r.nextInt(90) +10;

        String noStr=prefix+dateStr+rand;

        hashCodeStr=hashCodeStr.replace("-", "");
        int dif=28-noStr.length();
        int hashCodeLen=hashCodeStr.length();
        if(dif<hashCodeLen){

            hashCodeStr=hashCodeStr.substring(0, dif);

        }else if(dif>=hashCodeLen){

            int size=dif-hashCodeLen;
            StringBuilder sb=new StringBuilder();
            for(int i=0;i<size;i++){
                sb.append("0");
            }
            hashCodeStr=sb.toString()+hashCodeStr;

        }
        noStr=noStr+hashCodeStr;

        return noStr;
    }

    /**
     * 订购关系同步
     * @param req
     * @return
     */
    public String syncAccount(JSONObject req){
        JSONObject resp = new JSONObject();
        resp.put("code",0);
        resp.put("msg","成功");
        resp.put("ext_traceno", req.getString("ext_traceno") != null ? req.getString("ext_traceno"):"");
        boolean lock = false;
        String ext_traceno = null;
        try{
            ZnyxProductConfig config = znyxSyncAccountService.checkSyncAccount(req);
            ext_traceno = req.get("ext_traceno").toString();
            lock = redisCache.setnxWithExptime("SyncAccountLock_" + ext_traceno, ext_traceno, 30);
            if(lock){
                znyxSyncAccountService.saveAnNoticeSyncAccount(req,config);
            }else{
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "订购关系同步运营商平台订单号ext_traceno:"+ext_traceno+"正在处理请稍后");
                log.error("【syncAccount】ext_traceno:"+ext_traceno+"重复调用");
                return JSONObject.toJSONString(resp);
            }
        }catch (BusinessException e) {
            log.error("【syncAccount】捕获到自定义错误：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("【syncAccount】系统错误：" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "系统错误");
            return JSONObject.toJSONString(resp);
        }finally {
            if (lock) {
                try {
                    redisCache.del("SyncAccountLock_" + ext_traceno);
                } catch (Exception e) {
                    log.error("订购关系同步临时锁释放失败：【" + "SyncAccountLock_"+ext_traceno + "】" + e.getCause(), e);
                }
            }
        }
        return JSONObject.toJSONString(resp);
    }

}
