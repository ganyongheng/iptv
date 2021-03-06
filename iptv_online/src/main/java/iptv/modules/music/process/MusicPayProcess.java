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
        resp.put("msg","??????");
        try {
            CheckUtils.checkEmpty(req.getString("phone"), "?????????????????????????????????-???phone???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("source"), "?????????????????????????????????-???source???",
                    BizConstant.Code.Missing_Parameter);

        } catch (BusinessException e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }
        /**
         * ??????????????????????????????
         */
        String s = HttpUtils.doPost(getmsg_url, req);
        return JSONObject.toJSONString(resp);
    }
    public String checkMsg(JSONObject  req) {
        JSONObject resp = new JSONObject();
        resp.put("code",0);
        resp.put("msg","??????");
        try {
            CheckUtils.checkEmpty(req.getString("phone"), "?????????????????????????????????-???phone???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("source"), "?????????????????????????????????-???source???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("random_code"), "?????????????????????????????????-???random_code???",
                    BizConstant.Code.Missing_Parameter);
        } catch (BusinessException e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }

        /**
         * ??????????????????????????????
         */
        String s = HttpUtils.doPost(checkmsg_url, req);
        return JSONObject.toJSONString(resp);
    }

    @Transactional
    public String loginSuccess(JSONObject req) {
        JSONObject resp = new JSONObject();
        resp.put("code",0);
        resp.put("msg","??????");
        resp.put("phone", req.getString("phone"));
        try {
            CheckUtils.checkEmpty(req.getString("phone"), "?????????????????????????????????-???phone???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("source"), "?????????????????????????????????-???source???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("login_date"), "?????????????????????????????????-???login_date???",
                    BizConstant.Code.Missing_Parameter);
        } catch (BusinessException e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }
        String phone = req.getString("phone");
        String source = req.getString("source");
        String loginDate = req.getString("login_date");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //????????????
        boolean lock = false;
        try {
            try {
                //???????????????????????? ??????30???
                lock = redisCache.setnxWithExptime("LoginSuccessLock_" + phone, phone, 30);
            } catch (Exception e) {
                log.error("?????????????????????????????????????????????" + phone + "???" + e.getCause(), e);
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "???????????????????????????????????????");
                resp.put("phone", phone);
                return JSONObject.toJSONString(resp);
            }
            if (lock) {
                //???????????????????????????
                ZnyxLoginUser user = znyxLoginUserService.getUserByPhone(phone);
                if (user == null) {
                    //??????????????????
                    ZnyxLoginUser znyxLoginUser = new ZnyxLoginUser();
                    znyxLoginUser.setPhone(phone);
                    znyxLoginUser.setSource(source);
                    znyxLoginUser.setLoginDate(sdf.parse(loginDate));
                    znyxLoginUserService.save(znyxLoginUser);
                    loginSuccessLogger.info("?????????????????????" + znyxLoginUser);
                }
                //int i = 1/0;//????????????
                //??????????????????
                ZnyxUserLoginRecord znyxUserLoginRecord = new ZnyxUserLoginRecord();
                znyxUserLoginRecord.setPhone(phone);
                znyxUserLoginRecord.setSource(source);
                znyxUserLoginRecord.setLoginDate(sdf.parse(loginDate));
                znyxUserLoginRecordService.save(znyxUserLoginRecord);
                loginSuccessLogger.info("?????????????????????" + znyxUserLoginRecord);
                //Thread.sleep(1000*20);
            } else {
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "????????????????????????" + phone + "?????????????????????");
                resp.put("phone", phone);
                return JSONObject.toJSONString(resp);
            }
        } catch (Exception e) {
            log.error("???loginSuccess??????????????????????????????" + JSONObject.toJSONString(req) + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "???loginSuccess???????????????????????????:" + e.getMessage());
            resp.put("phone", phone);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return JSONObject.toJSONString(resp);
        } finally {
            if (lock) {
                try {
                    redisCache.del("LoginSuccessLock_" + phone);
                } catch (Exception e) {
                    log.error("?????????????????????????????????????????????" + phone + "???" + e.getCause(), e);
                }
            }
        }
        return JSONObject.toJSONString(resp);
    }

    public String getProduct(JSONObject req) {
        JSONObject resp = new JSONObject();
        JSONArray array = new JSONArray();
        resp.put("code", 0);
        resp.put("msg", "??????");
        resp.put("result", array);
        try {
            CheckUtils.checkEmpty(req.getString("source"), "?????????????????????????????????-???source???",
                    BizConstant.Code.Missing_Parameter);
        } catch (BusinessException e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
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
            //??????????????????
            List<ZnyxProduct> list = znyxProductService.getZnyxProduct(map);
            //list???json
            array = JSONArray.parseArray(JSONObject.toJSONString(list));
            resp.put("result", array);
        } catch (Exception e) {
            log.error("???????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "????????????????????????");
            return JSONObject.toJSONString(resp);
        }
        return JSONObject.toJSONString(resp);
    }

    /**
     * ????????????
     * @param req
     * @return
     */
    public String getOrder(JSONObject req){
        JSONObject resp = new JSONObject();
        resp.put("code", 0);
        resp.put("msg", "??????");
        try {
            CheckUtils.checkEmpty(req.getString("source"), "?????????????????????????????????-???source???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("seqno"), "?????????????????????????????????-???seqno???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("login_id"), "?????????????????????????????????-???login_id???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("login_type"), "?????????????????????????????????-???login_type???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("vippkg"), "?????????????????????????????????-???vippkg???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("vippkg_name"), "?????????????????????????????????-???vippkg_name???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("total"), "?????????????????????????????????-???total???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("is_auto"), "?????????????????????????????????-???is_auto???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("pay_type"), "?????????????????????????????????-???pay_type???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("is_auto"), "?????????????????????????????????-???is_auto???",
                    BizConstant.Code.Missing_Parameter);
            CheckUtils.checkEmpty(req.getString("content_code"), "?????????????????????????????????-???content_code???",
                    BizConstant.Code.Missing_Parameter);

        } catch (BusinessException e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("???getMsg??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        }
        String seqno = req.getString("seqno");
        //????????????????????????
        ZnyxOrderInfo zo = znyxOrderInfoService.getZnyxOrderInfoBySeqno(seqno);
        if(zo != null) {
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "???getOrder????????????????????????????????????????????????"+seqno+"???????????????");
            return resp.toJSONString();
        }
        // ????????????????????????
        ZnyxProduct product = znyxProductService.getZnyxProductByProductCode(req.getString("vippkg"));
        if(product == null) {
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "???getOrder???????????????????????????????????????"+req.getString("vippkg")+"???????????????????????????");
            return resp.toJSONString();
        }
        if("3".equals(req.getString("pay_type"))) {
            String traceno = nextSn("CA",seqno);
            req.put("traceno",traceno);
        }
        //????????????
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
     * ?????????????????????
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
     * ??????????????????
     * @param req
     * @return
     */
    public String syncAccount(JSONObject req){
        JSONObject resp = new JSONObject();
        resp.put("code",0);
        resp.put("msg","??????");
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
                resp.put("msg", "??????????????????????????????????????????ext_traceno:"+ext_traceno+"?????????????????????");
                log.error("???syncAccount???ext_traceno:"+ext_traceno+"????????????");
                return JSONObject.toJSONString(resp);
            }
        }catch (BusinessException e) {
            log.error("???syncAccount??????????????????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num);
            resp.put("msg", e.getMessage());
            return JSONObject.toJSONString(resp);
        } catch (Exception e) {
            log.error("???syncAccount??????????????????" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "????????????");
            return JSONObject.toJSONString(resp);
        }finally {
            if (lock) {
                try {
                    redisCache.del("SyncAccountLock_" + ext_traceno);
                } catch (Exception e) {
                    log.error("?????????????????????????????????????????????" + "SyncAccountLock_"+ext_traceno + "???" + e.getCause(), e);
                }
            }
        }
        return JSONObject.toJSONString(resp);
    }

}
