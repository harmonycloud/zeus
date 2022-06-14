package com.harmonycloud.zeus.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupAddress;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author yushuaikang
 * @Date 2022/6/9 17:55 下午
 */
@Mapper
@Repository
public interface BeanMiddlewareBackupAddressMapper extends BaseMapper<BeanMiddlewareBackupAddress> {

    Integer insertBatchSomeColumn(List<BeanMiddlewareBackupAddress> entityList);
}
