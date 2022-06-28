/**
 * @mbg.generated generator on Thu Mar 17 10:52:24 GMT+08:00 2022
 */
package iptv.modules.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.common.BusinessException;
import iptv.modules.base.entity.db.IptvProduct;
import iptv.modules.base.mapper.IptvProductMapper;
import iptv.util.BizConstant;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IptvProductServiceImpl extends ServiceImpl<IptvProductMapper, IptvProduct> {

    /**
     * 根据多个产品编码和渠道编码 获取产品列表信息
     * @param productcode
     * @param source
     * @return
     */
    public IptvProduct getIptvProduct(String productcode, String source, String thirdSystem) {
        QueryWrapper<IptvProduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_code", productcode);
        queryWrapper.eq("source", source);
        queryWrapper.eq("third_system", thirdSystem);
        return this.baseMapper.selectOne(queryWrapper);
    }

    /**
     * 根据产品编码和渠道编码 获取产品信息
     * @param productcodes
     * @param source
     * @return
     * @throws Exception
     */
    public List<IptvProduct> getIptvProduct(String[] productcodes, String source) throws Exception {
        List<IptvProduct> iptvProductList = new ArrayList<IptvProduct>();
        for (String product_code : productcodes) {
            IptvProduct iptvProduct = this.getIptvProduct(product_code, source, BizConstant.Code.Order.Cooperator_Code_Tencent);
            if (null == iptvProduct) {
                throw new BusinessException(BizConstant.Code.Order.Cooperator_Code_Tencent + "根据product_code【" + product_code + "】, souce【" + source + "】没有找到对应的配置产品");
            } else {
                iptvProductList.add(iptvProduct);
            }
        }
        return iptvProductList;
    }


    /**
     * 获取产品列表
     * @param source
     * @return
     */
    public List<IptvProduct> getIptvProductList(String source) {
        QueryWrapper<IptvProduct> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("source", source);
        return this.baseMapper.selectList(queryWrapper);
    }
}