package com.middleware.zeus.common.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/9/24 3:39 下午
 */
@Data
public class AlertSummaryDTO {

    /**
     * 提示告警数量
     */
    private int infoSum;
    /**
     * 严重告警数量
     */
    private int criticalSum;
    /**
     * 警告告警数量
     */
    private int warningSum;

    /**
     * 提示告警列表
     */
    private List<Map<String, Object>> infoList;

    /**
     * 严重告警数量
     */
    private List<Map<String, Object>> criticalList;

    /**
     * 警告告警数量
     */
    private List<Map<String, Object>> warningList;
}
