package iptv.modules.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.music.entity.db.ZnyxLoginUser;
import iptv.modules.music.entity.db.ZnyxOrderInfo;
import iptv.modules.music.mapper.ZnyxOrderInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zx
 * @date 2022年04月11日 15:25
 */
@Service
public class ZnyxOrderInfoServiceImpl extends ServiceImpl<ZnyxOrderInfoMapper,ZnyxOrderInfo> {

    public ZnyxOrderInfo getZnyxOrderInfoBySeqno(String seqno) {
        QueryWrapper<ZnyxOrderInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("seqno", seqno);
        return this.baseMapper.selectOne(wrapper);
    }
}
