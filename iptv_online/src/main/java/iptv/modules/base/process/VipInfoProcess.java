package iptv.modules.base.process;

import com.alibaba.fastjson.JSONObject;
import iptv.common.CheckUtils;
import iptv.util.BizConstant;
import iptv.util.YouKuRequstUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wyq
 * @create 2022/3/17 15:35
 */
public abstract class VipInfoProcess {

    protected Logger log = LoggerFactory.getLogger(VipInfoProcess.class);
    protected Logger logger = LoggerFactory.getLogger("VipInfoProcess");


    /**
     * 校验参数
     *
     * @param req
     */
    public void checkBaseReparam(JSONObject req) throws Exception {
        CheckUtils.checkEmpty(req.getString("vuid"), "请求失败：缺少请求参数-用户vuid【vuid】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("source"), "请求失败：缺少请求参数-渠道来源【source】",
                BizConstant.Code.Missing_Parameter);
        CheckUtils.checkEmpty(req.getString("userId"), "请求失败：缺少请求参数-用户编号【userId】",
                BizConstant.Code.Missing_Parameter);
    }

    /**
     * 查询权益时间
     *
     * @param req
     * @return
     */
    public abstract JSONObject queryTime(JSONObject req) throws Exception;

}
