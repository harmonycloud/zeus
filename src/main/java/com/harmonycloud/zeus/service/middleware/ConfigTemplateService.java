package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.middleware.CustomConfigTemplateDTO;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/25 10:36 上午
 */
public interface ConfigTemplateService {

    /**
     * 获取模板列表
     *
     * @param type      集群id
     * @return List<CustomConfigTemplateDTO>
     */
    List<CustomConfigTemplateDTO> list(String type);

    /**
     * 获取配置模板
     *
     * @param type      集群id
     * @param name      命名空间
     * @return CustomConfigTemplateDTO
     */
    CustomConfigTemplateDTO get(String type, String name);

}
