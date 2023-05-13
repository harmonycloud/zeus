package com.middleware.zeus.common.model.registry;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/08
 */
@Accessors(chain = true)
@Data
public class ProjectSummaryQuota {

    /**
     * 配额
     */
    private ProjectQuota hard;
    /**
     * 使用量
     */
    private ProjectQuota used;

    @Accessors(chain = true)
    @Data
    public static class ProjectQuota {
        /**
         * 镜像数量
         */
        private Long count;
        /**
         * 磁盘存储
         */
        private Long storage;
    }

}
