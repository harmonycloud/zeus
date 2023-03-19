package com.middleware.zeus.service.user;

import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.zeus.bean.user.BeanPlatformQuota;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/3/7 4:38 下午
 */
public interface PlatformQuotaService {

    /**
     * 分配配额
     * @param type 类型: 组织  项目
     * @param uid 项目/组织id
     */
    void allocate(String type, String uid, ResourceQuotaDo resourceQuotaDo);

    /**
     * 移除配额情况
     * @param type 类型: 组织  项目
     * @param uid 项目/组织id
     */
    void remove(String type, String uid, String name, String... target);

    /**
     * 查询配额情况
     * @param type 类型: 组织  项目
     * @param uid 项目/组织id
     * @return ResourceQuotaDo
     */
    List<ResourceQuotaDo> getQuota(String type, String uid, String... target);

    /**
     * 查询配额情况
     * @param type 类型: 组织  项目
     * @param uidList 项目/组织id
     * @return ResourceQuotaDo
     */
    List<ResourceQuotaDo> getQuota(String type, List<String> uidList, String... target);

    /**
     * 查询配额情况
     * @param type 类型: 组织  项目
     * @param uidList 项目/组织id
     * @param clusterId 集群id
     * @param name 名称
     * @return ResourceQuotaDo
     */
    List<BeanPlatformQuota> findQuota(String type, List<String> uidList, String clusterId, String name, String... target);

    /**
     * 将a2的资源request设置进a1的used
     * @param a1
     * @param a2
     * @return List<ResourceQuotaDo>
     */
    List<ResourceQuotaDo> convertUsedResource(List<ResourceQuotaDo> a1, List<ResourceQuotaDo> a2);

    /**
     * 封装存储信息
     * @param resourceQuotaDoList
     */
    void convertStorageName(List<ResourceQuotaDo> resourceQuotaDoList);

}
