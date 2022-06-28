package iptv.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import iptv.modules.music.entity.db.ZnyxProductConfig;
import iptv.modules.music.entity.db.ZnyxSyncAccount;
import iptv.modules.music.entity.vo.NotifyOrderVo;
import iptv.modules.music.service.impl.ZnyxProductConfigServiceImpl;
import iptv.modules.music.service.impl.ZnyxSyncAccountServiceImpl;
import iptv.util.BizConstant;
import iptv.util.HttpIncident;
import iptv.util.HttpUtils;
import iptv.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能音箱订购关系同步定时任务
 */
@Component
public class ZnyxSyncAccountTask extends QuartzJobBean {

    private static Logger log = LoggerFactory.getLogger(ZnyxSyncAccountTask.class);


    @Autowired
    private ZnyxSyncAccountServiceImpl syncAccountService;

    @Autowired
    private ZnyxProductConfigServiceImpl znyxProductConfigService;

    @Value(value = "${ZNYX.SYNC_ACCOUNT_NUMS}")
    private Integer SYNC_ACCOUNT_NUMS;

    @Value(value = "${ZNYX.SYNC_ACCOUNT_SELECTNUMS}")
    private Integer SYNC_ACCOUNT_SELECTNUMS;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("【ZnyxSyncAccountTask】订购关系同步定时任务开始执行");
        Long start = System.currentTimeMillis();
        Integer failNums = SYNC_ACCOUNT_NUMS;
        if (null == failNums || 0 == failNums) {
            failNums = 3;
            log.warn("【ZnyxSyncAccountTask】订购关系同步通知第三方最大次数暂未配置,设置默认值3");
        }
        Integer limit = SYNC_ACCOUNT_SELECTNUMS;
        if (null == limit || 0 == limit) {
            limit = 300;
            log.warn("【ZnyxSyncAccountTask】一次查询订购关系同步通知失败实体个数暂未配置,设置默认值300");
        }

        try {
            List<ZnyxSyncAccount> accountListst = syncAccountService.getSyncAccountByNotifyStatus(limit, BizConstant.ZNYX.initStatus,failNums);
            List<ZnyxProductConfig> configList = znyxProductConfigService.list();
            if (null != accountListst && accountListst.size() > 0 && null != configList && configList.size() > 0) {
                Integer toSuccess = 0;
                Integer toFail = 0;
                Map<String, String> mapUrl = new HashMap<>();
                for (ZnyxSyncAccount account : accountListst) {
                    NotifyOrderVo orderVo = this.getNotifyOrderVo(account, configList, mapUrl);
                    if (null != orderVo) {
                        String url = mapUrl.get(account.getSource());
                        //开始通知
                        String code = this.syncAccountHttp(orderVo, url);
                        if (null != code && "0".equals(code)) {
                            //通知成功修改状态值
                            account.setNotifyStatus(BizConstant.ZNYX.Notify_Success_Status);
                            account.setUpdateTime(new Date());
                            toSuccess++;
                        } else {
                            //通知失败
                            this.notifyFail(account, failNums, url, orderVo);
                            account.setUpdateTime(new Date());
                            toFail++;
                        }
                        syncAccountService.updateById(account);
                    } else {
                        //source没有配置对应url和签名key
                        toFail++;
                    }
                }
                log.info("【ZnyxSyncAccountTask】订购关系同步通知第三方成功数：{}", toSuccess);
                log.info("【ZnyxSyncAccountTask】订购关系同步通知第三方失败数：{}", toFail);
            }
        } catch (Exception e) {
            log.error("订购关系同步通知第三方定时任务失败", e);
            e.printStackTrace();
        } finally {
            Long end = System.currentTimeMillis();
            log.info("【ZnyxSyncAccountTask】订购关系同步通知第三方定时任务执行耗时: {} ms", (end - start));
        }

    }

    /**
     * 获取NotifyOrderVo
     *
     * @param account
     * @param configList 配置信息
     * @param mapUrl     通知urlMap
     * @return
     * @throws Exception
     */
    public NotifyOrderVo getNotifyOrderVo(ZnyxSyncAccount account, List<ZnyxProductConfig> configList, Map<String, String> mapUrl) throws Exception {
        for (ZnyxProductConfig config : configList) {
            if (StringUtils.isNotBlank(config.getSource()) && StringUtils.isNotBlank(config.getSyncAccountUrl()) &&
                    StringUtils.isNotBlank(config.getSignKey())) {
                if (account.getSource().equals(config.getSource())) {
                    NotifyOrderVo orderVo = new NotifyOrderVo(account);
                    orderVo.setSign(MD5Util.md5Encode(JSON.toJSONString(orderVo), config.getSignKey()));
                    if (!mapUrl.containsKey(account.getSource())) {
                        //添加通知url
                        mapUrl.put(account.getSource(), config.getSyncAccountUrl());
                    }
                    return orderVo;
                }
            }
        }
        return null;
    }

    /**
     * 订购关系同步通知HTTP
     *
     * @param orderVo 通知实体
     * @param url     通知url
     * @return    0通知成功  -1通知失败
     */
    public String syncAccountHttp(NotifyOrderVo orderVo, String url) {
        String result = null;
        try {
            JSONObject jsonObject = (JSONObject) JSONObject.toJSON(orderVo);
            result = HttpUtils.doPost(url, jsonObject);
        } catch (Exception e) {
            log.error("【ZnyxSyncAccountTask】订购关系同步第三方出错：" + e.getCause(), e);
            return "-1";
        }
        if (StringUtils.isNotBlank(result)) {
            JSONObject resp_json = JSON.parseObject(result);
            String code = resp_json.getString("result");
            if (StringUtils.isNotBlank(code) && BizConstant.ZNYX.Result_Code_Success_Num_0.equals(code)) {
                return "0";
            } else {
                return "-1";
            }
        }
        return "-1";
    }

    /**
     * 通知失败校验是否需要告警
     *
     * @param account
     * @param failNums
     */
    public void notifyFail(ZnyxSyncAccount account, Integer failNums, String url, NotifyOrderVo orderVo) {
        //判断是否达到最大通知次数
        Integer nums = account.getNums();
        if (failNums < nums + 1) {
            log.error("【ZnyxSyncAccountTask】订购关系同步第三方失败次数达到最大值:{}URL:{} 请求参数：{}", failNums, url, JSONObject.toJSONString(orderVo));
            //告警
            HttpIncident.incidentPush(String.format("【ZnyxSyncAccountTask】订购关系同步第三方失败次数达到最大值 [%s]次 URL:[%s] 参数:[%s]", failNums,
                    url, JSONObject.toJSONString(orderVo)),
                    BizConstant.IncidentPush.Lvlcode_Warm, BizConstant.IncidentPush.Incidentcategory_Alarm);
        }
        account.setNums(nums + 1);
    }
}
