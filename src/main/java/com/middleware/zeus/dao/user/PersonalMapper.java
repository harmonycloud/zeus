package com.middleware.zeus.dao.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.middleware.zeus.bean.PersonalizedConfiguration;
import org.springframework.stereotype.Repository;

/**
 * @author yushuaikang
 * @date 2021/11/3 上午11:11
 */
@Repository
public interface PersonalMapper extends BaseMapper<PersonalizedConfiguration> {
}
