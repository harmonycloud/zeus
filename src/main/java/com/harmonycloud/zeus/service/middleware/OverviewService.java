package com.harmonycloud.zeus.service.middleware;

import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.model.AlertDTO;
import com.harmonycloud.caas.common.model.MiddlewareDTO;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.bean.AlertMessageDTO;
import com.harmonycloud.zeus.bean.BeanOperationAudit;
import com.harmonycloud.zeus.bean.PlatformOverviewDTO;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/3/26 2:35 下午
 */
public interface OverviewService {

    /**
     * 查询中间件状态
     *
     * @param clusterId
     *            集群id
     * @param namespace
     *            命名空间
     * @return List<MiddlewareStatusDto>
     */
    List<MiddlewareStatusDto> getMiddlewareStatus(String clusterId, String namespace);

    /**
     * 查询中间件实时资源使用量
     *
     * @param clusterId
     *            集群id
     * @param namespace
     *            命名空间
     * @param startTime
     *            开始时间
     * @param endTime
     *            结束时间
     * @return List<MiddlewareMonitorDto>
     */
    List<MiddlewareMonitorDto> getMonitorInfo(String clusterId, String namespace, String name,String type, String startTime,
        String endTime) throws Exception;

    /**
     * 获取告警记录
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param current   当前页
     * @param size      size
     * @param level     等级
     *
     * @return List<AlertDTO>
     */
    PageInfo<AlertDTO> getAlertRecord(String clusterId, String namespace, String middlewareName, Integer current, Integer size, String level, String keyword, String lay);

    MiddlewareOverviewDTO getChartPlatformOverview();

    /**
     * 平台总览列表详细
     * @return 列表
     */
    List<MiddlewareDTO> getListPlatformOverview();

    /**
     * 获取指定集群的数据总览
     *
     * @param clusterId
     * @return PlatformOverviewDTO
     */
    PlatformOverviewDTO getClusterPlatformOverview(String clusterId);

    /**
     * 获取集群资源信息（包括集群数、分区数、CPU、内存）
     * @param clusterId
     * @return
     */
    ClusterQuotaDTO resources(String clusterId);

    /**
     * 控制器状态信息
     * @param clusterId
     * @return
     */
    MiddlewareOperatorDTO operatorStatus(String clusterId);

    /**
     * 服务信息（包含全部服务数量和异常服务数量）
     * @param clusterId
     * @return PlatformOverviewDTO
     */
    List<MiddlewareBriefInfoDTO> getClusterMiddlewareInfo(String clusterId);

    /**
     * 告警信息
     * @param clusterId
     * @param current
     * @param size
     * @return
     */
    AlertMessageDTO getAlertInfo(String clusterId, Integer current, Integer size, String level);

    /**
     * 近20条操作审计信息
     * @return
     */
    List<BeanOperationAudit> recentAudit();

    /**
     * 平台版本信息
     * @return
     */
    Map<String,String> version();

}
