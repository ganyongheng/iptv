package iptv.modules.tx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import iptv.modules.tx.entity.db.MobileUserProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface MobileUserProductMapper extends BaseMapper<MobileUserProduct> {

    /**
     * 用户产品权益表更新etime时间
     * @param etime  结束时间
     * @param source 来源
     * @param userid
     * @param product_type 产品类型
     * @param vuid
     * @return
     */
    @Update({
            "<script>",
            "UPDATE mobile_user_product SET etime = #{etime}",
            "WHERE source = #{source} and user_id = #{userid} and product_type = #{product_type} and vuid = #{vuid}",
            "</script>"
    })
    int updateEtimeMobileUserProduct(@Param(value="etime") Date etime, @Param(value="source")String source,
                                     @Param(value="userid")String userid, @Param(value="product_type")String product_type,
                                     @Param(value="vuid")String vuid);


    /**
     * 获取需要出账的数据
     * @param current_time
     * @param nums
     * @param pstatus
     * @param is_autopay
     * @return
     */
    @Select({
            "<script>",
              " SELECT * FROM mobile_user_product WHERE ",
              " is_autopay = #{is_autopay} AND pstatus= #{pstatus}",
              " AND DATE_FORMAT(date_add(etime, interval -#{nums} day), '%Y-%m-%d')=#{current_time} ",
              " AND ignore_flag is null ",
              " AND source not in (select source from mobile_autopay_config where status = 2)",
            "</script>"

    })
    List<MobileUserProduct> getMobileUserProductAccountList(@Param(value="current_time")String current_time, @Param(value="nums")int nums,
                                                            @Param(value="pstatus") String pstatus,@Param(value="is_autopay")String is_autopay);

}