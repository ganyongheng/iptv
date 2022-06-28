package iptv.modules.tx.process;

import com.alibaba.fastjson.JSONObject;
import iptv.modules.tx.factory.getaccesstoken.GetAccessTokenProcessFactory;
import iptv.util.BizConstant;
import iptv.util.HttpUtils;
import iptv.util.SysConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Author wyy
 * Date 2022/3/16 17:22
 **/
@Component
public class GetAccessTokenProcess implements InitializingBean {

    @Autowired
    private SysConfig sysConfig;

    /**
     *公共接口必传参数校验，子类也可以覆写
     *
     * @param req
     * @throws Exception
     */
    public void checkBaseReparam(String req) throws Exception {
    }

    /**
     * 子类可以自定义添加参数校验
     *
     * @param req
     * @throws Exception
     */
    public void addCheckBaseReparam(String req) throws Exception {
    }

    /**
     *获取腾讯token
     * @return
     * @throws Exception
     */
    public String getAccessTokenFromTx() throws Exception{
        String url = sysConfig.getTX_ACCESS_TOKEN_URL();
        String appid = sysConfig.getTX_APPID();
        String appkey = sysConfig.getTX_APPKEY();
        if (StringUtils.isBlank(url) || StringUtils.isBlank(appid) || StringUtils.isBlank(appkey)) {
            //log.error("请求腾讯获取access_token时，请求配置缺少！");
            throw new Exception("请求腾讯获取access_token时，请求配置缺少！");
        }
        String result = HttpUtils.doGet(url + "&appid=" + appid + "&appkey=" + appkey);
        if (StringUtils.isBlank(result)) {
            //log.error("请求腾讯获取access_token返回为空！");
            throw new Exception("请求腾讯获取access_token返回为空！");
        }
        JSONObject json = JSONObject.parseObject(result);
        JSONObject json_result = JSONObject.parseObject(json.getString("result"));
        if (!BizConstant.Code.Result_Code_Success_Num_0.equals(json_result.getString("code"))) {
            // 返回报错
            //log.error("请求腾讯获取access_token返回报错：" + json_result.getString("code") + ":" + json_result.getString("msg"));
            throw new Exception("请求腾讯获取access_token返回报错：" + json_result.getString("code") + ":" + json_result.getString("msg"));
        }
        JSONObject data = JSONObject.parseObject(json.getString("data"));
        String accessToken = data.getString("access_token");
        return accessToken;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        GetAccessTokenProcessFactory.create("tencent",this);
    }
}
