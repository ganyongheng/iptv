package iptv.modules.music.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.common.BusinessException;
import iptv.common.CheckUtils;
import iptv.modules.base.service.impl.IncidentPushServcie;
import iptv.modules.music.entity.db.ZnyxProductConfig;
import iptv.modules.music.entity.db.ZnyxSyncAccount;
import iptv.modules.music.entity.vo.NotifyOrderVo;
import iptv.modules.music.mapper.ZnyxSyncAccountMapper;
import iptv.util.BizConstant;
import iptv.util.DateUtil;
import iptv.util.HttpUtils;
import iptv.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ZnyxSyncAccountServiceImpl extends ServiceImpl<ZnyxSyncAccountMapper, ZnyxSyncAccount> {

    private Logger log = LoggerFactory.getLogger(ZnyxSyncAccountServiceImpl.class);

    @Autowired
    private IncidentPushServcie incidentPushServcie;

    @Autowired
    private ZnyxSyncAccountMapper accountMapper;

    @Autowired
    private ZnyxProductConfigServiceImpl znyxProductConfigService;


    /**
     * 订购关系同步线程池(用于控制并发)
     */
    public static final ExecutorService pool = Executors.newFixedThreadPool(3);

    /**
     * 保存并通知3A
     *
     * @param req
     * @param config source对应渠道信息
     * @throws Exception
     */
    @Transactional
    public void saveAnNoticeSyncAccount(JSONObject req, ZnyxProductConfig config) throws Exception {
        ZnyxSyncAccount account = existence(req);
        if (null != account) {
            try {
                //判断id是否为空
                if (null == account.getId()) {
                    //新增
                    this.baseMapper.insert(account);
                } else {
                    //更新
                    this.baseMapper.updateById(account);
                }
            } catch (Exception e) {
                log.error("订购关系同步数据入库失败:" + JSONObject.toJSONString(account), e);
                alarm("保存订购关系同步实体失败ext_traceno:" + account.getExtTraceno() + ",phone:" + account.getPhone());
                //TODO 确定是否尝试通知一次
                //尝试去通知一次3A
                NotifyOrderVo orderVo = new NotifyOrderVo(account);
                orderVo.setSign(MD5Util.md5Encode(JSON.toJSONString(orderVo), config.getSignKey()));
                JSONObject jsonObject = (JSONObject) JSONObject.toJSON(orderVo);
                log.info("sync_account保存订购关系同步数据入库失败,重试通知一次AAA的ext_traceno:" + account.getExtTraceno());
                String result = HttpUtils.doPost(config.getSyncAccountUrl(), jsonObject);
                log.info("sync_account保存订购关系同步数据入库失败ext_traceno:" + account.getExtTraceno() + ".重试通知一次AAA返回结果:" + result);
                throw e;
            }

            try {
                pool.execute(new SendZnyxSyncAccountHttp(account, config));
            } catch (Exception e) {
                log.error("【syncAccount】订购关系同步通知第三方Http请求加入线程队列出错：" + JSONObject.toJSONString(account), e);
                e.printStackTrace();
                //异常不需要抛出，数据已保存，定时任务扫描重试通知即可
                return;
            }
        } else {
            log.error("订购关系同步数据入库失败:" + req.toJSONString());
            throw new BusinessException("系统错误");
        }


    }

    /**
     * 赋值
     *
     * @param req
     * @param account
     */
    public ZnyxSyncAccount assignment(JSONObject req, ZnyxSyncAccount account) {
        if (null == account) {
            account = new ZnyxSyncAccount();
            account.setCreateTime(new Date());
        }
        account.setSource(req.getString("source"));
        account.setVippkg(req.getString("vippkg"));
        account.setPhone(req.getString("phone"));
        account.setExtTraceno(req.getString("ext_traceno"));
        account.setExtTracenoCvod(req.getString("ext_traceno_cvod") != null ? req.getString("ext_traceno_cvod") : "");
        account.setAuto(req.getIntValue("auto"));
        account.setPrice(req.getIntValue("price"));
        account.setTotal(req.getIntValue("total"));
        account.setRefundTotal(req.getIntValue("refund_total") != 0 ? req.getIntValue("refund_total") : 0);
        account.setSyncType(req.getIntValue("sync_type"));
        account.setPayType(req.getIntValue("pay_type"));
        account.setVipMsg(req.getString("vip_msg"));
        account.setVipId(req.getIntValue("vip_id") != 0 ? req.getIntValue("vip_id") : 0);
        String begin_time = req.getString("begin_time");
        String end_time = req.getString("end_time");
        String cancel_time = req.getString("cancel_time") != null ? req.getString("cancel_time") : "";
        account.setBeginTime(DateUtil.StringToDate(begin_time, DateUtil.YYYY_MM_DD_HH_MM_SS));
        account.setEndTime(DateUtil.StringToDate(end_time, DateUtil.YYYY_MM_DD_HH_MM_SS));
        if (StringUtils.isNotBlank(cancel_time)) {
            account.setCancelTime(DateUtil.StringToDate(cancel_time, DateUtil.YYYY_MM_DD_HH_MM_SS));
        }
        //重试次数初始化0
        account.setNums(0);
        account.setNotifyStatus(BizConstant.ZNYX.initStatus);
        account.setUpdateTime(new Date());
        return account;
    }

    /**
     * 查询是否保存过,没保存赋值
     *
     * @param req
     * @return
     */
    public ZnyxSyncAccount existence(JSONObject req) {
        Map map = new HashMap();
        //流水号+手机+来源确定唯一
        map.put("ext_traceno", req.getString("ext_traceno"));
        map.put("phone", req.getString("phone"));
        map.put("source", req.getString("source"));
        List<ZnyxSyncAccount> list = this.baseMapper.selectByMap(map);
        if (list.size() > 0) {
            return this.assignment(req, list.get(0));
        } else {
            ZnyxSyncAccount account = null;
            return this.assignment(req, account);
        }
    }

    /**
     * 移动业管平台订购关系同步参数校验
     *
     * @param req
     * @throws Exception
     */
    public ZnyxProductConfig checkSyncAccount(JSONObject req) throws Exception {
        CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-【source】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("vippkg"), "请求失败：缺少请求参数-【vippkg】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("phone"), "请求失败：缺少请求参数-【phone】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("ext_traceno"), "请求失败：缺少请求参数-【ext_traceno】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("auto"), "请求失败：缺少请求参数-【auto】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("price"), "请求失败：缺少请求参数-【price】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("total"), "请求失败：缺少请求参数-【total】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("begin_time"), "请求失败：缺少请求参数-【begin_time】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("end_time"), "请求失败：缺少请求参数-【end_time】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("sync_type"), "请求失败：缺少请求参数-【sync_type】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("pay_type"), "请求失败：缺少请求参数-【pay_type】",
                BizConstant.Code.Missing_Parameter);
        String source = req.getString("source");
        ZnyxProductConfig config = znyxProductConfigService.getOneBySource(source);
        if (null == config) {
            log.error("source暂未在znyx_product_config表里配置产品信息,流水号ext_traceno:" + req.getString("ext_traceno"));
            throw new BusinessException("系统错误");
        }
        if (StringUtils.isBlank(config.getSignKey())) {
            log.error("source在znyx_product_config表暂未配置签名key,流水号ext_traceno:" + req.getString("ext_traceno"));
            throw new BusinessException("系统错误");
        }
        if (StringUtils.isBlank(config.getSyncAccountUrl())) {
            log.error("source在znyx_product_config表暂未配置订购关系同步通知第三方url,流水号ext_traceno:" + req.getString("ext_traceno"));
            throw new BusinessException("系统错误");
        }
        return config;
    }

    /**
     * 告警
     *
     * @param msg
     */
    private void alarm(String msg) {
        incidentPushServcie.incidentPush(msg, BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
    }

    /**
     * 根据Ext与phone与source更新nums与status
     *
     * @param account
     */
    @Transactional
    public void updatePart(ZnyxSyncAccount account) {
        accountMapper.updateByExtAndPhoneAndSource(account);
    }

    /**
     * 根据notify_status查询指定数量
     *
     * @param limit         查询数量
     * @param notify_status -1为通知失败，9为通知成功
     * @param failNums      次数
     * @return
     */
    public List<ZnyxSyncAccount> getSyncAccountByNotifyStatus(Integer limit, String notify_status,Integer failNums) {
        return accountMapper.getSyncAccountByNotifyStatus(limit, notify_status,failNums);
    }


    /**
     * 工作线程类通知第三方，订购关系同步通知
     */
    private class SendZnyxSyncAccountHttp implements Runnable {

        private ZnyxSyncAccount account;

        private ZnyxProductConfig config;

        public SendZnyxSyncAccountHttp(ZnyxSyncAccount account, ZnyxProductConfig config) {
            this.account = account;
            this.config = config;
        }

        @Override
        public void run() {
            String result = null;
            JSONObject jsonObject = null;
            try {
                NotifyOrderVo orderVo = new NotifyOrderVo(account);
                //签名
                orderVo.setSign(MD5Util.md5Encode(JSON.toJSONString(orderVo), config.getSignKey()));
                jsonObject = (JSONObject) JSONObject.toJSON(orderVo);
                result = HttpUtils.doPost(config.getSyncAccountUrl(), jsonObject);
            } catch (Exception e) {
                log.error(String.format("【sync_account】订购关系同步通知AAAHTTP出错 [%s] " + e.getCause(), jsonObject.toJSONString()), e);
            }

            if (StringUtils.isNotBlank(result)) {
                JSONObject resp_json = JSON.parseObject(result);
                String code = resp_json.getString("result");
                if (StringUtils.isNotBlank(code) && BizConstant.ZNYX.Result_Code_Success_Num_0.equals(code)) {
                    //通知成功
                    account.setNotifyStatus(BizConstant.ZNYX.Notify_Success_Status);
                } else {
                    //通知失败重试次数+1
                    account.setNums(account.getNums() + 1);
                }
                //通知结果修改数据
                updatePart(account);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        NotifyOrderVo orderVo = new NotifyOrderVo();
        orderVo.setActionType("1");
        orderVo.setContentName("12553");
        orderVo.setSign(MD5Util.md5Encode(JSON.toJSONString(orderVo), "1"));
        System.out.println(orderVo);

    }
}
