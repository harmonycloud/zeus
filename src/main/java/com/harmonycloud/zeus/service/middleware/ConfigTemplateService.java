package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.middleware.CustomConfig;
import com.harmonycloud.caas.common.model.middleware.CustomConfigTemplateDTO;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/25 10:36 上午
 */
public interface ConfigTemplateService {

    /**
     * 创建模板
     *
     * @param customConfigTemplateDTO
     */
    void create(CustomConfigTemplateDTO customConfigTemplateDTO);

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
     * @param uid       模板id
     * @return CustomConfigTemplateDTO
     */
    CustomConfigTemplateDTO get(String type, String uid, String chartVersion);

    /**
     * 获取初始化模板
     *
     * @param type            中间件类型
     * @param chartVersion    中间件版本
     * @return CustomConfigTemplateDTO
     */
    List<CustomConfig> get(String type, String chartVersion);

    /**
     * 更新模板
     *
     * @param customConfigTemplateDTO
     */
    void update(CustomConfigTemplateDTO customConfigTemplateDTO);

    /**
     * 删除模板
     *
     * @param uid 模板id
     */
    void delete(String type, String uids);

}
