package iptv.modules.youku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import iptv.modules.youku.entity.db.YoukuOrderInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface YoukuOrderInfoMapper extends BaseMapper<YoukuOrderInfo> {

    /**
     * 批量插入
     * @param infoList
     * @return
     */
    @Insert({
            "<script>",
                     " insert into youku_order_info " ,
                     " (order_confirmtime,create_time,order_id,status,order_type,userid,vuid,source,third_vippkg) VALUES  ",
                     "<foreach item='item' index='index' collection='list'  separator=','>",
                            " (#{item.orderConfirmtime},#{item.createTime},#{item.orderId},#{item.status},#{item.orderType}, " ,
                            "  #{item.userid},#{item.vuid},#{item.source},#{item.thirdVippkg} ) ",
                     "</foreach>",
            "</script>"
    })
    int insertForBatch(@Param(value="list") List<YoukuOrderInfo> infoList);

}