package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.StorageDto;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.middleware.PodInfo;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/6/8 10:29 上午
 */
public interface StorageService {

    /**
     * 查询存储列表
     *
     * @param clusterId 集群id
     * @param key       关键词检索
     * @param type      存储类型
     * @param all       是否全部
     * @return List<StorageDto>
     */
    List<StorageDto> list(String clusterId, String key, String type, Boolean all);

    /**
     * 添加存储
     *
     * @param storageDto 存储业务对象
     */
    void add(StorageDto storageDto);

    /**
     * 删除存储
     *
     * @param clusterId   集群id
     * @param storageName 存储名称
     */
    void delete(String clusterId, String storageName);

    /**
     * 更新存储信息
     *
     * @param storageDto 存储业务对象
     */
    void update(StorageDto storageDto);

    /**
     * 查询存储详情
     *
     * @param clusterId   集群id
     * @param storageName 存储名称
     * @return StorageDto
     */
    StorageDto detail(String clusterId, String storageName);

    /**
     * 查询中间件存储使用情况
     *
     * @param clusterId   集群id
     * @param storageName 存储名称
     * @return List<MiddlewareResourceInfo>
     */
    List<Middleware> middlewares(String clusterId, String storageName);

    /**
     * 查询中间件存储使用情况
     *
     * @param clusterId   集群id
     * @param storageName 存储名称
     * @param middlewareName 中间件名称
     * @return List<PodInfo>
     */
    List<PodInfo> pods(String clusterId, String storageName, String middlewareName);


}
