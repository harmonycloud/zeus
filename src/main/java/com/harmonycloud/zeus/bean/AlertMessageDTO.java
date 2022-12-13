package com.harmonycloud.zeus.bean;

import com.github.pagehelper.PageInfo;
import com.middleware.caas.common.model.AlertSummaryDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liyinlong
 * @since 2021/9/24 3:42 下午
 */
@Data
@NoArgsConstructor
public class AlertMessageDTO {

    /**
     * 告警信息
     */
    private AlertSummaryDTO alertSummary;

    /**
     * 告警事件
     */
    private PageInfo alertPageInfo;

}
