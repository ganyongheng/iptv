package iptv.modules.music.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author wyy
 * Date 2022/4/8 11:48
 **/
@Data
@TableName("znyx_user_login_record")
public class ZnyxUserLoginRecord implements Serializable {
    private static final long serialVersionUID = 5327622213605802506L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    //手机号
    private String phone;
    //渠道
    private String source;
/*    //上一次登录时间
    private Date lastLoginDate;*/
    //登陆时间
    private Date loginDate;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
}
