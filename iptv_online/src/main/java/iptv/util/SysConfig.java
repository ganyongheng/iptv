package iptv.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Author wyy
 * Date 2022/3/17 11:06
 **/

@Data
@Component
@ConfigurationProperties(prefix = "sys-config")
public class SysConfig {
    private String TX_ACCESS_TOKEN_URL;
    private String TX_ACCESS_TOKEN_EXPIRETIME;

    private String TX_APPID;
    private String TX_APPKEY;
    private String TX_CREATE_ORDER_URL;
    private String TX_CONFIRM_ORDER_URL;
    private String TX_VIP_INFO_URL;
    private String TX_VUID_URL;
    private String TX_Q_UA;
    private String VENDOR_PLATFORM;

    private String AIQIYI_VIP_INFO_URL;
    private String AIQIYI_CONFIRM_ORDER_URL;

    private String SYNC_TOKEN_TO_MOBILE_URL;

    private String MobileOrderResend_Interval;
    private String MobileOrderResendDequeueTask_DequeueNums;
    private String MobileOrderResend_Max_Nums;

    private String MobileDoAccountSwitch;

    //告警配置
    private String IncidentPush_Url;
    private String IncidentPush_Appid;
    private String IncidentPush_Appkey;
    private String IncidentPush_Incidentcode;

    //http请求异常达到发送告警的最大次数
    private String IncidentPush_HttpErrorMaxCount;
    //IncidentPush_HttpErrorMaxCount的redis-key最大有效时长 单位:秒
    private String IncidentPush_HttpErrorPerTime;

    //本地ip
    private String Local_Ip;

    //优酷查询权益的Channel_Ids
    private String Channel_Ids;

    //同步微信权益时vip_bid值选择
    private String Vip_Bid;
    //爱奇艺合作方编号
    private String AIQIYI_PARTNER_NO;

    //爱奇艺发货接口版本
    private String AIQIYI_SYNCORDER_VERSION;

    //爱奇艺获取会员信息接口版本
    private String AIQIYI_GET_VIP_INFO_VERSION;

    //每次从队列取的个数
    private  String syncRightsTimer_DequeueNums;
    //延迟取数据的时间
    private  String syncRightsTimer_DelayTime;
    //订单状态回调通知最大次数
    private  String syncRightsTime_Max_Nums;

    //爱奇艺鉴权密钥
    private String pubkey;

    //优酷请求发货url
    private String YOUKU_CONFIRM_ORDER_URL;

    private String YOUKU_APPKEY;

    private String YOUKU_APPSECRET;

    private String YOUKU_SIGNTYPE;

    //;优酷发货接口地址
    private String YOUKU_CONFIRM_ORDER_INTERFACE;
}
