package com.middleware.zeus.service.middleware;

import com.github.pagehelper.PageInfo;
import com.middleware.zeus.common.model.AlertDTO;
import com.middleware.zeus.common.model.AlertRecordQueryDto;

/**
 * @author liyinlong
 * @since 2023/4/6 3:02 下午
 */
public interface MiddlewareAlertRecordService {

    /**
     * 查询告警记录
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param alertRecordQueryDto 告警记录查询条件
     * @return
     */
    PageInfo<AlertDTO> list(String clusterId, String namespace, String middlewareName, AlertRecordQueryDto alertRecordQueryDto);

}
