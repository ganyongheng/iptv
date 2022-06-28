package iptv.modules.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import iptv.modules.music.entity.db.ZnyxProductConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ZnyxProductConfigMapper extends BaseMapper<ZnyxProductConfig> {


    @Select({
            "<script>",
            " SELECT id,sync_account_url,source,sign_key " ,
            " FROM znyx_product_config ",
            " WHERE source = #{source} ",
            "</script>"
    })
    public ZnyxProductConfig getOneBySource(@Param(value="source")String source);
}
