package iptv.modules.aiqiyi.process;

import com.alibaba.fastjson.JSONObject;
import iptv.common.BossException;
import iptv.config.redis.RedisCache;
import iptv.modules.aiqiyi.entity.db.AiqiyiUserBlackInfo;
import iptv.modules.aiqiyi.entity.db.AiqiyiUserInfo;
import iptv.modules.aiqiyi.service.impl.AiqiyiUserBlackInfoServiceImpl;
import iptv.modules.aiqiyi.service.impl.AiqiyiUserInfoServiceImpl;
import iptv.modules.tx.factory.getvuid.GetVuidProcessFactory;
import iptv.modules.tx.process.GetVuidProcess;
import iptv.modules.youku.entity.db.YoukuUserBlackInfo;
import iptv.modules.youku.entity.db.YoukuUserInfo;
import iptv.modules.youku.process.GetYoukuVuidProcess;
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
 * Date 2022/3/18 14:20
 **/
@Component
public class GetAiqiyiVuidProcess extends GetVuidProcess {
    private static Logger log = LoggerFactory.getLogger(GetAiqiyiVuidProcess.class);
    @Autowired
    private RedisCache redisCache;

    @Autowired
    private AiqiyiUserInfoServiceImpl aiqiyiUserInfoService;

    @Autowired
    private AiqiyiUserBlackInfoServiceImpl aiqiyiUserBlackInfoService;

    @Override
    public void afterPropertiesSet() throws Exception {
        GetVuidProcessFactory.create("aiqiyi", this);
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
            AiqiyiUserInfo aiqiyiUserInfo = aiqiyiUserInfoService.getMobelUserInfo(userId, source);
            if (ConstDef.KEY_GET_VUID_STATUS_ADD.equals(optype)) {
                // userId与source是唯一索引
                if (aiqiyiUserInfo != null) {
                    mapRetuen.put("code", "0");
                    mapRetuen.put("msg", "请求成功");
                    mapRetuen.put("vuid", aiqiyiUserInfo.getVuid());
                    log.info(userId + "是老用户");
                } else {
                    try {
                        long generate = redisCache.generate("Aiqiyi_ALong");
                        String randomCodeByTime = HCommUtil.getRandomCodeByTime(generate);
                        AiqiyiUserInfo userInfo = new AiqiyiUserInfo();
                        mapRetuen.put("code", "0");
                        mapRetuen.put("msg", "请求成功");
                        mapRetuen.put("vuid", randomCodeByTime);
                        userInfo.setUserId(userId);
                        userInfo.setSource(source);
                        userInfo.setCreatTime(new Date());
                        userInfo.setVuid(randomCodeByTime);
                        // 将数据存数据库
                        aiqiyiUserInfoService.save(userInfo);
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
                if (aiqiyiUserInfo != null) {
                    AiqiyiUserBlackInfo aiqiyiUserBlackInfo = new AiqiyiUserBlackInfo();
                    aiqiyiUserBlackInfo.setCreatTime(new Date());
                    aiqiyiUserBlackInfo.setSource(aiqiyiUserInfo.getSource());
                    aiqiyiUserBlackInfo.setUserId(aiqiyiUserInfo.getUserId());
                    aiqiyiUserBlackInfo.setVuid(aiqiyiUserInfo.getVuid());
                    aiqiyiUserBlackInfoService.save(aiqiyiUserBlackInfo);
                    aiqiyiUserInfoService.removeById(aiqiyiUserInfo.getId());
                    mapRetuen.put("code", "0");
                    mapRetuen.put("msg", "注销成功");
                    mapRetuen.put("vuid", aiqiyiUserInfo.getVuid());
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
