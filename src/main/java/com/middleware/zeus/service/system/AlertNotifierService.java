package com.middleware.zeus.service.system;

import com.alibaba.fastjson.JSONArray;
import com.middleware.zeus.common.model.AlertRecordDo;
import com.middleware.zeus.common.model.AlertUserDo;

import java.util.List;

/**
 * 这个类定义了一个接口，用于发送告警消息。
 * @author liyinlong
 * @since 2023/5/16 3:54 下午
 */
public interface AlertNotifierService {

    /**
     * 发送告警消息
     * @param alertRecordDo
     * @param alertUserDoList
     * @return 告警结果
     */
    JSONArray sendSMSAlertMessage(AlertRecordDo alertRecordDo, List<AlertUserDo> alertUserDoList);

}
