/**
* @mbg.generated
* generator on Thu Mar 17 14:10:12 GMT+08:00 2022
*/
package iptv.modules.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.base.entity.db.MobileOrderInfoSingle;
import iptv.modules.base.mapper.MobileOrderInfoSingleMapper;
import org.springframework.stereotype.Service;

@Service
public class MobileOrderInfoSingleServiceImpl  extends ServiceImpl<MobileOrderInfoSingleMapper, MobileOrderInfoSingle> {
    public MobileOrderInfoSingle getMobileOrderInfoSingleByExtTraceno(String ext_traceno, String source) {

        LambdaQueryWrapper<MobileOrderInfoSingle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MobileOrderInfoSingle::getExtTraceno, ext_traceno);
        queryWrapper.eq(MobileOrderInfoSingle::getSource, source);
        queryWrapper.orderByDesc(MobileOrderInfoSingle::getId);
        return baseMapper.selectOne(queryWrapper);
    }

}