package iptv.modules.base.entity.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author wyy
 * Date 2022/3/21 10:00
 **/
@Data
public class MobileOrderInfoBase implements Serializable {
    private static final long serialVersionUID = 3113098313087974989L;
    //渠道代码
    @TableField(exist = false)
    private String source;

    //产品包唯一id
    @TableField(exist = false)
    private String vippkg;

    //第三方产品包唯一id
    @TableField(exist = false)
    private String thirdVippkg;

    //腾讯视频用户id
    @TableField(exist = false)
    private String vuid;

    //用户id
    @TableField(exist = false)
    private String userid;

    //平台的订单号
    @TableField(exist = false)
    private String extTraceno;

    //本地订单号
    @TableField(exist = false)
    private String traceno;

    //订单支付时间
    @TableField(exist = false)
    private Date innerPayTime;

    //腾讯平台的订单号
    @TableField(exist = false)
    private String orderId;

    //订单参考价格
    @TableField(exist = false)
    private Integer orderPrice;

    //订单处理状态
    @TableField(exist = false)
    private Integer orderStatus;

    //订单类型
    @TableField(exist = false)
    private String orderType;

    //订单开通服务内容
    @TableField(exist = false)
    private String service;

    //腾讯订单创建时间
    @TableField(exist = false)
    private Date orderCreatetime;

    //腾讯订单发货时间
    @TableField(exist = false)
    private Date orderConfirmtime;

    //订单处理状态
    @TableField(exist = false)
    private String status;

    //是否顺延
    @TableField(exist = false)
    private String isAutopay;

    //订单处理结果描述
    @TableField(exist = false)
    private String msg;

    //需要透传给腾讯发货接口
    @TableField(exist = false)
    private String extReserved;
}
