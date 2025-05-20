package com.middleware.zeus.service.middleware;

import java.util.List;

import com.middleware.zeus.common.model.middleware.MiddlewareLogAlertDto;

/**
 * @author xutianhong
 * @Date 2025/5/14 下午5:22
 */
public interface MiddlewareLogAlertsService {

    /**
     * 查询日志告警规则
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param middlewareName 中间件名称
     * @param type 中间件类型
     * @return List<MiddlewareLogAlertDto>
     */
    List<MiddlewareLogAlertDto> listRules(String clusterId, String namespace, String middlewareName, String type);

    /**
     * 创建日志告警规则
     * @param middlewareAlertsDTO 告警规则对象
     */
    void createRules(MiddlewareLogAlertDto middlewareLogAlertDto);

    /**
     * 更新日志告警规则
     * @param middlewareAlertsDTO 告警规则对象
     */
    void updateRules(MiddlewareLogAlertDto middlewareLogAlertDto);

    /**
     * 删除日志告警规则
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param middlewareName 中间件名称
     * @param type 中间件类型
     * @param alertName 告警名称
     */
    void deleteRules(String clusterId, String namespace, String middlewareName, String type, String alertName);

}
