package com.harmonycloud.zeus.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.zeus.bean.BeanMailToUser;
import com.harmonycloud.zeus.bean.user.BeanUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author yushuaikang
 * @date 2021/11/11 下午3:58
 */
@Repository
public interface BeanMailToUserMapper extends BaseMapper<BeanMailToUser> {

}
