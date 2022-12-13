package com.middleware.zeus.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.middleware.zeus.bean.BeanMiddlewareBackupName;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @author yushuaikang
 * @date 2022/6/25 11:15 AM
 */

@Mapper
@Repository
public interface BeanMiddlewareBackupNameMapper extends BaseMapper<BeanMiddlewareBackupName> {
}
