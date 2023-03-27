package com.middleware.zeus.skyview.v2.service;

import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.caas.common.model.user.ProjectDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/24 4:07 下午
 */
public interface V2ProjectService {

    /**
     * 查询项目列表
     * @param organId 组织id
     *
     * @return List<ProjectDto>
     */
    List<ProjectDto> list(String organId);

    /**
     * 查询项目列表(包含用户权限过滤)
     * @param organId 组织id
     *
     * @return List<ProjectDto>
     */
    List<ProjectDto> switchTenants(String organId);

    /**
     * 查询项目列表
     * @param organId 组织id
     * @param projectId 项目id
     *
     * @return List<ProjectDto>
     */
    List<Namespace> nsList(String organId, String projectId, Boolean withQuota);

    /**
     * 查询项目详情
     * @param organId 组织id
     * @param projectId 项目id
     *
     * @return List<ProjectDto>
     */
    ProjectDto get(String organId, String projectId, Boolean includeNsCount, Boolean includeQuota);

}
