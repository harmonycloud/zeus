package com.middleware.zeus.service.middleware;

import com.middleware.zeus.common.model.AlertRecordDo;

/**
 * @author xutianhong
 * @Date 2023/6/5 2:59 下午
 */
public interface AlertRecordService {

    /***
     * 查询告警记录
     *
     * @param alertRecordDo 告警记录对象
     */
    void insert(AlertRecordDo alertRecordDo);

    /***
     * 清理告警记录
     *
     */
    void clear();

}
