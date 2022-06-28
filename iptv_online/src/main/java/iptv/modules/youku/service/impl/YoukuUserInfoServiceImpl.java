/**
* @mbg.generated
* generator on Thu Mar 17 11:48:42 GMT+08:00 2022
*/
package iptv.modules.youku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.youku.entity.db.YoukuUserInfo;
import iptv.modules.youku.mapper.YoukuUserInfoMapper;
import org.springframework.stereotype.Service;

@Service
public class YoukuUserInfoServiceImpl extends ServiceImpl<YoukuUserInfoMapper, YoukuUserInfo> {

    public YoukuUserInfo getMobelUserInfo(String userId, String source) {
        QueryWrapper<YoukuUserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("source", source);
        return this.baseMapper.selectOne(wrapper);
    }
}