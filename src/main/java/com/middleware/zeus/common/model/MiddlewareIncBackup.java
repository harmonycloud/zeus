package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @description
 * @author  liyinlong
 * @since 2023/1/30 4:31 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "增量备份对象")
public class MiddlewareIncBackup {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区名称")
    private String namespace;

    @ApiModelProperty("备份名称")
    private String backupName;

    @ApiModelProperty("是否关闭：off/on")
    private String pause;

    @ApiModelProperty("n分钟周期")
    private String time;

    /**
     * 注解
     */
    private Map<String, String> annotations;

    /**
     * labels
     */
    private Map<String, String> labels;

}
