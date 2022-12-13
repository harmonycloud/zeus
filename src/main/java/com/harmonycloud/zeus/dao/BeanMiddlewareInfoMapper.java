package com.harmonycloud.zeus.dao;

import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import feign.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 * 中间件表 Mapper 接口
 * </p>
 *
 * @author skyview
 * @since 2021-03-23
 */
@Repository
public interface BeanMiddlewareInfoMapper extends BaseMapper<BeanMiddlewareInfo> {
    List<BeanMiddlewareInfo> listInstalledWithMiddlewareDetail(@Param("clusters") List<MiddlewareClusterDTO> clusters);
}
