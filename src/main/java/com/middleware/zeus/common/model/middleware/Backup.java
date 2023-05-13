package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/6 5:33 下午
 */
@Data
@Accessors(chain = true)
public class Backup {

    @ApiModelProperty("名称")
    private String name;
    @ApiModelProperty("cr名称")
    private String crName;
    @ApiModelProperty("分区")
    private String namespace;
    @ApiModelProperty("备份时间")
    private String backupTime;
    @ApiModelProperty("创建时间")
    private String creationTime;
    @ApiModelProperty("备份名称")
    private String backupName;
    @ApiModelProperty("备份文件名")
    private String backupFileName;
    @ApiModelProperty("备份状态")
    private String phase;
    @ApiModelProperty("备份中间件名称")
    private String middlewareCluster;
    @ApiModelProperty("")
    private String controllerName;
    @ApiModelProperty("minio地址")
    private String endPoint;
    @ApiModelProperty("bucket名称")
    private String bucketName;
    @ApiModelProperty("备份位置id")
    private String addressId;
    @ApiModelProperty("备份任务名称")
    private String backupId;
    @ApiModelProperty("类型")
    private String type;
    @ApiModelProperty("所属备份")
    private String owner;

}
