/**
* @mbg.generated
* generator on Thu Mar 17 14:10:12 GMT+08:00 2022
*/
package iptv.modules.tx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.tx.entity.db.MobileUserProduct;
import iptv.modules.tx.mapper.MobileUserProductMapper;
import iptv.util.BizConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class MobileUserProductServiceImpl  extends ServiceImpl<MobileUserProductMapper, MobileUserProduct> {

    @Autowired
    private MobileUserProductMapper userProductMapper;

    /**
     * 用户产品权益表更新etime时间
     * @param etime 结束时间
     * @param source 来源
     * @param userid
     * @param product_type 产品类型
     * @param vuid
     * @return
     */
    @Transactional
    public int updateMobileUserProduct(Date etime , String source, String userid, String product_type, String vuid){
        return userProductMapper.updateEtimeMobileUserProduct(etime,source,userid,product_type,vuid);
    }

    public MobileUserProduct getMobileUserProduct(String userId , String vuid, String source){
        QueryWrapper<MobileUserProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        wrapper.eq("vuid",vuid);
        wrapper.eq("source",source);
        return this.baseMapper.selectOne(wrapper);
    }

    public MobileUserProduct getMobileUserProductByProductType(String userId , String vuid, String productType, String source){
        QueryWrapper<MobileUserProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        wrapper.eq("vuid",vuid);
        wrapper.eq("product_type",productType);
        wrapper.eq("source",source);
        return this.baseMapper.selectOne(wrapper);
    }

    /**
     * 获取需要出账的数据
     * @param current_time
     * @param nums
     * @return
     */
    public List<MobileUserProduct> getMobileUserProductAccountList(String current_time, int nums ){
        return userProductMapper.getMobileUserProductAccountList(current_time.substring(0, 10),nums,
                BizConstant.MobileUserProduct.Pstatus_Useid,BizConstant.MobileUserProduct.IsAutoPay_Yes);
    }


    public MobileUserProduct getMobileUserProductByVuid(String vuid, String productCode, String source) {
        LambdaQueryWrapper<MobileUserProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MobileUserProduct::getVuid, vuid);
        queryWrapper.eq(MobileUserProduct::getProductCode, productCode);
        queryWrapper.eq(MobileUserProduct::getSource, source);
        MobileUserProduct mobileUserProduct = baseMapper.selectOne(queryWrapper);
        return mobileUserProduct;
    }

    public void updateEntity(MobileUserProduct mobileUserProduct) {
        baseMapper.updateById(mobileUserProduct);
    }

    public Date getVuidEtime(String vuid, String source, String productType) {
        QueryWrapper<MobileUserProduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("vuid", vuid);
        queryWrapper.eq("source", source);
        queryWrapper.eq("product_type", productType);
        queryWrapper.select("max(etime) as etime");
        Map<String, Object> map = this.getMap(queryWrapper);
        if (map != null) {
            return (Date) map.get("etime");
        } else {
            return null;
        }
    }
}