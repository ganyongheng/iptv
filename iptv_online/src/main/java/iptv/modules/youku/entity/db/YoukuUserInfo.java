package iptv.modules.youku.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("youku_user_info")
public class YoukuUserInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(value = "creat_time")
    private Date creatTime;

    @TableField(value = "source")
    private String source;

    @TableField(value = "user_id")
    private String userId;

    @TableField(value = "vuid")
    private String vuid;
}