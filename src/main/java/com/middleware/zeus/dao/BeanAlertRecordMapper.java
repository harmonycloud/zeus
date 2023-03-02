package com.middleware.zeus.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.middleware.zeus.bean.BeanAlertRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/5/8 10:00 上午
 */
@Mapper
@Repository
public interface BeanAlertRecordMapper extends BaseMapper<BeanAlertRecord> {

    List<Map<String, Object>> queryByTimeAndLevel(@Param("beginTime") String beginTime, @Param("endTime") String endTime, @Param("level") String level);
}
