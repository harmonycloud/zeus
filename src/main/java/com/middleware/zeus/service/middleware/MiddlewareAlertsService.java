package com.middleware.zeus.service.middleware;

import java.util.List;

import com.github.pagehelper.PageInfo;
import com.middleware.caas.common.model.AlertSettingDTO;
import com.middleware.caas.common.model.AlertUserDto;
import com.middleware.caas.common.model.AlertUserListDto;
import com.middleware.caas.common.model.MiddlewareAlertsListDto;
import com.middleware.caas.common.model.middleware.MiddlewareAlertsDTO;
import com.middleware.caas.common.model.registry.HelmChartFile;

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
     * @return List<BeanPrometheusRules>
     */
    void createRules(String clusterId, String namespace, String middlewareName,
                     MiddlewareAlertsListDto middlewareAlertsListDto) throws Exception;

    /**
     * 删除告警规则
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
     * @param middlewareAlertsDTO 中间件告警规则
     * @return List<BeanPrometheusRules>
     */
    void updateRules(String clusterId, String namespace, String middlewareName,
                     String ding, String alertRuleId,
                     MiddlewareAlertsDTO middlewareAlertsDTO) throws Exception;

    /**
     * 删除系统告警规则
     *
     * @param clusterId 集群id
     * @param alert 规则名称
     */
    void deleteSystemRules(String clusterId, String alert, String alertRuleId);

    /**
     * 获取告警规则详情
     * @param alertRuleId
     */
    MiddlewareAlertsDTO alertRuleDetail(String alertRuleId);

    /**
     * 查询服务告警设置
     * @param args 可变数组，长度为0时查询系统告警设置，长度为3时查询服务告警设置(参数依次为clusterId,namespace,middlewareName)
     * @return
     */
    AlertSettingDTO queryAlertSetting(String... args);

    /**
     * 查询告警用户
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param allocatable 可分配的
     * @param organId 组织id
     * @param projectId 项目id
     * @return List<AlertUserDTO>
     */
    List<AlertUserDto> alertUser(String clusterId, String namespace, String middlewareName, Boolean allocatable, String organId, String projectId);

    /**
     * 添加告警用户
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param alertUserListDto 告警用户列表
     */
    void addAlertUser(String clusterId, String namespace, String middlewareName, AlertUserListDto alertUserListDto);

    /**
     * 移除告警用户
     *
     * @param username 用户名
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     */
    void removeAlertUser(String clusterId, String namespace, String middlewareName, String username);

    /**
     * 获取备份告警开关状态
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     */
    Boolean getBackupAlert(String clusterId, String namespace, String middlewareName);

    /**
     * 修改备份告警开关状态
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param enable 开启/关闭
     */
    void editBackupAlert(String clusterId, String namespace, String middlewareName, Boolean enable);

}
