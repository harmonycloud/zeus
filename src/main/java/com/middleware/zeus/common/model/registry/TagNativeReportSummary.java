package com.middleware.zeus.common.model.registry;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/01/18
 */
@Accessors(chain = true)
@Data
public class TagNativeReportSummary {

    private String reportId;
    private String startTime;
    private String endTime;
    private String severity;
    private String duration;
    private String scanStatus;
    private TagVulnerabilitySummary summary;


}
