package iptv.modules.music.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author wyy
 * Date 2022/4/7 18:00
 **/
@Data
@TableName("znyx_product")
public class ZnyxProduct implements Serializable {
    private static final long serialVersionUID = -2180257304346840743L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    //产品编码
    private String productCode;
    //产品名称
    private String productName;
    //产品价格 单位分
    private Integer price;
    //活动价格 单位分
    private Integer vipPrice;
    //产品类型 svod_1(包月),tvod（单点）,svod_3(包季),svod_12(包年)
    private String vodType;
    //产品描述
    private String productDescribe;
    //source
    private String source;
    //是否自动续费 0:非自动续费 1:是
    private Integer isAuto;
    //有效期单位 1:小时 2:天 3:月
    private Integer cycleUnit;
    //有效期
    private Integer cycleCount;
    //图标路径
    private String icon;
    //背景图路径
    private String background;
    //是否需要二次验证 0:非 1:是
    private Integer isCheck;
    //权益类型
    private String vipType;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
}
