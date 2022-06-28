package iptv.modules.base.service.impl;

import java.util.List;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import iptv.modules.base.entity.db.SysBaseParam;
import iptv.modules.base.mapper.SysBaseParamMapper;
import iptv.util.ConstDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 查询系统参数等公共静态数据
 * @author DerekLee
 *
 */
@Service
public class SysCacheService {
    private static Logger log = LoggerFactory.getLogger(SysCacheService.class);

    @Autowired
    private SysBaseParamMapper sysBaseParamMapper;

    /**
     * 查询系统参数
     * @return
     */
    public List<SysBaseParam> querySysBaseParamMap() throws Exception{
        LambdaQueryWrapper<SysBaseParam> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //状态是有效的
        lambdaQueryWrapper.eq(SysBaseParam::getStatus,ConstDef.STATUS_VALID);
        List<SysBaseParam> sysBaseParamList = sysBaseParamMapper.selectList(lambdaQueryWrapper);
        return sysBaseParamList;
    }


}
