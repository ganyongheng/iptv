package iptv.modules.base.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import iptv.common.BusinessException;
import iptv.config.redis.RedisCache;
import iptv.modules.tx.entity.db.MobileUserInfo;
import iptv.modules.tx.service.impl.MobileInterService;
import iptv.modules.tx.service.impl.MobileUserInfoServiceImpl;
import iptv.util.ConstDef;
import iptv.util.HCommUtil;
import iptv.util.HttpUtils;
import iptv.util.SysConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MobileUserInterService {

    @Autowired
    private SysConfig sysConfig;

    @Autowired
    private RedisCache redisCache;

    private static Logger log = LoggerFactory.getLogger(MobileUserInterService.class);

    private static Logger logHttpErr = LoggerFactory.getLogger("getVuidErrLogger");
    private static final Logger logViud = LoggerFactory.getLogger("httpUtilsLogger");

    @Autowired
    private MobileInterService mobileInterService;

    @Autowired
    private MobileUserInfoServiceImpl mobileUserInfoServiceImpl;


    /**
     *
     * @param status 0表示异步  1表示同步
     * @param count 表示插入数量
     * @throws Exception
     */
    public void pushVuidRedisCache(int status,int count) throws Exception{
        if(ConstDef.SYNC_STATUS_OFF==status){
            //异步
            GetVuidThread getVuidThread=new GetVuidThread(count);
            getVuidThread.start();
        }else{
            //同步
            getVuidList(count);
        }
    }

    public  void getVuidList(int count) throws Exception{

        List<Map<String,String>> maplist=new ArrayList<>();
        List<List<Map<String,String>>> maplistGroup=new ArrayList<>();
        int num=0;
        String randomCode = HCommUtil.getRandomCodeByTime();
        for(int i=0 ;i <count;i++){
            //这里分批放入redis，不然数据量太大，导致很久redis没有数据
            Map<String,String> map=new HashMap<>();
            map.put("tp_userid", randomCode+i);
            maplist.add(map);
            num++;
            //最后一次少于100也不要了
            if(num>1000){
                maplistGroup.add(maplist);
                num=0;
                maplist=new ArrayList<>();
            }
            if(i==count-1){
                //说明是到最后一条数据了
                maplistGroup.add(maplist);
            }
        }
        for (int i=0 ;i <maplistGroup.size();i++) {
            List<Map<String, String>> list = maplistGroup.get(i);
            log.info("本次放入redis的vuid数量一共"+maplistGroup.size()+"个组"+";一次："+list.size()+"数据;"+"现在执行到"+i);
            List<Map<String, String>> mapvuid = getVuid(list);
            putVuidRedisCache(mapvuid);
        }
    }


    public void putVuidRedisCache(List<Map<String, String>> mapvuid){
        log.info("本次放入redis的vuid数量"+mapvuid.size());
        for (Map<String, String> map : mapvuid) {
            try {
                String vuid = map.get("vuid");
                String vtoken = map.get("vtoken");
                String tp_userid = map.get("tp_userid");
                if(StringUtils.isNotBlank(vuid)&&StringUtils.isNotBlank(vtoken)&&StringUtils.isNotBlank(tp_userid)){
                    //这里必须严格控制，格式不然取出分析会报错
                    String redisStr=tp_userid+"##"+vtoken+"##"+vuid;
                    //redis选取左进右出的方式
                    //redisCache.lpushCache(ConstDef.KEY_REDIS, redisStr);
                    //更换新key
                    redisCache.lpushCache(ConstDef.KEY_REDIS_NEW, redisStr);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                log.error("redisCache放入缓存失败");
                e.printStackTrace();
            }

        }
    }

    class GetVuidThread extends Thread{

        private int count;
        public GetVuidThread(int count) {
            // TODO Auto-generated constructor stub
            this.count=count;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                getVuidList(count);
                //说明数据已经跑完了，可以把开关打开了
                redisCache.del(ConstDef.KEY_REDIS_STATUS);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public List<Map<String,String>> getVuid(List<Map<String,String>> maplist){
        List<Map<String,String>> returnlistmap=new ArrayList<>();
        for (Map<String, String> map : maplist) {
            /*Map<String, String> paramsMap=new HashMap<>();*/
            Map<String, Object> paramsMap2=new HashMap<>();
            String doGet2="";
            try {
                String tp_userid = map.get("tp_userid");
                String accessToken = mobileInterService.getAccessToken();
                if(StringUtils.isEmpty(accessToken)){
                    throw new BusinessException("获取不到accessToken","0");
                }
				/*paramsMap.put("access_token", accessToken);
				paramsMap.put("tp_userid", tp_userid);*/
                paramsMap2.put("access_token", accessToken);
                paramsMap2.put("tp_userid", tp_userid);
                paramsMap2.put("version", "1");
                paramsMap2.put("format", "json");
                String tx_vuid_url = sysConfig.getTX_VUID_URL();
                /*doGet = HttpGetPostUtil.doGet(tx_vuid_url, paramsMap);*/
                doGet2 = HttpUtils.doGet(tx_vuid_url, paramsMap2);
                if(StringUtils.isNotBlank(doGet2)){
                    Map<String,String> mapresult=new HashMap<>();
                    JSONObject parse = JSONObject.parseObject(doGet2);
                    JSONObject result = parse.getJSONObject("result");
                    if("0".equals(result.getString("code"))){
                        JSONObject data = parse.getJSONObject("data");
                        if(data==null){
                            throw new BusinessException("获取不到VUID","0");
                        }
                        String  vtoken= data.getString("vtoken");
                        String  vuid= data.getString("vuid");
                        if(StringUtils.isNotBlank(vtoken)&&StringUtils.isNotBlank(vuid)){
							/*code为0不一定成功{
								"data": {
									"vtoken": "",
									"vuid": 0
								},
								"result": {
									"code": 0,
									"costtime": 0,
									"msg": "access token too short",
									"ret": 2
								}
							}*/
                            mapresult.put("vuid", vuid);
                            mapresult.put("vtoken", vtoken);
                            mapresult.put("tp_userid", tp_userid);
                            returnlistmap.add(mapresult);
                        }
                    }else{
                        logHttpErr.info("请求参数"+paramsMap2+"返回结果"+doGet2);
                        throw new BusinessException("获取不到vuid","0");

                    }
                }else{
                    logHttpErr.info("请求参数"+paramsMap2+"返回结果"+doGet2);
                    throw new BusinessException("获取不到vuid","0");
                }
				/*String code = doGet.get("code");
				if("200".equals(code)){
					String message = doGet.get("message");
					JSONObject jsonobject = JSONObject.parseObject(message);
					JSONObject object = (JSONObject)jsonobject.get("data");
					JSONObject result = (JSONObject)jsonobject.get("result");
					String codeReslut = result.get("code")+"";
					if("0".equals(codeReslut)){
						Object vuid = object.get("vuid");
						Object vtoken = object.get("vtoken");
						Map<String, String> mapresult=new HashMap<>();
						if(vuid!=null){
							mapresult.put("vuid", vuid+"");
						}
						if(vtoken!=null){
							mapresult.put("vtoken", vtoken+"");
						}
						mapresult.put("tp_userid", tp_userid);
						returnlistmap.add(mapresult);
					}else{
						throw new BusinessException("获取不到vuid","0");
					}

				}else{
					throw new BusinessException("获取不到vuid","0");
				}*/
                logViud.info("已经获取了+"+returnlistmap.size()+"个vuid");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                logHttpErr.info("请求失败返回结果："+JSON.toJSONString(doGet2)+"；请求参数："+JSON.toJSONString(paramsMap2)+e.getCause(),e);
            }

        }
        return returnlistmap;
    }

    public void getVuidForOnce(Map<String, String> mapRetuen, String userId, String source) throws Exception {
        MobileUserInfo mobelUserInfo = getMobileUserInfoNoRedis();
        if (mobelUserInfo == null) {
            //说明获取vuid失败
            throw new Exception("获取不到vuid");
        }
        mapRetuen.put("code", "0");
        mapRetuen.put("msg", "请求成功");
        mapRetuen.put("vuid", mobelUserInfo.getVuid());
        mapRetuen.put("vtoken", mobelUserInfo.getVtoken());
        mobelUserInfo.setUserId(userId);
        mobelUserInfo.setSource(source);
        mobelUserInfo.setCreatTime(new Date());
        //将数据存数据库
        mobileUserInfoServiceImpl.save(mobelUserInfo);
    }

    public MobileUserInfo getMobileUserInfoByRedis() throws Exception {
        String rpopCache = redisCache.rpopCache(ConstDef.KEY_REDIS, String.class);
        String[] split = rpopCache.split("##");
        MobileUserInfo mobelUserInfo = new MobileUserInfo();
        mobelUserInfo.setRandomCode(split[0]);
        mobelUserInfo.setVtoken(split[1]);
        mobelUserInfo.setVuid(split[2]);
        return mobelUserInfo;
    }

    public MobileUserInfo getMobileUserInfoNoRedis() throws Exception{
        //获取一条vuid
        List<Map<String,String>> maplist=new ArrayList<>();
        String userId = HCommUtil.getRandomCode();
        Map<String,String> map=new HashMap<>();
        map.put("tp_userid", userId);
        maplist.add(map);
        List<Map<String, String>> vuidList = getVuid(maplist);
        if(vuidList.size()>0){
            //说明没有拿到vuid
            MobileUserInfo mobelUserInfo=new MobileUserInfo();
            mobelUserInfo.setRandomCode(userId);
            for (Map<String, String> mapVuid : vuidList) {
                String vuid = mapVuid.get("vuid")+"";
                String vtoken = mapVuid.get("vtoken")+"";
                if(StringUtils.isNotBlank(vuid)){
                    mobelUserInfo.setVtoken(vtoken);
                    mobelUserInfo.setVuid(vuid);
                }else{
                    throw new Exception();
                }
            }
            return mobelUserInfo;
        }
        return null;
    }
}
