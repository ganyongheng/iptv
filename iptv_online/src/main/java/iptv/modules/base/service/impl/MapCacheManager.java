package iptv.modules.base.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import iptv.modules.base.entity.db.SysBaseParam;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class MapCacheManager {
    private final static Log log = LogFactory.getLog(MapCacheManager.class);

    private volatile long updateTime = 0L;// 更新缓存时记录的时间

    private volatile boolean updateFlag = false;// 正在更新时的阀门，为false时表示当前没有更新缓存，为true时表示当前正在更新缓存

    private static Map<String, Map<String, String>> cacheMap = new ConcurrentHashMap<>();// 缓存容器

    private final int MAX_TIME = 60 * 1000; // 60秒刷新一次

    @Autowired
    private SysCacheService sysCacheService;


    /**
     * 装载缓存
     */
    private void LoadCache() {
        this.updateFlag = true;// 正在更新
        this.cacheMap.clear();

        /********** 数据处理，将数据放入cacheMap缓存中 **begin ******/
        try {
            List<SysBaseParam> listModels = sysCacheService.querySysBaseParamMap();
            //抓取KEY
            Set<String> keySet = new HashSet<>();
            for(SysBaseParam mm: listModels){
                keySet.add(mm.getParamName());
            }

            //组装数据
            for(String name : keySet){
                Map<String, String> paramMap = new HashMap<String, String>();
                for(SysBaseParam mm: listModels){
                    if(name.equals(mm.getParamName())){
                        paramMap.put(mm.getParamKey(), mm.getParamValue());
                    }
                }
                cacheMap.put(name, paramMap);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        /********** 数据处理，将数据放入cacheMap缓存中 ***end *******/
        this.updateFlag = false;// 更新已完成

    }

    /**
     * 返回缓存对象
     *
     * @return
     * @throws InterruptedException
     */
    public Map<String, Map<String, String>> getMapCache() throws InterruptedException {

        long currentTime = System.currentTimeMillis();

        if (this.updateFlag) {// 前缓存正在更新
            TimeUnit.SECONDS.sleep(1); // 休息一秒再次获取
            if (this.updateFlag) {// 前缓存正在更新
                log.info("cache is Instance .....");
                return null;
            }
        }
        if (this.IsTimeOut(currentTime)) {// 如果当前缓存正在更新或者缓存超出时限，需重新加载
            synchronized (this) {
                this.LoadCache();
                this.updateTime = currentTime;
            }
        }
        return this.cacheMap;
    }

    /**
     * 是否超时
     *
     * @param currentTime
     * @return
     */
    private boolean IsTimeOut(long currentTime) {
        boolean isTimeOut = ((currentTime - this.updateTime) > MAX_TIME);// 超过时限，超时
        return isTimeOut;
    }

}
