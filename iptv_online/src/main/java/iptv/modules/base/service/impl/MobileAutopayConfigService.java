package iptv.modules.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.base.entity.db.MobileAutopayConfig;
import iptv.modules.base.mapper.MobileAutopayConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MobileAutopayConfigService extends ServiceImpl<MobileAutopayConfigMapper, MobileAutopayConfig> {

    @Autowired
    MobileAutopayConfigMapper autopayConfigMapper;


    /**
     * 获取配置source与url
     * key为source value为url
     * @return
     */
    public Map<String,String> getMapBySource(){
        Map<String,String> map = new HashMap<>();
        List<MobileAutopayConfig> list = autopayConfigMapper.selectList(null);
        if(list == null || list.size() == 0 ){
            return map ;
        }
        for(MobileAutopayConfig m : list){
            map.put(m.getSource(), m.getUrl());
        }
        return map ;
    }

    /**
     *
     * @param source
     * @return
     */
    public MobileAutopayConfig getConfigBySource(String source) {
        QueryWrapper<MobileAutopayConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("source", source);
        return this.baseMapper.selectOne(wrapper);
    }
}
