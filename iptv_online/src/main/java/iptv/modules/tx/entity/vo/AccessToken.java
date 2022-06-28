package iptv.modules.tx.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务SyncRightsTimerTask获取腾讯token封装实体类
 */
@Data
public class AccessToken  implements Serializable {

    private static String accesstoken;
    private static Date expiretime;
    private static Date updateTime;
    private static String localIp;

    public static String getAccesstoken() {
        return accesstoken;
    }
    public static void setAccesstoken(String accesstoken) {
        AccessToken.accesstoken = accesstoken;
    }
    public static Date getExpiretime() {
        return expiretime;
    }
    public static void setExpiretime(Date expiretime) {
        AccessToken.expiretime = expiretime;
    }

    public static Date getUpdateTime() {
        return updateTime;
    }

    public static void setUpdateTime(Date updateTime) {
        AccessToken.updateTime = updateTime;
    }

    public static String getLocalIp() {
        return localIp;
    }

    public static void setLocalIp(String localIp) {
        AccessToken.localIp = localIp;
    }
}
