package iptv.modules.base.controller;

import com.alibaba.fastjson.JSONObject;
import iptv.modules.tx.entity.vo.AccessToken;
import iptv.modules.tx.service.impl.MobileInterService;
import iptv.util.BizConstant;
import iptv.util.SysBaseUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;

@Controller
@RequestMapping("/sys")
public class SystemInterController {
    private static Logger log = LoggerFactory.getLogger(SystemInterController.class.getName());

    @Autowired
    private iptv.modules.tx.service.impl.MobileInterService mobileInterService;

     private SimpleDateFormat dateFormat = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SysBaseUtil sysBaseUtil;
    /**
     * 获取调用腾讯接口需要的token
     * 获取本地缓存 ，非 redis
     * @param req
     * @return
     */
    @RequestMapping(value = "getAccessToken",method = RequestMethod.POST)
    @ResponseBody
    public String getAccessTokenWithExpiretime(@RequestBody JSONObject req){

        JSONObject resp=new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg","OK");

        try {
            if(StringUtils.isBlank(AccessToken.getAccesstoken())){
                resp.put("accessToken", "");
            }else{
                resp.put("accessToken", AccessToken.getAccesstoken());
            }
            if(null==AccessToken.getExpiretime()){
                resp.put("expireTime", "");
            }else{
                resp.put("expireTime", dateFormat.format(AccessToken.getExpiretime()));
            }
        } catch (Exception e) {
            log.error("【getAccessToken】查询AccessToken出错："+e.getCause(),e);
            resp.put("code", BizConstant.Code.Result_Code_Systemrror);
            resp.put("msg","【getAccessToken】查询AccessToken出错："+e.getMessage());
            return JSONObject.toJSONString(resp);
        }

        return JSONObject.toJSONString(resp);

    }

    /**
     * 重置调用腾讯接口需要的token
     * 获取本地缓存 ，非 redis
     * @param req
     * @return
     */
    @RequestMapping(value = "reSetAccessToken",method = RequestMethod.POST)
    @ResponseBody
    public String reSetAccessTokenWithExpiretime(@RequestBody JSONObject req){

        JSONObject resp=new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg","OK");

        try {
            AccessToken.setAccesstoken(null);
            AccessToken.setExpiretime(null);
        } catch (Exception e) {
            log.error("【reSetAccessToken】重置AccessToken出错："+e.getCause(),e);
            resp.put("code", BizConstant.Code.Result_Code_Systemrror);
            resp.put("msg","【reSetAccessToken】重置AccessToken出错："+e.getMessage());
            return JSONObject.toJSONString(resp);
        }

        return JSONObject.toJSONString(resp);

    }

    /**
     * 后台刷新第三方token
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "refreshMobileToken", method = RequestMethod.POST)
    @ResponseBody
    public String refreshMobileToken(@RequestBody JSONObject req) {

        JSONObject resp = new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");

        String source = req.getString("source");

        String url = "";
        try {
            url = sysBaseUtil.getSysBaseParam("SYNC_TOKEN_TO_THIRD_SYSTEM_URL", source);
        } catch (Exception e1) {
            log.error("后台刷新token获取渠道" + source + "的url出错：" + e1.getCause(), e1);
            resp.put("code", BizConstant.Code.Result_Code_Systemrror);
            resp.put("msg", "后台刷新token获取渠道" + source + "的url出错：" + e1.getMessage());
            return JSONObject.toJSONString(resp);
        }

        try {
            if (StringUtils.isNotBlank(url)) {
                String[] url_list = url.split(";");
                for (String req_url : url_list) {
                    resp = mobileInterService.getAccessTokenFromTxForMobile(source, req_url,log, "手动刷新");
                }
            }
            if (!BizConstant.Code.Result_Code_Success_Num_0.equals(resp.getString("code"))) {
                throw new Exception(resp.getString("msg"));
            }
        } catch (Exception e) {
            log.error("后台刷新第三方token出错：" + e.getCause(), e);
            resp.put("code", BizConstant.Code.Result_Code_Systemrror);
            resp.put("msg", "后台刷新第三方token出错：" + e.getMessage());
            return JSONObject.toJSONString(resp);
        }

        return JSONObject.toJSONString(resp);

    }


    /**
     * 后台刷新本地token
     * @param req
     * @return
     */
    @RequestMapping(value = "refreshLocalToken",method = RequestMethod.POST)
    @ResponseBody
    public String refreshLocalToken(@RequestBody JSONObject req){

        JSONObject resp=new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg","OK");

        try {
            resp=mobileInterService.updateLocalTxToken("手动刷新");
            if(!BizConstant.Code.Result_Code_Success_Num_0.equals(resp.getString("code"))){
                throw new Exception(resp.getString("msg"));
            }
        } catch (Exception e) {
            log.error("后台刷新本地token出错："+e.getCause(),e);
            resp.put("code", BizConstant.Code.Result_Code_Systemrror);
            resp.put("msg",e.getMessage());
            return JSONObject.toJSONString(resp);
        }

        return JSONObject.toJSONString(resp);

    }
}

