package iptv.modules.base.factory.vipinfo;


import iptv.modules.base.process.VipInfoProcess;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class VipInfoProcessFactory {
    private static Map<String, VipInfoProcess> map = new HashMap<>();

    public VipInfoProcess creatBaseAction(String source) {
        VipInfoProcess process = map.get(source);
        if (process == null) {
            process = map.get("vip_info_default");
        }
        return process;
    }

    public static void create(String defaultProcess, VipInfoProcess vipInfoProcessBaseProcess) {
        map.put(defaultProcess, vipInfoProcessBaseProcess);
    }
}
