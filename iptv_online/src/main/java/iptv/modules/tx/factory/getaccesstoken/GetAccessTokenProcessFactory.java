package iptv.modules.tx.factory.getaccesstoken;

import iptv.modules.tx.process.GetAccessTokenProcess;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Author wyy
 * Date 2022/3/16 17:21
 **/

@Component
public class GetAccessTokenProcessFactory {

    private static Map<String, GetAccessTokenProcess> map = new HashMap<>();

    public GetAccessTokenProcess creatBaseAction(String source) {
        GetAccessTokenProcess process = map.get(source);
        if (process == null) {
            process = map.get("tencent");
        }
        return process;
    }

    public static void create(String defaultProcess, GetAccessTokenProcess getAccessTokenBaseProcess) {
        map.put(defaultProcess, getAccessTokenBaseProcess);
    }
}
