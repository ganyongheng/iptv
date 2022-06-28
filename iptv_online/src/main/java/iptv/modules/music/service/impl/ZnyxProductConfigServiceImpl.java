package iptv.modules.music.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.music.entity.db.ZnyxProductConfig;
import iptv.modules.music.mapper.ZnyxProductConfigMapper;
import org.springframework.stereotype.Service;


@Service
public class ZnyxProductConfigServiceImpl extends ServiceImpl<ZnyxProductConfigMapper, ZnyxProductConfig> {

    public ZnyxProductConfig getOneBySource(String source){
        return this.baseMapper.getOneBySource(source);
    }
}
