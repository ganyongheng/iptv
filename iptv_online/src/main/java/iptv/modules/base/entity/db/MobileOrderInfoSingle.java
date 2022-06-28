package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import iptv.modules.base.entity.vo.MobileOrderInfoBase;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("mobile_order_info_single")
public class MobileOrderInfoSingle extends MobileOrderInfoBase implements Serializable {


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
    * 腾讯视频用户id
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
    * 腾讯平台的订单号
    */
    private String orderId;

    /**
    * 订单参考价格
    */
    private Integer orderPrice;

    /**
    * 订单处理状态
    */
    private Integer orderStatus;

    /**
    * 订单开通服务内容
    */
    private String service;

    /**
    * 腾讯订单创建时间
    */
    private Date orderCreatetime;

    /**
    * 腾讯订单发货时间
    */
    private Date orderConfirmtime;

    /**
    * 订单处理状态
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
    * tencent--腾讯；youku--优酷
    */
    private String cooperatorCode;

    private static final long serialVersionUID = 1L;

}