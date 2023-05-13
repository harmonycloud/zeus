package com.middleware.zeus.common.model;

import com.middleware.zeus.common.model.middleware.BackupServerDetailDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/9 2:06 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("备份服务器信息")
public class BackupServerDTO {

    @ApiModelProperty("备份服务器id")
    private Integer id;

    @ApiModelProperty("备份服务器名称")
    private String name;

    @ApiModelProperty("所属集群")
    private String clusterId;

    @ApiModelProperty("集群别名")
    private String clusterNickName;

    @ApiModelProperty("备份服务器类型（1：普通备份服务器，2：双活备份服务器）")
    private Integer type;

    @ApiModelProperty("服务器介质类型（S3、FTP、server）")
    private String serverType;

    @ApiModelProperty("使用中")
    private Boolean using;

    @ApiModelProperty("备份服务器详细信息列表")
    private List<BackupServerDetailDTO> serverDetailList;

    @ApiModelProperty("备份位置列表")
    private List<BackupPositionDTO> positionList;

}
