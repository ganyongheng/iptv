package iptv.modules.music.strategy.pay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zx
 * @date 2022年04月14日 11:06
 */
@Service
public class PayStrategy {
    @Autowired
    private final Map<String, PayStrategyService> payStrategyMap = new ConcurrentHashMap<>();

    public PayStrategy(Map<String, PayStrategyService> strategyMap){
        this.payStrategyMap.clear();
        strategyMap.forEach((k,v) -> this.payStrategyMap.put(k,v));
    }

    public PayStrategyService getSource(String resource) {
        String type = "";
        if("1".equals(resource)) {
            type = "zfbPay";
        } else if("2".equals(resource)) {
            type = "wxPay";
        } else {
            type = "phonePay";
        }
        return payStrategyMap.get(type);
    }
}
