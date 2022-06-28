/**
 * @mbg.generated
 * generator on Thu Mar 17 14:10:12 GMT+08:00 2022
 */
package iptv.modules.base.service.impl;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import iptv.modules.base.entity.db.MobileOrderInfoDet;
import iptv.modules.base.mapper.MobileOrderInfoDetMapper;
import iptv.modules.tx.entity.db.MobileOrderInfo;
import iptv.util.BizConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MobileOrderInfoDetServiceImpl extends ServiceImpl<MobileOrderInfoDetMapper, MobileOrderInfoDet> {

    @Autowired
    private MobileOrderInfoDetMapper mobileOrderInfoDetMapper;

    public MobileOrderInfoDet getMobileOrderInfoDetByTraceno(String traceno, String source) {
        QueryWrapper<MobileOrderInfoDet> wrapper = new QueryWrapper();
        wrapper.eq("traceno", traceno);
        wrapper.eq("source", source);
        return this.baseMapper.selectOne(wrapper);
    }

    /**
     * 获取需要发货的子订单数据
     * @param current_time
     * @param nums
     * @return
     */
    public List<MobileOrderInfoDet> getDeliverGoodsList(String current_time, int nums){
        //时间需要截取
        return mobileOrderInfoDetMapper.getDeliverGoodsList(current_time.substring(0, 10),nums, BizConstant.Tencent.OrderDet_Status_Create_Prepare);
    }


    /**
     * 当前一笔子订单完成的前提下，确认父订单的发货状态。
     * @param ext_traceno
     * @param traceno
     * @param source
     * @return
     */
    public List<MobileOrderInfoDet> getMobileOrderInfoStatus(String ext_traceno, String traceno, String source){
        return mobileOrderInfoDetMapper.getMobileOrderInfoStatus(ext_traceno,traceno,source);
    }



    public MobileOrderInfoDet selectOne(QueryWrapper<MobileOrderInfoDet> queryWrapper){
        MobileOrderInfoDet mobileOrderInfoDet = mobileOrderInfoDetMapper.selectOne(queryWrapper);
        return  mobileOrderInfoDet;
    }

    public int updateByWrapper(MobileOrderInfoDet entity, Wrapper<MobileOrderInfoDet> warp){
        int update = mobileOrderInfoDetMapper.update(entity, warp);
        return update;
    }


    public void ignoreMobileOrderInfoDetsByUserId(String userId, String source) {
        LambdaUpdateWrapper<MobileOrderInfoDet> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MobileOrderInfoDet::getUserid, userId).eq(MobileOrderInfoDet::getSource, source).set(MobileOrderInfoDet::getIgnoreFlag, 1);
        baseMapper.update(null, updateWrapper);

    }



}