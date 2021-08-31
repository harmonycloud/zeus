package com.harmonycloud.zeus.service.middleware;

import java.util.List;

import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.AlertDTO;
import com.harmonycloud.caas.common.model.MiddlewareDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareMonitorDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareOverviewDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareStatusDto;

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
     * 平台总览
     * @return 返回结果
     */
    BaseResult getPlatformOverview();

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
    PageInfo<AlertDTO> getAlertRecord(String clusterId, String namespace, Integer current, Integer size, String level);

    MiddlewareOverviewDTO getChartPlatformOverview();

    /**
     * 平台总览列表详细
     * @return 列表
     */
    List<MiddlewareDTO> getListPlatformOverview();
}
