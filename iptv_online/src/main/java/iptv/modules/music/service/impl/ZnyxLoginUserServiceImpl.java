package iptv.modules.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.music.entity.db.ZnyxLoginUser;
import iptv.modules.music.mapper.ZnyxLoginUserMapper;
import org.springframework.stereotype.Service;

/**
 * Author wyy
 * Date 2022/4/8 14:30
 **/
@Service
public class ZnyxLoginUserServiceImpl extends ServiceImpl<ZnyxLoginUserMapper, ZnyxLoginUser> {
    public ZnyxLoginUser getUserByPhone(String phone) {
        QueryWrapper<ZnyxLoginUser> wrapper = new QueryWrapper<>();
        wrapper.eq("phone", phone);
        return this.baseMapper.selectOne(wrapper);
    }
}
