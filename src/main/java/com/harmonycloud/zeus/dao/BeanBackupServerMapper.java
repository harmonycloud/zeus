package com.harmonycloud.zeus.dao;

import com.harmonycloud.zeus.bean.BeanBackupServer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 备份服务器 Mapper 接口
 * </p>
 *
 * @author zeus
 * @since 2023-01-09
 */
@Mapper
@Repository
public interface BeanBackupServerMapper extends BaseMapper<BeanBackupServer> {

}
