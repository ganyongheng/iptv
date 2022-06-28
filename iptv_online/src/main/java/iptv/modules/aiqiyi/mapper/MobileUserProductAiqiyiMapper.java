package iptv.modules.aiqiyi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import iptv.modules.aiqiyi.entity.db.MobileUserProductAiqiyi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Mapper
public interface MobileUserProductAiqiyiMapper extends BaseMapper<MobileUserProductAiqiyi> {

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
            "UPDATE mobile_user_product_aiqiyi SET etime = #{etime}",
            "WHERE source = #{source} and user_id = #{userid} and product_type = #{product_type} and vuid = #{vuid}",
            "</script>"
    })
    int updateEtimeMobileUserProduct(@Param(value="etime") Date etime, @Param(value="source")String source,
                                     @Param(value="userid")String userid, @Param(value="product_type")String product_type,
                                     @Param(value="vuid")String vuid);

}