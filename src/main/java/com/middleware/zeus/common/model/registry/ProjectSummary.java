package com.middleware.zeus.common.model.registry;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/08
 */
@Accessors(chain = true)
@Data
public class ProjectSummary {

    /**
     * helm chart数量
     */
    private Long chartCount;
    /**
     * 镜像数量
     */
    private Long repoCount;
    /**
     * 容量
     */
    private ProjectSummaryQuota quota;


}
