package com.middleware.zeus.bean;

import com.github.pagehelper.PageInfo;
import com.middleware.caas.common.model.AlertSummaryDTO;
import com.middleware.caas.common.model.middleware.ClusterQuotaDTO;
import com.middleware.caas.common.model.middleware.MiddlewareBriefInfoDTO;
import com.middleware.caas.common.model.middleware.MiddlewareOperatorDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author liyinlong
 * @since 2021/9/24 3:42 下午
 */
@Data
@NoArgsConstructor
public class PlatformOverviewDTO {

    /**
     * 平台版本
     */
    private String zeusVersion;

    /**
     * 集群信息
     */
    private ClusterQuotaDTO clusterQuota;

    /**
     * 集群中间件实例数量列表
     */
    private List<MiddlewareBriefInfoDTO> briefInfoList;

    /**
     * 控制器信息
     */
    private MiddlewareOperatorDTO operatorDTO;

    /**
     * 告警信息
     */
    private AlertSummaryDTO alertSummary;

    /**
     * 审计列表
     */
    private List<BeanOperationAudit> auditList;

    /**
     * 告警信息
     */
    private PageInfo alertPageInfo;

}
