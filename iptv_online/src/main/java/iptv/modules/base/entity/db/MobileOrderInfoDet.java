package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import iptv.modules.base.entity.vo.MobileOrderInfoBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * Author wyy
 * Date 2022/3/18 17:53
 **/
@Data
//生成equals/hashCode方法时包含其父类的属性
@EqualsAndHashCode(callSuper = true)
@TableName("mobile_order_info_det")
public class MobileOrderInfoDet extends MobileOrderInfoBase implements Serializable {
    private static final long serialVersionUID = 8259523414059018991L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    //渠道代码
    private String source;

    //产品包唯一id
    private String vippkg;

    //第三方产品包唯一id
    private String thirdVippkg;

    //腾讯视频用户id
    private String vuid;

    //用户id
    private String userid;

    //平台的订单号
    private String extTraceno;

    //本地订单号
    private String traceno;

    //订单支付时间
    private Date innerPayTime;

    //腾讯平台的订单号
    private String orderId;

    //订单参考价格
    private Integer orderPrice;

    //订单处理状态
    private Integer orderStatus;

    //订单类型
    private String orderType;

    //订单开通服务内容
    private String service;

    //腾讯订单创建时间
    private Date orderCreatetime;

    //腾讯订单发货时间
    private Date orderConfirmtime;

    //订单处理状态
    private String status;

    //是否顺延
    private String isAutopay;

    //订单处理结果描述
    private String msg;

    //需要透传给腾讯发货接口
    private String extReserved;

    private Date feetime;

    private Integer ignoreFlag;

}
