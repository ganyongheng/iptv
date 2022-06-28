package iptv.modules.music.strategy.pay;

import com.alibaba.fastjson.JSONObject;
import iptv.modules.music.entity.db.ZnyxOrderInfo;
import iptv.modules.music.service.impl.ZnyxOrderInfoServiceImpl;
import iptv.util.BizConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author zx
 * @date 2022年04月14日 11:12
 */
@Component("phonePay")
public class PhonePay implements PayStrategyService {
    @Autowired
    private ZnyxOrderInfoServiceImpl znyxOrderInfoService;
    @Override
    public JSONObject pay(JSONObject req,ZnyxOrderInfo znyxOrderInfo) {
        JSONObject resp = new JSONObject();
        resp.put("code", 0);
        resp.put("msg", "成功");
        //todo 请求业管
        String result = "0000";
        if("0000".equals(result)) {
            znyxOrderInfo.setStatus(BizConstant.Code.ZnyxOrder.Order_Pay_Status_Paied);
            znyxOrderInfo.setPayTime(new Date());
            znyxOrderInfoService.updateById(znyxOrderInfo);
        }

        return resp;
    }
}
