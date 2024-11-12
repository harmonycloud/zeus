package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.BackupPositionDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @author yushuaikang
 * @date 2022/6/6 15:15 下午
 */
@ApiModel("备份服务器详细信息")
@Accessors(chain = true)
@Data
public class BackupServerDetailDTO {

    @ApiModelProperty("id")
    private Integer id;

    @ApiModelProperty("备份服务器id")
    private Integer backupServerId;

    @ApiModelProperty("介质类型：1: S3. 2: ftp: 3: server")
    private Integer type;

    @ApiModelProperty("服务器用途,仅当备份服务器类型为2时才需要设置改字段：A:可用区A，B:可用区B")
    private String serverUsage;

    @ApiModelProperty("协议")
    private String protocol;

    @ApiModelProperty("主机")
    private String host;

    @ApiModelProperty("端口")
    private String port;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("备份位置列表")
    private List<BackupPositionDTO> positionList;

}
