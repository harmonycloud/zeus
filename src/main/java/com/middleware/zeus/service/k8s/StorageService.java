package com.middleware.zeus.service.k8s;

import com.middleware.zeus.common.model.QuotaBase;
import com.middleware.zeus.common.model.StorageDto;
import com.middleware.zeus.common.model.middleware.MiddlewareStorageInfoDto;
import com.middleware.zeus.common.model.middleware.StorageClassInfo;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/6/8 10:29 上午
 */
public interface StorageService {

    /**
     * 获取存储类型
     */
    List<String> getType();

    /**
     * 查询存储列表
     *
     * @param clusterId 集群id
     * @param name 存储名称
     * @return List<StorageDto>
     */
    StorageDto get(String clusterId, String name);

    /**
     * 查询存储列表
     *
     * @param clusterId 集群id
     * @param all       是否全部
     * @return List<StorageDto>
     */
    List<StorageDto> list(String clusterId, Boolean all);

    /**
     * 查询存储列表
     *
     * @param clusterId 集群id
     * @param storageId 存储id
     * @return List<StorageDto>
     */
    StorageDto getById(String clusterId, String storageId);

    /**
     * 查询存储列表
     *
     * @param clusterId 集群id
     * @param key       关键词检索
     * @param type      存储类型
     * @param all       是否全部(true会过滤掉已添加的存储)
     * @return List<StorageDto>
     */
    List<StorageDto> list(String clusterId, String key, String type, Boolean all);

    /**
     * 添加或更新存储
     *
     * @param storageDto 存储业务对象
     */
    void addOrUpdate(StorageDto storageDto);

    /**
     * 删除存储
     *
     * @param clusterId   集群id
     * @param storageId 存储id
     */
    void delete(String clusterId, String storageId);

    /**
     * 查询中间件存储使用情况
     *
     * @param clusterId   集群id
     * @param storageName 存储名称
     * @return List<MiddlewareResourceInfo>
     */
    List<MiddlewareStorageInfoDto> middlewares(String clusterId, String storageName);

    /**
     *
     * @param clusterId 集群id
     * @return
     */
    Map<String, Map<String, QuotaBase>> monitorStorageQuota(String clusterId);

    Map<String,String> listStorageMap(String clusterId, Boolean all);

    /**
     *
     * @param clusterId 集群id
     * @param storageName 存储名
     * @return
     */
    String getAliasName(String clusterId, String storageName);

    /**
     * 校验是否为hitachi存储并获取参数
     *
     * @param clusterId 集群id
     * @param storageName 存储名
     * @return Map<String, String>
     */
    Map<String, String> checkHitachiAndGetParams(String clusterId, String storageName);

    /**
     * 查询storageClassInfo
     *
     * @param clusterId 集群id
     * @param all 是否所有
     * @return List<StorageClassInfo>
     */
    List<StorageClassInfo> listStorageClassInfo(String clusterId, Boolean all);
}
