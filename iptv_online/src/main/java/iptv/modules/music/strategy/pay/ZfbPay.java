package iptv.modules.music.strategy.pay;

import com.alibaba.fastjson.JSONObject;
import iptv.modules.music.entity.db.ZnyxOrderInfo;

/**
 * @author zx
 * @date 2022年04月14日 11:12
 */
public class ZfbPay implements PayStrategyService{
    @Override
    public JSONObject pay(JSONObject req,ZnyxOrderInfo znyxOrderInfo) {
        return null;
    }
}
