package com.harmonycloud.zeus.service.middleware;

import java.util.List;

import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.model.AlertUserDTO;
import com.harmonycloud.caas.common.model.AlertsUserDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareAlertsDTO;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.user.BeanUser;

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
    PageInfo<MiddlewareAlertsDTO> listUsedRules(String clusterId, String namespace, String middlewareName, String lay, String keyword) throws Exception;

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
     * @param alertsUserDTO 中间件告警规则和用户
     * @return List<BeanPrometheusRules>
     */
    void createRules(String clusterId, String namespace,
                     String middlewareName, String ding,
                     AlertsUserDTO alertsUserDTO) throws Exception;

    /**
     * 创建告警规则
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param alert          告警名称
     * @return List<BeanPrometheusRules>
     */
    void deleteRules(String clusterId, String namespace, String middlewareName, String alert, String alertRuleId);

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
     * @param alertUserDTO 中间件告警规则
     * @return List<BeanPrometheusRules>
     */
    void updateRules(String clusterId, String namespace, String middlewareName,
                     String ding, String alertRuleId,
                     AlertUserDTO alertUserDTO) throws Exception;

    /**
     * 创建系统告警规则
     *
     * @param clusterId 集群id
     * @param alertsUserDTO 中间件告警规则和用户
     */
    void createSystemRule(String clusterId, String ding,
                          AlertsUserDTO alertsUserDTO);

    /**
     * 删除系统告警规则
     *
     * @param clusterId 集群id
     * @param alert 规则名称
     */
    void deleteSystemRules(String clusterId, String alert, String alertRuleId);

    /**
     * 修改系统告警规则
     *
     * @param clusterId 集群id
     * @param alertUserDTO 中间件告警规则
     */
    void updateSystemRules(String clusterId, String ding,
                           String alertRuleId,AlertUserDTO alertUserDTO);

}
