package iptv.modules.youku.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("youku_order_info")
public class YoukuOrderInfo implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
    * 渠道代码
    */
    private String source;

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
    * 腾讯平台的订单号
    */
    private String orderId;

    /**
    * 入表创建时间
    */
    private Date createTime;

    /**
    * 订购时间,格式yyyyMMddHHmmss
    */
    private Date orderConfirmtime;

    /**
    * 订单处理状态0-正常,1-失败,3-退订/退续订,4-手动取消
    */
    private String status;

    /**
    * 1-应用内,2-线下,3-续费,4-活动
    */
    private String orderType;

    private static final long serialVersionUID = 1L;

}