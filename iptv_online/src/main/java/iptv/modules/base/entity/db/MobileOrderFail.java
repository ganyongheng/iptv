package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("mobile_order_fail")
public class MobileOrderFail implements Serializable {

    private static final long serialVersionUID = -3628492130299263773L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Date createTime;

    private String extTraceno;

    private Date innerPayTime;

    private String memo;

    private Integer nums;

    private String source;

    private String status;

    private Date updateTime;

    @TableField("userId")
    private String userId;

    private String vippkg;

    private String thirdVippkg;

    private String vuid;

    private String isAutoAccount;

    private String productCode;

    private String traceno;

    private String isAutopay;
}