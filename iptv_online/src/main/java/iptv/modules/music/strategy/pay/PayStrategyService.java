package iptv.modules.music.strategy.pay;

import com.alibaba.fastjson.JSONObject;
import iptv.modules.music.entity.db.ZnyxOrderInfo;

public interface PayStrategyService {

    //下单支付
    JSONObject pay(JSONObject req,ZnyxOrderInfo znyxOrderInfo);
}
