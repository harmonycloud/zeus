package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/12/14 10:05 上午
 */
public interface ClusterRoleService {

    /**
     * 添加角色集群权限
     * @param roleId 角色id
     * @param clusterList 集群列表
     *
     */
    void add(Integer roleId, List<MiddlewareClusterDTO> clusterList);

    /**
     * 更新角色集群权限
     * @param roleId 角色id
     * @param clusterList 集群列表
     *
     */
    void update(Integer roleId, List<MiddlewareClusterDTO> clusterList);

    /**
     * 删除角色集群权限
     * @param roleId 角色id
     *
     */
    void delete(Integer roleId);

    /**
     * 获取角色集群权限
     * @param roleId 角色id
     *
     */
    List<MiddlewareClusterDTO> get(Integer roleId);

}
