package com.middleware.zeus.service.middleware;

import com.github.pagehelper.PageInfo;
import com.middleware.zeus.bean.BeanAlertRecord;

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
     * @param size
     * @param keyword
     * @param level
     * @param normalTimeOrder
     * @return
     */
    PageInfo list(String clusterId, String namespace, String middlewareName, Integer current, Integer size, String keyword, String level, Boolean normalTimeOrder);

}
