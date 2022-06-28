/**
* @mbg.generated
* generator on Thu Mar 17 14:10:12 GMT+08:00 2022
*/
package iptv.modules.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.base.entity.db.MobileOrderFail;
import iptv.modules.base.mapper.MobileOrderFailMapper;
import org.springframework.stereotype.Service;

@Service
public class MobileOrderFailServiceImpl extends ServiceImpl<MobileOrderFailMapper, MobileOrderFail> {

    public MobileOrderFail getMobileOrderFailByExtTraceno(String ext_traceno, String source) {
        QueryWrapper<MobileOrderFail> wrapper = new QueryWrapper();
        wrapper.eq("ext_traceno", ext_traceno);
        wrapper.eq("source", source);
        return this.baseMapper.selectOne(wrapper);
    }
}