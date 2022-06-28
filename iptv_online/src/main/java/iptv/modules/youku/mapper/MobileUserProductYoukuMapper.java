package iptv.modules.youku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import iptv.modules.youku.entity.db.MobileUserProductYouku;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface MobileUserProductYoukuMapper extends BaseMapper<MobileUserProductYouku> {

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
                 "UPDATE mobile_user_product_youku SET etime = #{etime}",
                 "WHERE source = #{source} and user_id = #{userid} and product_type = #{product_type} and vuid = #{vuid}",
            "</script>"
    })
    int updateEtimeMobileUserProduct(@Param(value="etime")Date etime,@Param(value="source")String source,
                                     @Param(value="userid")String userid,@Param(value="product_type")String product_type,
                                     @Param(value="vuid")String vuid);

    /**
     * 通过id范围查询id ,vuid ,user_id ,source
     * @param cindex  id大于数
     * @param oneLength 一次查询数量
     * @return
     */
    @Select({
            "<script>",
                    " SELECT id ,vuid ,user_id ,source  FROM   mobile_user_product_youku WHERE " ,
                    " cooperator_code ='youku' and is_autopay =0 and  id > #{cindex} LIMIT #{oneLength} ",
            "</script>"
    })
    List<Map> selectUserByIdRange(@Param(value="cindex")Long cindex, @Param(value="oneLength")Integer oneLength);


}