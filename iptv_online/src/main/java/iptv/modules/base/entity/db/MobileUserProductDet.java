package iptv.modules.base.entity.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

@TableName("mobile_user_product_det")
public class MobileUserProductDet implements Serializable {

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
    * 订购关系开始时间
    */
    private Date stime;

    /**
    * 订购关系开始时间
    */
    private Date etime;

    /**
    * 移动流水号
    */
    private String extTraceno;

    /**
    * 腾讯流水号
    */
    private String orderId;

    /**
    * 下个出账月
    */
    private String feemonth;

    /**
    * 是否顺延
    */
    private String isAutopay;

    /**
    * 下月续费产品包唯一id
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

    private String traceno;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getVuid() {
        return vuid;
    }

    public void setVuid(String vuid) {
        this.vuid = vuid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getThirdCode() {
        return thirdCode;
    }

    public void setThirdCode(String thirdCode) {
        this.thirdCode = thirdCode;
    }

    public Date getStime() {
        return stime;
    }

    public void setStime(Date stime) {
        this.stime = stime;
    }

    public Date getEtime() {
        return etime;
    }

    public void setEtime(Date etime) {
        this.etime = etime;
    }

    public String getExtTraceno() {
        return extTraceno;
    }

    public void setExtTraceno(String extTraceno) {
        this.extTraceno = extTraceno;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getFeemonth() {
        return feemonth;
    }

    public void setFeemonth(String feemonth) {
        this.feemonth = feemonth;
    }

    public String getIsAutopay() {
        return isAutopay;
    }

    public void setIsAutopay(String isAutopay) {
        this.isAutopay = isAutopay;
    }

    public String getpProductCode() {
        return pProductCode;
    }

    public void setpProductCode(String pProductCode) {
        this.pProductCode = pProductCode;
    }

    public String getComboPkgId() {
        return comboPkgId;
    }

    public void setComboPkgId(String comboPkgId) {
        this.comboPkgId = comboPkgId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getTraceno() {
        return traceno;
    }

    public void setTraceno(String traceno) {
        this.traceno = traceno;
    }
}