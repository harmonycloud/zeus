package com.middleware.zeus.service.middleware;

import java.util.List;

import com.middleware.caas.common.model.middleware.CustomConfig;
import com.middleware.caas.common.model.middleware.CustomConfigHistoryDTO;
import com.middleware.caas.common.model.middleware.MiddlewareCustomConfig;
import com.middleware.caas.common.model.registry.HelmChartFile;
import com.middleware.zeus.bean.BeanCustomConfig;

/**
 * @author xutianhong
 * @Date 2021/4/23 4:32 下午
 */
public interface MiddlewareCustomConfigService  {

    /**
     * 获取自定义配置
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param type 中间件类型
     * @param order 排序: ascend|descend
     * @return List<CustomConfig>
     */
    List<CustomConfig> listCustomConfig(String clusterId, String namespace, String middlewareName, String type, String order) throws Exception;

    /**
     * 更新自定义配置
     *
     * @param middlewareCustomConfig 自定义配置
     */
    void updateCustomConfig(MiddlewareCustomConfig middlewareCustomConfig);

    /**
     * 获取自定义配置修改记录
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param type 中间件类型
     * @return List<CustomConfigHistoryDTO>
     */
    List<CustomConfigHistoryDTO> getCustomConfigHistory(String clusterId, String namespace, String middlewareName,
                                                        String type, String item, String startTime, String endTime);

    /**
     * 上传helm包时，同步更新config
     *
     * @param helmChartFile helm包内容
     * @return List<BeanCustomConfig>
     */
    List<BeanCustomConfig> updateConfig2MySQL(HelmChartFile helmChartFile) throws Exception;

    /**
     * 上传helm包时，同步更新config
     *
     * @param helmChartFile helm包内容
     * @param update 是否为更新
     * @return List<BeanCustomConfig>
     */
    List<BeanCustomConfig> updateConfig2MySQL(HelmChartFile helmChartFile, Boolean update) throws Exception;

    /**
     * 删除中间件时，删除修改记录
     *
     * @param clusterId 集群
     * @param namespace 分区
     * @param name  名称
     */
    void deleteHistory(String clusterId, String namespace, String name);

    /**
     * 置顶参数
     *
     * @param clusterId 集群
     * @param namespace 分区
     * @param name  名称
     * @param configName 参数名称
     * @param type 中间件类型
     */
    void topping(String clusterId, String namespace, String name, String configName, String type);
}
