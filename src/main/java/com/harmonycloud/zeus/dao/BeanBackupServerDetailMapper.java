package com.harmonycloud.zeus.dao;

import com.harmonycloud.zeus.bean.BeanBackupServerDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 备份服务器详情 Mapper 接口
 * </p>
 *
 * @author zeus
 * @since 2023-01-11
 */
@Repository
@Mapper
public interface BeanBackupServerDetailMapper extends BaseMapper<BeanBackupServerDetail> {

}
