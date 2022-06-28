package iptv.modules.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import iptv.modules.music.entity.db.ZnyxSyncAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ZnyxSyncAccountMapper extends BaseMapper<ZnyxSyncAccount> {


    /**
     * 根据ext，phone，source修改nums与notify_status
     *
     * @param account
     */
    @Update({
            "<script>",
            "UPDATE znyx_sync_account SET ",
            "nums = #{account.nums},notify_status = #{account.notify_status}, update_time = now() ",
            " WHERE ext_traceno = #{account.ext_traceno} AND phone = #{account.phone} AND ",
            " source = #{account.source}",
            "</script>"
    })
    public void updateByExtAndPhoneAndSource(@Param(value = "account") ZnyxSyncAccount account);


    @Select({
            "<script>",
            " SELECT id,source,vippkg,phone,ext_traceno,ext_traceno_cvod,auto,price,total,refund_total,begin_time,end_time  ",
            " cancel_time,sync_type,pay_type,vip_msg,vip_id,nums,create_time,update_time,notify_status ",
            " FROM znyx_sync_account ",
            " WHERE notify_status = #{notify_status} AND nums <![CDATA[<=]]>  #{failNums} limit #{limits}",
            "</script>"

    })
    public List<ZnyxSyncAccount> getSyncAccountByNotifyStatus(@Param(value = "limits") Integer limits, @Param(value = "notify_status") String notify_status
                                                          ,@Param(value = "failNums")Integer failNums);
}
