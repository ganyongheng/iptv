package iptv.util;

import com.google.common.base.Joiner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.TreeMap;

/**
 * 爱奇艺鉴权
 */
@Component
public class AiqiyiAuthenticationUtil {


    private static String pubkey;

    public static String getPubkey() {
        return pubkey;
    }
    @Value("${sys-config.pubkey}")
    public void setPubkey(String pubkey) {
        AiqiyiAuthenticationUtil.pubkey = pubkey;
    }

    public static String authentication(TreeMap<String, Object> data) {

        String encode = "";
        if (!data.isEmpty()){
            //拼接为字符串
            String targetParam = Joiner.on("&").withKeyValueSeparator("=")
                    .useForNull("").join(data);
            //计算MD5值
            encode = MD5Util.encode(targetParam + pubkey);
        }
        return encode;
    }

}
