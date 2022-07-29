package com.harmonycloud.zeus.dao.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.zeus.bean.BeanMailToUser;
import com.harmonycloud.zeus.bean.user.BeanUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/7/27 10:14 上午
 */
@Repository
public interface BeanUserMapper extends BaseMapper<BeanUser> {

    List<UserDto> selectUserList(@Param("userIds") List<BeanMailToUser> userIds);

}
