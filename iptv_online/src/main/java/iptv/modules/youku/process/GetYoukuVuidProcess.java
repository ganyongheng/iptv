package iptv.modules.youku.process;

import com.alibaba.fastjson.JSONObject;
import iptv.common.BossException;
import iptv.config.redis.RedisCache;
import iptv.modules.tx.entity.db.MobileUserBlackInfo;
import iptv.modules.tx.entity.db.MobileUserInfo;
import iptv.modules.tx.factory.getvuid.GetVuidProcessFactory;
import iptv.modules.tx.process.GetVuidProcess;
import iptv.modules.youku.entity.db.YoukuUserBlackInfo;
import iptv.modules.youku.entity.db.YoukuUserInfo;
import iptv.modules.youku.service.impl.YoukuUserBlackInfoServiceImpl;
import iptv.modules.youku.service.impl.YoukuUserInfoServiceImpl;
import iptv.util.ConstDef;
import iptv.util.HCommUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * Author wyy
 * Date 2022/3/17 17:54
 **/
@Component
public class GetYoukuVuidProcess extends GetVuidProcess {

    private static Logger log = LoggerFactory.getLogger(GetYoukuVuidProcess.class);
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private YoukuUserInfoServiceImpl youkuUserInfoService;
    @Autowired
    private YoukuUserBlackInfoServiceImpl youkuUserBlackInfoService;

    @Override
    public void afterPropertiesSet() throws Exception {
        GetVuidProcessFactory.create("youku", this);
    }

    /**
     * @param req
     * @param mapRetuen
     */
    public void getVuidFromThird(JSONObject req, Map<String, String> mapRetuen, Boolean lock) throws Exception {
        String optype = req.getString("optype");
        String source = req.getString("source");
        String userId = req.getString("userId");
        try {
            lock = redisCache.setnxWithExptime("MobileUserGetvuidLock_" + userId, userId, 30);
        } catch (Exception e) {
            e.printStackTrace();
            //log.error("redis连接失败");
            throw new BossException();
        }
        if (lock) {
            YoukuUserInfo youKuUserInfo = youkuUserInfoService.getMobelUserInfo(userId, source);
            if (ConstDef.KEY_GET_VUID_STATUS_ADD.equals(optype)) {
                // userId与source是唯一索引
                if (youKuUserInfo != null) {
                    mapRetuen.put("code", "0");
                    mapRetuen.put("msg", "请求成功");
                    mapRetuen.put("vuid", youKuUserInfo.getVuid());
                    log.info(userId + "是老用户");
                } else {
                    try {
                        long generate = redisCache.generate("YOU_KU_ALong");
                        String randomCodeByTime = HCommUtil.getRandomCodeByTime(generate);
                        YoukuUserInfo userInfo = new YoukuUserInfo();
                        mapRetuen.put("code", "0");
                        mapRetuen.put("msg", "请求成功");
                        mapRetuen.put("vuid", randomCodeByTime);
                        userInfo.setUserId(userId);
                        userInfo.setSource(source);
                        userInfo.setCreatTime(new Date());
                        userInfo.setVuid(randomCodeByTime);
                        // 将数据存数据库
                        youkuUserInfoService.save(userInfo);
                    }catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        // 将异常抛出去
                        throw e;
                    }
                    log.info(userId + "------优酷新用户");
                }
            } else if (ConstDef.KEY_GET_VUID_STATUS_DELET.equals(optype)) {
                // 删除数据,存到black表里
                if (youKuUserInfo != null) {
                    YoukuUserBlackInfo youKuUserBlackInfo = new YoukuUserBlackInfo();
                    youKuUserBlackInfo.setCreatTime(new Date());
                    youKuUserBlackInfo.setSource(youKuUserInfo.getSource());
                    youKuUserBlackInfo.setUserId(youKuUserInfo.getUserId());
                    youKuUserBlackInfo.setVuid(youKuUserInfo.getVuid());
                    youkuUserBlackInfoService.save(youKuUserBlackInfo);
                    youkuUserInfoService.removeById(youKuUserInfo.getId());
                    mapRetuen.put("code", "0");
                    mapRetuen.put("msg", "注销成功");
                    mapRetuen.put("vuid", youKuUserInfo.getVuid());
                } else {
                    mapRetuen.put("code", "1");
                    mapRetuen.put("msg", "注销成功失败,该用户不存在!");
                }
            }
        } else {
            mapRetuen.put("code", "1");
            mapRetuen.put("msg", "已经存在请求正在获取该用户的vuid，请稍后再试。");
        }
    }
}
