package iptv.modules.base.factory.confirmordersingle;

import iptv.modules.base.process.ConfirmOrderSingleProcess;
import iptv.modules.base.process.VipInfoProcess;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wyq
 * @create 2022/3/18 11:48
 */

@Component
public class ConfirmOrderSingleProcessFactory {
    private static Map<String, ConfirmOrderSingleProcess> map = new HashMap<>();

    public ConfirmOrderSingleProcess creatBaseAction(String source) {
        ConfirmOrderSingleProcess process = map.get(source);
        if (process == null) {
            process = map.get("confirm_order_single_default");
        }
        return process;
    }

    public static void create(String defaultProcess, ConfirmOrderSingleProcess confirmOrderSingleProcess) {
        map.put(defaultProcess, confirmOrderSingleProcess);
    }
}
