package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("mobile_autopay_config")
public class MobileAutopayConfig implements Serializable {

    private static final long serialVersionUID = 2999076487076379348L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(value = "source")
    private String source;

    @TableField(value = "status")
    private Integer status;

    @TableField(value = "url")
    private String url;
}
