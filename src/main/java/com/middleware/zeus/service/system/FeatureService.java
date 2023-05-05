package com.middleware.zeus.service.system;

import com.middleware.caas.common.model.FeatureDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/5/4 7:21 下午
 */
public interface FeatureService {

    /**
     * 查询feature功能列表
     *
     * @return List<FeatureDto>
     */
    List<FeatureDto> list();

}
