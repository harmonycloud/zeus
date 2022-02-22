package com.harmonycloud.zeus.bean;

import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.model.AlertSummaryDTO;
import com.harmonycloud.caas.common.model.middleware.ClusterQuotaDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBriefInfoDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareOperatorDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
