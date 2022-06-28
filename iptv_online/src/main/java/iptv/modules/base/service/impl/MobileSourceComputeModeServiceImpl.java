package iptv.modules.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.base.entity.db.MobileSourceComputeMode;
import iptv.modules.base.mapper.MobileSourceComputeModeMapper;
import org.springframework.stereotype.Service;

/**
 * Author wyy
 * Date 2022/3/18 16:49
 **/
@Service
public class MobileSourceComputeModeServiceImpl extends ServiceImpl<MobileSourceComputeModeMapper, MobileSourceComputeMode> {
    public MobileSourceComputeMode getBySource(String source) {
        QueryWrapper<MobileSourceComputeMode> wrapper = new QueryWrapper<>();
        wrapper.eq("source", source);
        return this.baseMapper.selectOne(wrapper);
    }
}
