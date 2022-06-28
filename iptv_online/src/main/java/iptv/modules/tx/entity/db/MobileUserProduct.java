package iptv.modules.tx.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import iptv.modules.base.entity.vo.MobileUserProductBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@Data
//生成equals/hashCode方法时包含其父类的属性
@EqualsAndHashCode(callSuper = true)
@TableName("mobile_user_product")
public class MobileUserProduct extends MobileUserProductBase implements Serializable {

    private static final long serialVersionUID = 2861748658215920322L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
    * 渠道代码
    */
    private String source;

    /**
    * 腾讯视频用户id
    */
    private String vuid;

    /**
    * 用户id
    */
    private String userId;

    /**
    * 产品类别
    */
    private String productType;

    /**
    * 产品包唯一id
    */
    private String productCode;

    /**
    * 第三方产品包唯一id
    */
    private String thirdCode;

    /**
    * 状态
    */
    private String pstatus;

    /**
    * 订购关系开始时间
    */
    private Date stime;

    private Date etime;

    /**
    * 第三方权益到期时间
    */
    private Date thirdEtime;

    /**
    * 下个出账月
    */
    private String feemonth;

    /**
    * 是否顺延
    */
    private String isAutopay;

    /**
    * 下个月续费产品
    */
    private String pProductCode;

    /**
    * 套餐唯一id
    */
    private String comboPkgId;

    /**
    * 创建时间
    */
    private Date createTime;

    /**
    * 更新时间
    */
    private Date updateTime;

    /**
    * tencent--腾讯；youku--优酷
    */
    private String cooperatorCode;

    /**
    * 忽略标志，新流程中该字段置1，则自动续费时忽略发货该用户
    */
    private Integer ignoreFlag;
}