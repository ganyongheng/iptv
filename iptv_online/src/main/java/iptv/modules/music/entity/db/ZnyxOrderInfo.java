package iptv.modules.music.entity.db;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zx
 * @date 2022年04月11日 14:36
 */
@Data
@TableName("znyx_order_info")
public class ZnyxOrderInfo implements Serializable {

    private static final long serialVersionUID = -6871826299971888379L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    //产品source
    private String source;

    //合作方流水号
    private String seqno;

    //登录账号
    private String loginId;

    //登录方式
    private String loginType;

    //产品编码
    private String vippkg;

    //产品名称
    private String vippkgName;

    //支付金额
    private Integer total;

    //是否续费
    private Integer isAuto;

    //购买数量
    private Integer buyNum;

    //支付类型
    private String payType;

    //支付方式
    private String payWay;

    //平台订单号
    private String traceno;

    //支付状态
    private String status;

    //创建时间
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    //更新时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    //支付时间
    private Date payTime;

    //内容类型
    private String contentType;

    //内容标识
    private String contentCode;

    //内容名称
    private String contentName;

}
