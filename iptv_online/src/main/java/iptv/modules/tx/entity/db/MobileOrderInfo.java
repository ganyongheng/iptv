package iptv.modules.tx.entity.db;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import iptv.modules.base.entity.vo.MobileOrderInfoBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@Data
//生成equals/hashCode方法时包含其父类的属性
@EqualsAndHashCode(callSuper = true)
@TableName("mobile_order_info")
public class MobileOrderInfo extends MobileOrderInfoBase implements Serializable {

    private static final long serialVersionUID = 5698114663610122844L;

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
    @TableField(value="order_createtime")
    private Date orderCreatetime;

    /**
    * 腾讯订单发货时间
    */
   /* @TableField(exist = false)*/
    @TableField(value="order_confirmtime")
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
}