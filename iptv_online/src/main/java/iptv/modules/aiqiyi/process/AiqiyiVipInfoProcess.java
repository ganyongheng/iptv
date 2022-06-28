package iptv.modules.aiqiyi.process;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import iptv.common.BusinessException;
import iptv.modules.base.factory.vipinfo.VipInfoProcessFactory;
import iptv.modules.base.process.VipInfoProcess;
import iptv.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.TreeMap;
import java.util.UUID;

/**
 * @author wyq
 * @create 2022/3/17 16:43
 */

@Component
public class AiqiyiVipInfoProcess extends VipInfoProcess implements InitializingBean {

    @Autowired
    private SysConfig sysConfig;

    /**
     * 查询权益时间
     *
     * @param req
     * @return
     */
    @Override
    public JSONObject queryTime(JSONObject req) throws Exception {
        JSONObject resp = new JSONObject();
        resp.put("code", BizConstant.Code.Result_Code_Success_Num_0);
        resp.put("msg", "OK");
//组装参数请求爱奇艺
        TreeMap<String, Object> aiqiyi_req = new TreeMap<String, Object>();
        String reqId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        aiqiyi_req.put("version",sysConfig.getAIQIYI_GET_VIP_INFO_VERSION());
        aiqiyi_req.put("reqId", reqId);
        aiqiyi_req.put("partner_no",sysConfig.getAIQIYI_PARTNER_NO());
        aiqiyi_req.put("open_id", req.get("vuid"));

        String sign = AiqiyiAuthenticationUtil.authentication(aiqiyi_req);
        sign = StringUtils.lowerCase(sign);
        aiqiyi_req.put("sign", sign);

        String url = sysConfig.getAIQIYI_VIP_INFO_URL();
        String result;
        JSONObject aiqiyi_req_json = new JSONObject(aiqiyi_req);
        try {
            result = HttpUtils.doPost(url, aiqiyi_req_json);
        } catch (Exception e) {
            log.error("请求爱奇艺获取会员信息出错!" + e.getCause(), e);
            throw new BusinessException("请求爱奇艺获取会员信息出错!");
        }

        if (StringUtils.isBlank(result)) {
            log.error("请求爱奇艺获取会员信息接口返回为空");
            throw new BusinessException("请求爱奇艺获取会员信息接口返回为空!");
        }

        JSONObject resultJson = JSONObject.parseObject(result);

        String resultcode = resultJson.getString("code");
        if (BizConstant.Aiqiyi.SuccessCode.equals(resultcode)) {
            //获取会员信息成功
            JSONArray data = resultJson.getJSONArray("data");
            String endStr = "";
            String startStr = "";
            if (!data.isEmpty() || data.size() > 0) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject job = data.getJSONObject(i);
                    endStr = job.getString("endTime");
                    startStr = job.getString("startTime");
                }
            }
            Date endDate = DateUtil.StringToDate(endStr,"yyyy-MM-dd HH:mm:ss");
            Date startDate = DateUtil.StringToDate(startStr, "yyyy-MM-dd HH:mm:ss");
            long endDateTime = endDate.getTime();
            long startDateTime = startDate.getTime();
            long now = System.currentTimeMillis();
            if (startDateTime < now && now < endDateTime) {
                //是vip
                resp.put("is_vip",1);
                resp.put("start", startDateTime/1000);
                resp.put("end", endDateTime/1000);
            } else {
                //不是vip
                resp.put("is_vip", 0);
            }
            resp.put("vuid", req.getString("vuid"));

        } else if (BizConstant.Aiqiyi.NoRightsCode.equals(resultcode)) {
            resp.put("vuid", req.getString("vuid"));
            resp.put("is_vip", 0);
        } else if (BizConstant.Aiqiyi.SignatureErrorCode.equals(resultcode)) {
            log.error("请求爱奇艺获取会员信息签名错误");
            throw new BusinessException("请求爱奇艺获取会员信息签名错误");
        } else {
            //获取会员信息失败
            log.error("请求爱奇艺获取会员信息接口返回失败");
            throw new BusinessException("请求爱奇艺获取会员信息接口返回失败");
        }
        return resp;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        VipInfoProcessFactory.create("aiqiyi", this);
    }
}
