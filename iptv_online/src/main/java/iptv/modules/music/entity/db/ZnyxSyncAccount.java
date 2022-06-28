package iptv.modules.music.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 订购关系同步实体
 */
@Data
@TableName("znyx_sync_account")
public class ZnyxSyncAccount implements Serializable {
    private static final long serialVersionUID = 5327822213625802506L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 合作方来源
     */
    @TableField(value = "source")
    private String source;

    /**
     * 自动续费产品包唯一id
     */
    @TableField(value = "vippkg")
    private String vippkg;

    /**
     * 用户手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 运营商平台的订单号
     */
    @TableField(value = "ext_traceno")
    private String extTraceno;

    /**
     * 自动续费订单号
     */
    @TableField(value = "ext_traceno_cvod")
    private String extTracenoCvod;

    /**
     * 是否自动续费
     * 0: 非自动续费
     * 1: 自动续费
     */
    @TableField(value = "auto")
    private Integer auto;

    /**
     *产品实际价格
     *单位：分
     */
    @TableField(value = "price")
    private Integer price;

    /**
     *支付总金额
     *单位：分
     */
    @TableField(value = "total")
    private Integer total;

    /**
     *退费金额
     *单位：分
     */
    @TableField(value = "refund_total")
    private Integer refundTotal;

    /**
     * 订购关系生效时间
     */
    @TableField(value = "begin_time")
    private Date beginTime;

    /**
     * 退费后或者续费后订购关系失效时间
     */
    @TableField(value = "end_time")
    private Date endTime;

    /**
     * 用户取消自动续费或者退订时间
     */
    @TableField(value = "cancel_time")
    private Date cancelTime;

    /**
     *订单同步类型
     * 1:订购
     * 2:退费立即生效（包年，包季退费情况）
     * 3：取消自动续费
     */
    @TableField(value = "sync_type")
    private Integer syncType;

    /**
     * 支付方式
     * 1: 话费、
     * 2: 微信、
     * 3: 支付宝
     */
    @TableField(value = "pay_type")
    private Integer payType;

    /**
     * 活动描述
     */
    @TableField(value = "vip_msg")
    private String vipMsg;

    /**
     * 活动ID
     */
    @TableField(value = "vip_id")
    private Integer vipId;

    /**
     * 重发次数
     */
    @TableField(value = "nums")
    private Integer nums;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 通知状态
     */
    @TableField(value = "notify_status")
    private String notifyStatus;




}
