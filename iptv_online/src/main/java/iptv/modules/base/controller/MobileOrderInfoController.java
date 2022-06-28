package iptv.modules.base.controller;

import com.alibaba.fastjson.JSONObject;
import iptv.modules.base.service.impl.MobileOrderInterService;
import iptv.util.BizConstant;
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
@RequestMapping("/iptv")
public class MobileOrderInfoController {

    private static Logger log = LoggerFactory.getLogger(MobileOrderInfoController.class);

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MobileOrderInterService mobileOrderInterService;

    /**
     * 停止发货
     * @param req
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "stop_ship", method = RequestMethod.POST)
    public String stopShip(@RequestBody JSONObject json) {
        JSONObject resp = new JSONObject();
        // 校验参数
        try {
            String extTraceno = json.getString("ext_traceno");
            String source = json.getString("source");
            if(StringUtils.isBlank(extTraceno) || StringUtils.isBlank(source) ){
                resp.put("code", BizConstant.Code.Result_Order_Fail_999);
                resp.put("msg", "订单号或者渠道值为空!");
                return JSONObject.toJSONString(resp);
            }
            resp = mobileOrderInterService.stopShip(extTraceno, source);
        } catch (Exception e) {
            log.error("*********stopShip*********出错:" + e.getMessage(), e);
            resp.put("code", BizConstant.Code.Result_Order_Fail_999);
            resp.put("msg", "系统异常!");
            return JSONObject.toJSONString(resp);
        }
        return JSONObject.toJSONString(resp);

    }

}
