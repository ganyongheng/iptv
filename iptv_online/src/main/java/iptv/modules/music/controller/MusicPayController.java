package iptv.modules.music.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import iptv.common.BusinessException;
import iptv.common.CheckUtils;
import iptv.modules.music.process.MusicPayProcess;
import iptv.modules.tx.controller.PublicController;
import iptv.util.BizConstant;
import iptv.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 智能音响全部接口
 */
@RestController
@RequestMapping(value = "/znyx_shop")
public class MusicPayController {
    private  Logger log = LoggerFactory.getLogger(MusicPayController.class);
    @Autowired
    private MusicPayProcess musicPayProcess;

    @ApiOperation(value = "获取验证接口")
    @PostMapping(value = "/getMsg")
    public String getMsg(@RequestBody JSONObject req) {
        String msg = musicPayProcess.getMsg(req);
        return msg;
    }
    @ApiOperation(value = "校验验证接口")
    @PostMapping(value = "/checkMsg")
    public String checkMsg(@RequestBody JSONObject  req) {
        String s = musicPayProcess.checkMsg(req);
        return s;
    }
    @ApiOperation(value = "同步用户登陆")
    @PostMapping(value = "/loginSuccess")
    public String loginSuccess(@RequestBody JSONObject  req) {
        String s = musicPayProcess.loginSuccess(req);
        return s;
    }
    @ApiOperation(value = "获取产品列表")
    @PostMapping(value = "/getProduct")
    public String getProduct(@RequestBody JSONObject  req) {
        String s = musicPayProcess.getProduct(req);
        return s;
    }

    @ApiOperation(value = "下单支付")
    @PostMapping(value = "/getOrder")
    public String getOrder(@RequestBody JSONObject  req) {
        String s = musicPayProcess.getOrder(req);
        return s;
    }

      /* @ApiOperation(value = "手机获取验证接口", notes = "手机获取验证")
    @ApiImplicitParam(name = "jsonObject", value = "手机号", required = true, dataType = "json")
    @PostMapping(value = "/getMsg")
    public String getMsg(@RequestBody JSONObject jsonObject) {
        System.out.println("This springCloudConsumerService 1:" + jsonObject.getString("phone"));
        String result = jsonObject.getString("phone") + "_consumer_" + System.currentTimeMillis();
        return result;
    }*/

    /**
     * 移动业管平台订购关系同步
     * @param req
     * @return
     */
    @PostMapping(value = "/sync_account")
    public String syncAccount(@RequestBody JSONObject  req){
        String s = musicPayProcess.syncAccount(req);
        return s;
    }

}
