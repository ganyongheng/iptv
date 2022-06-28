package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

/**
 * Author wyy
 * Date 2022/3/18 16:42
 **/
@Data
@TableName("mobile_source_compute_mode")
public class MobileSourceComputeMode implements Serializable {

    private static final long serialVersionUID = 4421362206052355771L;
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(value = "compute_mode")
    private String computeMode;

    @TableField(value = "days")
    private Integer days;

    @TableField(value = "warn_buy_days")
    private Integer warnBuyDays;

    @TableField(value = "source")
    private String source;

    @TableField(value = "deduction_time")
    private String deductionTime;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;
}
