package iptv.modules.aiqiyi.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("aiqiyi_user_black_info")
public class AiqiyiUserBlackInfo implements Serializable {

    private static final long serialVersionUID = 3818145703417602964L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 渠道：snm_yidong
     */
    @TableField(value = "source")
    private String source;

    @TableField(value = "user_id")
    private String userId;

    /**
     * 视频用户id
     */
    @TableField(value = "vuid")
    private String vuid;

    @TableField(value = "creat_time")
    private Date creatTime;
}