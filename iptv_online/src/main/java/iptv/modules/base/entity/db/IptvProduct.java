package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("iptv_product")
public class IptvProduct implements Serializable {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String comboDes;

    private String comboPkgId;

    private Date createTime;

    private Date endTime;

    private String operator;

    private Integer price;

    private String productType;

    private String productCode;

    private String productDes;

    private String productDuration;

    private String productName;

    private String source;

    private Date startTime;

    private String thirdCode;

    private String pThirdCode;

    private String thirdSystem;

    private Date updateTime;

    private String vodType;

    private String isAutopay;

    private String pProductCode;

    private Integer months;

    private String txThirdCode;
}