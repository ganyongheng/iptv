/**
* @mbg.generated
* generator on Thu Mar 17 11:36:22 GMT+08:00 2022
*/
package iptv.modules.aiqiyi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.aiqiyi.entity.db.MobileUserProductAiqiyi;
import iptv.modules.aiqiyi.mapper.MobileUserProductAiqiyiMapper;
import iptv.modules.youku.entity.db.MobileUserProductYouku;
import iptv.modules.tx.entity.db.MobileUserProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

@Service
public class MobileUserProductAiqiyiServiceImpl extends ServiceImpl<MobileUserProductAiqiyiMapper, MobileUserProductAiqiyi> {


    @Autowired
    private MobileUserProductAiqiyiMapper userProductAiqiyiMapper;

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
    public int updateMobileUserProductaiqiyi(Date etime , String source, String userid, String product_type, String vuid){
        return userProductAiqiyiMapper.updateEtimeMobileUserProduct(etime,source,userid,product_type,vuid);
    }

    /**
     *
     * @param userId
     * @param vuid
     * @param productType
     * @param source
     * @return
     */
    public MobileUserProductAiqiyi getMobileUserProductByProductTypeAiqiyi(String userId , String vuid, String productType, String source){
        QueryWrapper<MobileUserProductAiqiyi> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        wrapper.eq("vuid",vuid);
        wrapper.eq("product_type",productType);
        wrapper.eq("source",source);
        return this.baseMapper.selectOne(wrapper);
    }


    public Date getVuidEtime(String vuid, String source, String productType) {
        QueryWrapper<MobileUserProductAiqiyi> queryWrapper = new QueryWrapper<>();
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