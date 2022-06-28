/**
* @mbg.generated
* generator on Thu Mar 17 11:36:22 GMT+08:00 2022
*/
package iptv.modules.aiqiyi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.aiqiyi.entity.db.AiqiyiUserInfo;
import iptv.modules.aiqiyi.mapper.AiqiyiUserInfoMapper;
import iptv.modules.youku.entity.db.YoukuUserInfo;
import org.springframework.stereotype.Service;

@Service
public class AiqiyiUserInfoServiceImpl extends ServiceImpl<AiqiyiUserInfoMapper, AiqiyiUserInfo> {

    public AiqiyiUserInfo getMobelUserInfo(String userId, String source) {
        QueryWrapper<AiqiyiUserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("source", source);
        return this.baseMapper.selectOne(wrapper);
    }
}