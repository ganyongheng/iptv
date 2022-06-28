package iptv.modules.youku.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import iptv.modules.base.entity.vo.MobileOrderInfoBase;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("mobile_order_info_youku")
public class MobileOrderInfoYouku extends MobileOrderInfoBase implements Serializable {

    private static final long serialVersionUID = 1077183397022074270L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
    * 渠道代码
    */
    private String source;

    /**
    * 产品包唯一id
    */
    private String vippkg;

    /**
    * 第三方产品唯一id
    */
    private String thirdVippkg;

    /**
    * 优酷视频用户id
    */
    private String vuid;

    /**
    * 用户id
    */
    private String userid;

    /**
    * 移动平台的订单号
    */
    private String extTraceno;

    /**
    * 移动订单支付时间
    */
    private Date innerPayTime;

    /**
    * 优酷平台的订单号
    */
    private String orderId;

    /**
    * 订单处理状态 0:发货失败，2：发货成功，4：退费成功
    */
    private String status;

    /**
    * 订单处理结果描述
    */
    private String msg;

    private String extReserved;

    private String isAutopay;

    private String orderType;

    private String traceno;

    private Date feetime;

    /**
    *  退款时间
    */
    private Date innerRefundTime;
}