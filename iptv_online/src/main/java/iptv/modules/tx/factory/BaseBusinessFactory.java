package iptv.modules.tx.factory;

import iptv.modules.tx.business.BaseBusiness;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Author wyy
 * Date 2022/3/16 17:08
 **/

@Component
public class BaseBusinessFactory {

    private static Map<String, BaseBusiness> map = new HashMap<>();

    public static void create(String base_business_default, BaseBusiness baseBusiness) {
        map.put(base_business_default, baseBusiness);
    }

    public BaseBusiness creatBaseBusiness(String source) {
        BaseBusiness baseBusiness = map.get(source);
        if (baseBusiness == null) {
            baseBusiness = map.get("tencent");
        }
        return baseBusiness;
    }
}
