package iptv.modules.base.entity.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author wyy
 * Date 2022/3/21 10:40
 **/
@Data
public class MobileUserProductBase implements Serializable {
    private static final long serialVersionUID = -2903255166910790030L;
    @TableField(exist = false)
    private String userId;

    @TableField(exist = false)
    private String vuid;

    @TableField(exist = false)
    private String productType;

    @TableField(exist = false)
    private String productCode;

    @TableField(exist = false)
    private String thirdCode;

    @TableField(exist = false)
    private String comboPkgId;

    @TableField(exist = false)
    private String pProductCode;

    @TableField(exist = false)
    private Date stime;

    @TableField(exist = false)
    private Date etime;

    @TableField(exist = false)
    private Date thirdEtime;

    @TableField(exist = false)
    private String pstatus;

    @TableField(exist = false)
    private String feemonth;

    @TableField(exist = false)
    private String source;

    @TableField(exist = false)
    private String isAutopay;

    @TableField(exist = false)
    private String cooperatorCode;

    @TableField(exist = false)
    private Date createTime;

    @TableField(exist = false)
    private Date updateTime;

    @TableField(exist = false)
    private Integer ignoreFlag;
}
