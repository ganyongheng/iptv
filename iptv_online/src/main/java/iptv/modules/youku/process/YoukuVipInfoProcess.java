package iptv.modules.youku.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import iptv.modules.base.factory.vipinfo.VipInfoProcessFactory;
import iptv.modules.base.process.VipInfoProcess;
import iptv.util.BizConstant;
import iptv.util.ServerResponse;
import iptv.util.YouKuRequstUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wyq
 * @create 2022/3/17 17:31
 */
@Component
public class YoukuVipInfoProcess extends VipInfoProcess implements InitializingBean {

    @Autowired
    private YouKuRequstUtils youKuRequstUtils;


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
        Object vuid = req.get("vuid");
        String source = req.get("source") + "";
        Map<String, Object> requestParams = new HashMap<String, Object>();
        String channelIdBySource = youKuRequstUtils.getChannelIdBySource(source);
        requestParams.put("thirdPartId", vuid);
        requestParams.put("pid", channelIdBySource);
        ServerResponse doRequest = youKuRequstUtils.doRequest("ott.operator.order.query", requestParams);
        String bizParam = JSON.toJSONString(doRequest);
        log.info("请求优酷ott.operator.order.query----返回--" + bizParam);
        int status = doRequest.getStatus();
        if (0 == status) {
            Object data_obj = doRequest.getData();
            JSONObject data = (JSONObject) JSON.parse(data_obj.toString());
            Object code = data.get("code");
            if ("10000".equals(code)) {
                JSONObject bizResp = (JSONObject) data.get("bizResp");
                JSONObject model = (JSONObject) bizResp.get("model");
                Object object_currentRight = model.get("currentRight");
                if (object_currentRight != null) {
                    JSONObject currentRight = (JSONObject) object_currentRight;
                    Long startTime = Long.valueOf(currentRight.get("startTime").toString());
                    Long endTime = Long.valueOf(currentRight.get("endTime").toString());
                    String vip = currentRight.get("vip") + "";
                    if ("true".equals(vip)) {
                        resp.put("is_vip", 1);
                    } else {
                        resp.put("is_vip", 0);
                    }
                    resp.put("start", startTime / 1000);
                    resp.put("end", endTime / 1000);
                } else {
						/*resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
						resp.put("msg", "请求优酷会员信息返回为空!");*/
                    resp.put("is_vip", 0);
                }
            } else {
                resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
                resp.put("msg", "请求优酷会员信息返回为空!");
            }
        } else {
            log.error("【YoukuVipInfoProcess】vuid:" + req.getString("vuid") + "请求优酷会员信息返回错误-----" + bizParam);
            resp.put("code", BizConstant.Code.Result_Code_Fail_Num_1);
            resp.put("msg", "请求优酷会员信息返回为空!");
        }
        return resp;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        VipInfoProcessFactory.create("youku", this);
    }
}
