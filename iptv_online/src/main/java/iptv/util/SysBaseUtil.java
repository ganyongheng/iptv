package iptv.util;

import iptv.common.BossException;
import iptv.modules.base.service.impl.MapCacheManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



import java.util.Map;


/**
 * 系统参数获取的公共方法 该util类不允许随便新增方法
 * @author DerekLee
 *
 */
@Component
public class SysBaseUtil {
    private static Log log = LogFactory.getLog(SysBaseUtil.class);
    @Autowired
    private MapCacheManager mapCacheManager;

    /**
     * 根据参数名获取参数信息
     * @param envProd
     */
    public String getRunEnv(String envProd) throws Exception{
        if(StringUtils.isBlank(envProd)){
            throw new BossException("envProd名称不能为空");
        }
        //查询参数
        Map<String, String> map = this.getSysBaseParam("ENV_RUN");
        String envRun = map.get(envProd);
        log.info("cache runEnv:"+envRun);
        //返回运行环境
        return envRun;
    }


    /**
     * 根据参数名获取参数信息
     * @param paramName
     */
    public String getSysBaseParam(String paramName, String key) throws Exception{
        if(StringUtils.isBlank(paramName)){
            throw new BossException("paramName名称不能为空");
        }
        if(StringUtils.isBlank(key)){
//			throw new BossException("key名称不能为空");
            return "";
        }

        Map<String, String> map = this.getSysBaseParam(paramName);
        if(map==null || map.isEmpty() || !map.containsKey(key)){
            return null;
        }
        String paranValue = map.get(key);

        //查询参数
        log.info("cache paramName:"+paramName+", key:"+key+", paranValue: "+ paranValue);
        //返回
        return paranValue;
    }


    /**
     * 查询系统参数返回的map
     * @param paramName
     * @return
     * @throws Exception
     */
    public Map<String, String> getSysBaseParam(String paramName) throws Exception {

        Map<String, Map<String, String>> cacheMap = mapCacheManager.getMapCache();

        if(!cacheMap.containsKey(paramName)){
            return null;
        }

        Map<String, String> paramMap = cacheMap.get(paramName);
        return paramMap;
    }
}