package iptv.modules.music.entity.db;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("znyx_product_config")
public class ZnyxProductConfig implements Serializable {

    private static final long serialVersionUID = 3818125708417602964L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 订购关系同步通知第三方url
     */
    @TableField(value = "sync_account_url")
    private String syncAccountUrl;

    /**
     * 渠道
     */
    @TableField(value = "source")
    private String source;

    /**
     * 签名盐值
     */
    @TableField(value = "sign_key")
    private String signKey;
}
