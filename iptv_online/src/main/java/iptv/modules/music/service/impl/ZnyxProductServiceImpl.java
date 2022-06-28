package iptv.modules.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.music.entity.db.ZnyxProduct;
import iptv.modules.music.mapper.ZnyxProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Author wyy
 * Date 2022/4/8 9:23
 **/
@Service
public class ZnyxProductServiceImpl extends ServiceImpl<ZnyxProductMapper, ZnyxProduct> {
    public List<ZnyxProduct> getZnyxProduct(Map map) {
        return this.baseMapper.selectByMap(map);
    }

    public ZnyxProduct getZnyxProductByProductCode(String productCode) {
        QueryWrapper<ZnyxProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("product_code", productCode);
        return this.baseMapper.selectOne(wrapper);
    }
}
