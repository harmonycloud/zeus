package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yushuaikang
 * @date 2022/6/25 11:21 AM
 */

@ApiModel("备份任务映射")
@Accessors(chain = true)
@Data
public class MiddlewareBackupNameDTO {

    /**
     * id
     */
    private Integer id;

    /**
     * 备份任务标识
     */
    private String backupId;

    /**
     * 备份任务名称
     */
    private String backupAliasName;

    /**
     * 备份类型
     */
    private String backupType;

    /**
     * 集群ID
     */
    private String clusterId;
}
