package iptv.modules.tx.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Author wyy
 * Date 2022/3/17 15:30
 **/
@Data
@TableName("mobile_user_info")
public class MobileUserInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer pkId;

    @TableField(value = "source")
    private String source;

    @TableField(value = "user_id")
    private String userId;

    @TableField(value = "vuid")
    private String vuid;

    @TableField(value = "vtoken")
    private String vtoken;

    @TableField(value = "creat_time")
    private Date creatTime;

    @TableField(value = "random_code")
    private String randomCode;
}
