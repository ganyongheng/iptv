package iptv.modules.youku.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import iptv.modules.youku.entity.db.YoukuOrderInfo;
import iptv.modules.youku.mapper.MobileUserProductYoukuMapper;
import iptv.modules.youku.mapper.YoukuOrderInfoMapper;
import iptv.util.ServerResponse;
import iptv.util.YouKuRequstUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;


@Component
public class MobileDownOrderInfoServiceImpl{

    protected Logger logger = LoggerFactory.getLogger(MobileDownOrderInfoServiceImpl.class);

    @Autowired
    private YouKuRequstUtils youKuRequstUtils;

    @Autowired
    private MobileUserProductYoukuMapper mobileUserProductYoukuMapper;

    @Autowired
    private YoukuOrderInfoMapper youkuOrderInfoMapper;

    public void operate() {
        List<Map> cmaplist_all=new ArrayList<Map>();
        // 第一步获取用户
        getDbDate(cmaplist_all);
        // 第二步查询用户订单，并入表
        if(cmaplist_all.size()>0){
            getHttpOrderinfo(cmaplist_all);
        }
    }

    public void getDbDate(List<Map> cmaplist_all){
        BigInteger cindex = new BigInteger("0");
        Integer oneLength = 200000;
        while (true) {
            try {
                List<Map> cmaplist = mobileUserProductYoukuMapper.selectUserByIdRange(cindex.longValue(),oneLength);
                logger.info( "DowmYoukuOrderTask定时任务查询id数位："+ cindex.longValue() + "一次查询总数" + oneLength);
                if (cmaplist.size() > 0) {
                    if (cmaplist.get(cmaplist.size() - 1).get("id") instanceof Integer) {
                        Integer cid = (Integer) cmaplist.get(cmaplist.size() - 1).get("id");
                        cindex = new BigInteger(cid.toString());
                    } else {
                        BigInteger cid = (BigInteger) cmaplist.get(cmaplist.size() - 1).get("id");
                        cindex = cid;
                    }
                    cmaplist_all.addAll(cmaplist);
                }
                if (cmaplist.size() < oneLength) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
    }

    private void getHttpOrderinfo(List<Map> cmaplist_all) {
        for (Map map : cmaplist_all) {
            try {
                Object vuid = map.get("vuid");
                String source = map.get("source")+"";
                Object user_id = map.get("user_id");
                String channelIdBySource = youKuRequstUtils.getChannelIdBySource(source);
                Map<String, Object> requestParams=new HashMap<String, Object>();
                requestParams.put("thirdPartId", vuid);
                requestParams.put("pid", channelIdBySource);
                ServerResponse doRequest = youKuRequstUtils.doRequest("ott.operator.order.query",requestParams);
                String bizParam = JSON.toJSONString(doRequest);
                logger.info("请求优酷ott.operator.order.query----返回--"+bizParam);
                int status = doRequest.getStatus();
                if(0==status){
                    Object data_obj =  doRequest.getData();
                    JSONObject data = (JSONObject) JSON.parse(data_obj.toString());
                    Object code = data.get("code");
                    if("10000".equals(code)){
                        JSONObject bizResp =  (JSONObject)data.get("bizResp");
                        JSONObject model = (JSONObject) bizResp.get("model");
                        JSONArray orders = (JSONArray) model.get("orders");
                        if(orders!=null&&orders.size()>0){
                            List<YoukuOrderInfo> listYoukuOrderInfo=new ArrayList<>();
                            for (Object entity : orders) {
                                YoukuOrderInfo youkuOrderInfo=new YoukuOrderInfo();
                                JSONObject entity_js =  (JSONObject)entity;
                                Object orderTime = entity_js.get("orderTime");
                                if(orderTime!=null){
                                    Date date = new SimpleDateFormat("yyyyMMddHHmmss").parse(orderTime.toString());
                                    youkuOrderInfo.setOrderConfirmtime(date);
                                }
                                String outOrderId = entity_js.get("outOrderId")+"";
                                String outProductId = entity_js.get("outProductId")+"";
                                String status_obj = entity_js.get("orderStatus")+"";
                                String orderFrom = entity_js.get("orderFrom")+"";
                                youkuOrderInfo.setCreateTime(new Date());
                                youkuOrderInfo.setOrderId(outOrderId);
                                youkuOrderInfo.setStatus(status_obj);
                                youkuOrderInfo.setOrderType(orderFrom);
                                youkuOrderInfo.setUserid(user_id.toString());
                                youkuOrderInfo.setVuid(vuid.toString());
                                youkuOrderInfo.setSource(source.toString());
                                youkuOrderInfo.setThirdVippkg(outProductId);
                                listYoukuOrderInfo.add(youkuOrderInfo);
                            }
                            if(listYoukuOrderInfo.size()>0){
                                //保存数据
                                youkuOrderInfoMapper.insertForBatch(listYoukuOrderInfo);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("请求优酷ott.operator.order.query接口出错-----"+e.getMessage());
            }
        }
    }
}
