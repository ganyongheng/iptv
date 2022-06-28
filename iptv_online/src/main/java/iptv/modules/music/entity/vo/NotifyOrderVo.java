package iptv.modules.music.entity.vo;

import iptv.modules.music.entity.db.ZnyxSyncAccount;
import iptv.util.DateUtil;
import lombok.Data;

import java.io.Serializable;

/**
 * 同步AAA订购关系VO
 */
@Data
public class NotifyOrderVo implements Serializable {
    private static final long serialVersionUID = 132762221254802506L;
    public NotifyOrderVo(){

    }

    public NotifyOrderVo(ZnyxSyncAccount account){
        this.cpTransID = account.getExtTraceno();
        this.loginAccount = account.getPhone();
        this.productID = account.getVippkg();
        this.effectiveTime = DateUtil.DateToString(account.getBeginTime(),DateUtil.YYYY_MM_DD_HH_MM_SS);
        this.expireTime = DateUtil.DateToString(account.getEndTime(),DateUtil.YYYY_MM_DD_HH_MM_SS);
        this.continueFlag = null;  //待确定
        this.actionType = String.valueOf(account.getSyncType());
        this.orderSource = null; //待确定account.getSource();
        if(account.getVipId() != null && account.getVipId() != 0){
            this.planId = String.valueOf(account.getVipId());
        }
        if(account.getPayType() != null && account.getPayType() != 0){
            this.payWay = String.valueOf(account.getPayType());
        }

    }

    /**
     * 请求流水号
     * 时间戳(yyyyMMddHHmmss 14位)+序号（18位自增）
     * 必传
     */
    private String transactionID;

    /**
     * 线上订购时的CP方订单流水号,线下订购为空
     */
    private String cpTransID;

    /**
     * 用户账号
     * 必传
     */
    private String loginAccount;

    /**
     * 产品ID
     * 必传
     */
    private String productID;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 订购时的内容名称
     */
    private String contentName;

    /**
     * 订购时填内容ID
     */
    private String contentID;

    /**
     * 订购关系生效时间，格式：yyyy-MM-dd HH:mm:ss
     * 必传
     */
    private String effectiveTime;

    /**
     * 订购关系失效时间，格式：yyyy-MM-dd HH:mm:ss
     * 必传
     */
    private String expireTime;

    /**
     * 续订标识：0-否；1-是
     * 必传
     */
    private String continueFlag;


    /**
     * 操作类型：1-订购；2-取消续订；3-退订
     * 必传
     */
    private String actionType;

    /**
     * 订购来源  0：营业厅门户 1：线上 2：其他
     * 必传
     */
    private String orderSource;

    /**
     * 活动id
     */
    private String planId;

    /**
     * 支付方式  1:手机号 2：微信  3：支付宝  4:和支付 5：其他
     */
    private String payWay;

    /**
     * 必选
     */
    private String sign;


}
