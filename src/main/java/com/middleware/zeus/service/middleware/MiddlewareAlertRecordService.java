package com.middleware.zeus.service.middleware;

import com.middleware.zeus.bean.BeanAlertRecord;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/4/6 3:02 下午
 */
public interface MiddlewareAlertRecordService {

    /**
     * 查询告警记录
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param current
     * @param pageSize
     * @param keyword
     * @return
     */
    List<BeanAlertRecord> list(String clusterId, String namespace, String middlewareName, Integer current, Integer pageSize, String keyword);

}
