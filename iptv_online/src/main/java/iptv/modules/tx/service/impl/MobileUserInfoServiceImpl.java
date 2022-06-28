/**
* @mbg.generated
* generator on Thu Mar 17 14:10:12 GMT+08:00 2022
*/
package iptv.modules.tx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.tx.entity.db.MobileUserInfo;
import iptv.modules.tx.mapper.MobileUserInfoMapper;
import org.springframework.stereotype.Service;

@Service
public class MobileUserInfoServiceImpl  extends ServiceImpl<MobileUserInfoMapper, MobileUserInfo> {
    public MobileUserInfo getMobelUserInfo(String userId, String source) {
        QueryWrapper<MobileUserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("source", source);
        return this.baseMapper.selectOne(wrapper);
    }
}