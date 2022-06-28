package iptv.modules.tx.factory.getvuid;

import iptv.modules.tx.process.GetAccessTokenProcess;
import iptv.modules.tx.process.GetVuidProcess;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Author wyy
 * Date 2022/3/17 14:40
 **/

@Component
public class GetVuidProcessFactory {

    private static Map<String, GetVuidProcess> map = new HashMap<>();

    public GetVuidProcess creatBaseAction(String source) {
        GetVuidProcess process = map.get(source);
        if (process == null) {
            process = map.get("tencent");
        }
        return process;
    }

    public static void create(String defaultProcess, GetVuidProcess getVuidProcess) {
        map.put(defaultProcess, getVuidProcess);
    }
}
