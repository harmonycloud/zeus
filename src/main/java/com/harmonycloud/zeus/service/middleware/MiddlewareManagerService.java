package com.harmonycloud.zeus.service.middleware;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @Author: zack chen
 * @Date: 2021/5/14 11:02 上午
 */
public interface MiddlewareManagerService {

    /**
     * 中间件上架
     * @param clusterId
     * @param file
     */
    void upload(String clusterId, MultipartFile file);

    /**
     * 中间件operator发布
     * @param clusterId 集群id
     * @param chartName chart名称
     * @param chartVersion chart版本
     * @param type 是否高可用
     */
    void install(String clusterId, String chartName, String chartVersion, String type);

    /**
     * 中间件下架
     * @param clusterId 集群id
     * @param chartName chart名称
     * @param chartVersion chart版本
     */
    void delete(String clusterId, String chartName, String chartVersion);

    /**
     * 中间件operator更新升级
     * @param clusterId 集群id
     * @param chartName chart名称
     * @param chartVersion chart版本
     */
    void update(String clusterId, String chartName, String chartVersion);
}
