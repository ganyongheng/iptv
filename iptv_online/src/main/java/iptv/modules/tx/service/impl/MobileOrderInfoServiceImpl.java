/**
* @mbg.generated
* generator on Thu Mar 17 14:10:12 GMT+08:00 2022
*/
package iptv.modules.tx.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.tx.entity.db.MobileOrderInfo;
import iptv.modules.tx.mapper.MobileOrderInfoMapper;
import org.springframework.stereotype.Service;

@Service
public class MobileOrderInfoServiceImpl  extends ServiceImpl<MobileOrderInfoMapper, MobileOrderInfo> {

    public MobileOrderInfo getMobileOrderInfoByExtTraceno(String ext_traceno, String source) {
        QueryWrapper<MobileOrderInfo> wrapper = new QueryWrapper();
        wrapper.eq("ext_traceno", ext_traceno);
        wrapper.eq("source", source);
        return this.baseMapper.selectOne(wrapper);
    }

    public int updateByWrapper(MobileOrderInfo entity,UpdateWrapper<MobileOrderInfo> wrapper) {
        int update = baseMapper.update(entity, wrapper);
        return update;
    }
}