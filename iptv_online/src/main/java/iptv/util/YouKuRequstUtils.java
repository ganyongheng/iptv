package iptv.util;

import com.alibaba.fastjson.JSON;
import com.youku.ott.openapi.sdk.DefaultOttOpenapiClient;
import com.youku.ott.openapi.sdk.OttOpenapiResponse;
import com.youku.ott.openapi.sdk.request.CommonOttOpenapiResquest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;


/**
 * 优酷提供的是sdk的jar包需要手动打到maven本地仓库中引用
 * mvn install:install-file -Dfile=sak的jar包目录 -DgroupId=com.youku.ott -DartifactId=openapi -Dversion=1.0 -Dpackaging=jar
 * 请求优酷sdk工具类
 */
@Component
public class YouKuRequstUtils {

    @Autowired
    private SysConfig sysConfig;

    protected Logger logger = LoggerFactory.getLogger(YouKuRequstUtils.class);

    private static final String logSuccessMsgFormat ="方法:[%s],参数:[%s],状态码:[%s],状态信息:[%s],结果BizResp[%s]";

    private static Map<String,String> channelIds = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            String channel_Ids = sysConfig.getChannel_Ids();
            String[] split = channel_Ids.split("&&");
            for (String str : split) {
                String[] split2 = str.split("#");
                channelIds.put(split2[0], split2[1]);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 请求优酷接口通用方法
     * @param methodName  优酷接口名称
     * @param requestParams  请求接口body参数
     * @return
     */
    public ServerResponse doRequest(String methodName, Map<String,Object> requestParams ){
        //构建业务参数结束
        String bizParam = JSON.toJSONString(requestParams);

        CommonOttOpenapiResquest commonOttOpenapiResquest = new CommonOttOpenapiResquest();
        commonOttOpenapiResquest.setApiMethodName(methodName);
        commonOttOpenapiResquest.setBizParam(bizParam);
        try {
            DefaultOttOpenapiClient defaultOttOpenapiClient = new DefaultOttOpenapiClient(sysConfig.getYOUKU_CONFIRM_ORDER_URL(),
                    sysConfig.getYOUKU_APPKEY(),sysConfig.getYOUKU_APPSECRET(),sysConfig.getYOUKU_SIGNTYPE());
            OttOpenapiResponse ottOpenapiResponse = defaultOttOpenapiClient.execute(commonOttOpenapiResquest);
            //网关返回成功
            if (ottOpenapiResponse.isGateWaySuccess()) {
                //业务返回成功
                if (ottOpenapiResponse.isBizSuccess()) {
                    //处理业务逻辑
                    ServerResponse serverResponse=new ServerResponse(Constant.ServerResponseStatus.SUCCESS,"成功",ottOpenapiResponse);
                    logger.info(String.format(YouKuRequstUtils.logSuccessMsgFormat,methodName,bizParam,serverResponse.getStatus(),serverResponse.getMsg(),serverResponse.getData()));
                    return serverResponse;
                } else {
                    //业务返回失败
                    logger.info(String.format(YouKuRequstUtils.logSuccessMsgFormat,methodName,bizParam,Constant.ServerResponseStatus.FAIL,ottOpenapiResponse.getSubMsg(),ottOpenapiResponse));
                    return new ServerResponse(Constant.ServerResponseStatus.FAIL,ottOpenapiResponse.getSubMsg(),ottOpenapiResponse);
                }
            } else {
                logger.info(String.format(YouKuRequstUtils.logSuccessMsgFormat,methodName,bizParam,Constant.ServerResponseStatus.FAIL,ottOpenapiResponse.getSubMsg(),ottOpenapiResponse));
                return new ServerResponse(Constant.ServerResponseStatus.FAIL,ottOpenapiResponse.getSubMsg());
            }
        }catch (Exception e) {
            e.printStackTrace();
            logger.error(String.format(YouKuRequstUtils.logSuccessMsgFormat,methodName,bizParam,Constant.ServerResponseStatus.FAIL,"请求异常:"+e.getMessage(),null));
            OttOpenapiResponse ottOpenapiResponse=new OttOpenapiResponse();
            ottOpenapiResponse.setCode("9999");
            ottOpenapiResponse.setMsg("请求异常:"+e.getMessage());
            return new ServerResponse(Constant.ServerResponseStatus.FAIL,"请求异常:"+e.getMessage(),ottOpenapiResponse);
        }
    }

    public String getChannelIdBySource(String source){
        return channelIds.get(source);
    }

}
