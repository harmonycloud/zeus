package com.middleware.zeus.dao;

import com.middleware.zeus.bean.BeanBackupPosition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 备份位置表 Mapper 接口
 * </p>
 *
 * @author zeus
 * @since 2023-01-09
 */
@Repository
@Mapper
public interface BeanBackupPositionMapper extends BaseMapper<BeanBackupPosition> {

}
