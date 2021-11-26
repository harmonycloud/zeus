package com.harmonycloud.zeus.service.middleware;

import java.util.List;

import com.harmonycloud.caas.common.model.middleware.MiddlewareAlertsDTO;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;

/**
 * @author xutianhong
 * @Date 2021/4/26 10:23 上午
 */
public interface MiddlewareAlertsService {

    /**
     * 获取已添加告警规则列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param keyword 关键字
     * @return List<MiddlewareAlertsDTO>
     */
    List<MiddlewareAlertsDTO> listUsedRules(String clusterId, String namespace, String middlewareName,String lay, String keyword) throws Exception;

    /**
     * 获取可添加告警规则列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param type
     * @return List<MiddlewareAlertsDTO>
     */
    List<MiddlewareAlertsDTO> listRules(String clusterId, String namespace, String middlewareName, String type) throws Exception;

    /**
     * 创建告警规则
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param middlewareAlertsDTOList 中间件告警规则
     * @return List<BeanPrometheusRules>
     */
    void createRules(String clusterId, String namespace, String middlewareName, List<MiddlewareAlertsDTO> middlewareAlertsDTOList) throws Exception;

    /**
     * 创建告警规则
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param alertId         告警名称
     * @return List<BeanPrometheusRules>
     */
    void deleteRules(String clusterId, String namespace, String middlewareName, String alertId);

    /**
     * 同步告警规则进数据库
     *
     * @param helmChart helm包
     * @return String
     */
    String updateAlerts2Mysql(HelmChartFile helmChart);

    /**
     * 同步告警规则进数据库
     *
     * @param helmChart helm包
     * @param update 是否更新
     * @return String
     */
    String updateAlerts2Mysql(HelmChartFile helmChart, Boolean update);

    /**
     * 修改告警规则
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param middlewareAlertsDTO 中间件告警规则
     * @return List<BeanPrometheusRules>
     */
    void updateRules(String clusterId, String namespace, String middlewareName, MiddlewareAlertsDTO middlewareAlertsDTO) throws Exception;

}
