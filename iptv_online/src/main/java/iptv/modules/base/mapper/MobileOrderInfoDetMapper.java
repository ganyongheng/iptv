package iptv.modules.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import iptv.modules.base.entity.db.MobileOrderInfoDet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MobileOrderInfoDetMapper extends BaseMapper<MobileOrderInfoDet> {

    /**
     * 获取需要发货的子订单数据
     * @param current_time
     * @param nums
     * @return
     */
    @Select({
            "<script>",
               " SELECT * FROM mobile_order_info_det WHERE status = #{status} ",
               " AND DATE_FORMAT(date_add(feetime, interval -#{nums} day), '%Y-%m-%d') &lt; #{current_time} ",
               " AND ignore_flag is null ",
               " AND source not in (select source from mobile_autopay_config where status = 2) ",
            "</script>"
    })
    List<MobileOrderInfoDet> getDeliverGoodsList(@Param(value="current_time")String current_time, @Param(value="nums")int nums,
                                                 @Param(value="status")String status);


    /**
     * 当前一笔子订单完成的前提下，确认父订单的发货状态。
     * @param ext_traceno
     * @param traceno
     * @param source
     * @return
     */
    @Select({
            "<script>",
               " SELECT * FROM mobile_order_info_det WHERE ",
               " ext_traceno = #{ext_traceno} AND  traceno != #{traceno} AND source= #{source}",
            "</script>"
    })
    List<MobileOrderInfoDet> getMobileOrderInfoStatus(@Param(value="ext_traceno")String ext_traceno, @Param(value="traceno")String traceno,
                                                      @Param(value="source") String source);
}