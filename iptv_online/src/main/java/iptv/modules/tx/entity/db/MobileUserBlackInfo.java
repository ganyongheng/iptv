package iptv.modules.tx.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Author wyy
 * Date 2022/3/17 16:06
 **/
@Data
@TableName("mobile_user_black_info")
public class MobileUserBlackInfo {
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

    @TableField(value = "operate")
    private String operate;

    @TableField(value = "creat_time")
    private Date creatTime;

    @TableField(value = "random_code")
    private String randomCode;


}
