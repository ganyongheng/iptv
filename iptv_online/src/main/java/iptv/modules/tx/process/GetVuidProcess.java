package iptv.modules.tx.process;

import com.alibaba.fastjson.JSONObject;
import iptv.common.BossException;
import iptv.common.BusinessException;
import iptv.common.CheckUtils;
import iptv.config.redis.RedisCache;
import iptv.modules.base.service.impl.MobileUserInterService;
import iptv.modules.tx.entity.db.MobileUserBlackInfo;
import iptv.modules.tx.entity.db.MobileUserInfo;
import iptv.modules.tx.factory.getvuid.GetVuidProcessFactory;
import iptv.modules.tx.service.impl.MobileInterService;
import iptv.modules.tx.service.impl.MobileUserBlackInfoServiceImpl;
import iptv.modules.tx.service.impl.MobileUserInfoServiceImpl;
import iptv.util.ConstDef;
import iptv.util.SysConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * Author wyy
 * Date 2022/3/17 14:41
 **/
@Component
public class GetVuidProcess implements InitializingBean {

    private static Logger log = LoggerFactory.getLogger(GetVuidProcess.class);

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MobileUserInfoServiceImpl mobelUserInfoService;

    @Autowired
    private MobileUserBlackInfoServiceImpl mobileUserBlackInfoService;

    @Autowired
    private MobileInterService mobileInterService;

    @Autowired
    private MobileUserInterService mobileUserInterService;

    @Autowired
    private SysConfig sysConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        GetVuidProcessFactory.create("tencent", this);
    }

    /**
     * 公共接口必传参数校验，子类也可以覆写
     *
     * @param req
     * @throws Exception
     */
    public void checkBaseReparam(JSONObject req) throws Exception {
        try {
            CheckUtils.checkEmpty(req.getString("optype"), "optype不能为空");
            CheckUtils.checkEmpty(req.getString("source"), "source不能为空");
            CheckUtils.checkEmpty(req.getString("userId"), "userId不能为空");
            if (!ConstDef.KEY_GET_VUID_STATUS_ADD.equals(req.getString("optype")) && !ConstDef.KEY_GET_VUID_STATUS_DELET.equals(req.getString("optype"))) {
                throw new BusinessException("传参不正确:1为申请用户vuid;2为注销用户vuid!");
            }
            addCheckBaseReparam(req);
        } catch (BusinessException b) {
            throw new BusinessException(b.getMessage());
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * 子类可以自定义添加参数校验
     *
     * @param req
     */
    public void addCheckBaseReparam(JSONObject req) throws Exception {

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
            log.error("redis连接失败");
            throw new BossException();
        }
        if (lock) {
            MobileUserInfo mobileUserInfo = mobelUserInfoService.getMobelUserInfo(userId, source);
            /*
             * 1为申请用户vuid, 2为注销用户vuid
             */
            if (ConstDef.KEY_GET_VUID_STATUS_ADD.equals(optype)) {
                // userId与source是唯一索引
                if (mobileUserInfo != null) {
                    mapRetuen.put("code", "0");
                    mapRetuen.put("msg", "请求成功");
                    mapRetuen.put("vuid", mobileUserInfo.getVuid());
                    mapRetuen.put("vtoken", mobileUserInfo.getVtoken());
                    log.info(userId + "是老用户");
                } else {
                    try {
                        // 说明没有数据的,需要重新分配
                        long lLen = 0;
                        try {
                            lLen = redisCache.lLen(ConstDef.KEY_REDIS);
                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                            log.error("redis连接失败");
                            throw new BossException();
                        }
                        if (lLen == 0) {
                            // 说明redis缓存数据取完了
                            mobileUserInterService.getVuidForOnce(mapRetuen, userId, source);
                        } else {
                            // 成功拿到锁之后 ，给锁加个超时时间，防止突然中断服务，导致锁不能释放，默认30秒失效
                            MobileUserInfo userInfo = mobileUserInterService.getMobileUserInfoByRedis();
                            mapRetuen.put("code", "0");
                            mapRetuen.put("msg", "请求成功");
                            mapRetuen.put("vuid", userInfo.getVuid());
                            mapRetuen.put("vtoken", userInfo.getVtoken());
                            userInfo.setUserId(userId);
                            userInfo.setSource(source);
                            userInfo.setCreatTime(new Date());
                            // 将数据存数据库
                            mobelUserInfoService.save(userInfo);
                        }
                    } catch (BossException e) {
                        // 说明redis不能连接
                        mobileUserInterService.getVuidForOnce(mapRetuen, userId, source);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 将异常抛出去
                        throw e;
                    }
                    log.info(userId + "是新用户");
                }
            } else if (ConstDef.KEY_GET_VUID_STATUS_DELET.equals(optype)){
                // 删除数据,存到black表里
                if (mobileUserInfo != null) {
                    MobileUserBlackInfo mobelUserBlackInfo = new MobileUserBlackInfo();
                    mobelUserBlackInfo.setCreatTime(new Date());
                    mobelUserBlackInfo.setOperate("");
                    mobelUserBlackInfo.setSource(mobileUserInfo.getSource());
                    mobelUserBlackInfo.setUserId(mobileUserInfo.getUserId());
                    mobelUserBlackInfo.setVtoken(mobileUserInfo.getVtoken());
                    mobelUserBlackInfo.setVuid(mobileUserInfo.getVuid());
                    mobelUserBlackInfo.setRandomCode(mobileUserInfo.getRandomCode());
                    //记黑名单表
                    mobileUserBlackInfoService.save(mobelUserBlackInfo);
                    //根据id删除用户记录
                    mobelUserInfoService.removeById(mobileUserInfo.getPkId());
                    mapRetuen.put("code", "0");
                    mapRetuen.put("msg", "注销成功");
                    mapRetuen.put("vuid", mobileUserInfo.getVuid());
                    mapRetuen.put("vtoken", mobileUserInfo.getVtoken());
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
