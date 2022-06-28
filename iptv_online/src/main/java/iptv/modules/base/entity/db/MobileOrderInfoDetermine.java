package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author wyy
 * Date 2022/3/18 17:23
 **/
@Data
@TableName("mobile_order_info_determine")
public class MobileOrderInfoDetermine implements Serializable {

    private static final long serialVersionUID = -5607304319596479232L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

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

    //订单支付时间
    private Date innerPayTime;

    //订单类型
    private String orderType;

    //是否顺延
    private String isAutopay;

    //需要透传给腾讯发货接口
    private String extReserved;
}
