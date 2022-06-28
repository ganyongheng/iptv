package iptv.modules.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import iptv.modules.base.entity.db.IptvProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IptvProductMapper extends BaseMapper<IptvProduct> {

}