package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/9/3 4:10 下午
 */
public interface ClusterMiddlewareInfoService {

    /**
     * 查询集群关联中间件列表
     *
     * @param clusterId 集群id
     * @return List<BeanClusterMiddlewareInfo>
     */
    List<BeanClusterMiddlewareInfo> list(String clusterId);

    /**
     * 查询集群关联中间件列表
     *
     * @param chartName 名称
     * @param chartVersion 版本
     * @return List<BeanClusterMiddlewareInfo>
     */
    List<BeanClusterMiddlewareInfo> listByChart(String chartName, String chartVersion);

    /**
     * 查询指定集群关联中间件
     *
     * @param clusterId 集群id
     * @param type 类型
     * @return List<BeanClusterMiddlewareInfo>
     */
    BeanClusterMiddlewareInfo get(String clusterId, String type);

    /**
     * 添加集群中间件关联记录
     *
     * @param beanClusterMiddlewareInfo 对象
     */
    void insert(BeanClusterMiddlewareInfo beanClusterMiddlewareInfo);

    /**
     * 更新集群中间件关联记录
     *
     * @param beanClusterMiddlewareInfo 对象
     */
    void update(BeanClusterMiddlewareInfo beanClusterMiddlewareInfo);

    /**
     * 删除集群中间件关联记录
     *
     * @param clusterId 集群id
     * @param chartName chart名称
     * @param chartVersion chart版本
     */
    void delete(String clusterId, String chartName, String chartVersion);

}
