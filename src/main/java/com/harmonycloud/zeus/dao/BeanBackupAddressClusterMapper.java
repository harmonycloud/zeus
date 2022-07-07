package com.harmonycloud.zeus.dao;

import com.harmonycloud.zeus.bean.BeanMiddlewareBackupToCluster;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 * @author yushuaikang
 * @Date 2022/6/15 17:55 下午
 */
@Mapper
@Repository
public interface BeanBackupAddressClusterMapper extends BaseMapper<BeanMiddlewareBackupToCluster> {
}
