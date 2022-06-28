/**
* @mbg.generated
* generator on Thu Mar 17 11:48:42 GMT+08:00 2022
*/
package iptv.modules.youku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.youku.entity.db.MobileOrderInfoYouku;
import iptv.modules.youku.mapper.MobileOrderInfoYoukuMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MobileOrderInfoYoukuServiceImpl extends ServiceImpl<MobileOrderInfoYoukuMapper, MobileOrderInfoYouku> {
    /**
     * 根据ext_traceno查产品
     * @param ext_traceno
     * @return
     */
    public MobileOrderInfoYouku getProductByExtTraceno(String ext_traceno) {
        QueryWrapper<MobileOrderInfoYouku> wrapper = new QueryWrapper<>();
        wrapper.eq("ext_traceno", ext_traceno);
        return this.baseMapper.selectOne(wrapper);
    }


    /**
     *
     * @param ext_traceno
     * @param source
     * @return
     */
    public MobileOrderInfoYouku getMobileOrderInfoYoukuByExtTraceno(String ext_traceno, String source) {
        QueryWrapper<MobileOrderInfoYouku> wrapper = new QueryWrapper<>();
        wrapper.eq("ext_traceno", ext_traceno);
        wrapper.eq("source", source);
        return this.baseMapper.selectOne(wrapper);
    }
}