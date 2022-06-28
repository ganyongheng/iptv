package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_base_param")
public class SysBaseParam  implements Serializable {

    @TableId(value = "pk_id", type = IdType.AUTO)
    private Long pkId;  			//主键

    @TableField(value = "param_name")
    private String paramName;		//参数名称

    @TableField(value = "param_key")
    private String paramKey;		//参数KEY

    @TableField(value = "param_value")
    private String paramValue;		//参数值

    @TableField(value = "param_desc")
    private String paramDesc;		//参数描述

    @TableField(value = "status")
    private Integer status;			//状态:0-无效,1-有效

    @TableField(value = "create_date")
    private Date createDate;		//创建时间
}
