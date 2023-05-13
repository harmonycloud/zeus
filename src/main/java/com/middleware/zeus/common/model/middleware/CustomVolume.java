package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/12/1 11:07 上午
 */
@Accessors(chain = true)
@Data
@ApiModel("自定义挂载目录")
public class CustomVolume {

    @ApiModelProperty("容器内目录标识(不可修改)")
    private String  name;

    @ApiModelProperty("容器内数据目录")
    private String mountPath;

    @ApiModelProperty("容器名称(不可修改)")
    private List<String> targetContainers;

    @ApiModelProperty("storageClass名称")
    private String storageClass;

    @ApiModelProperty("磁盘大小")
    private String volumeSize;

    @ApiModelProperty("宿主机目录")
    private String hostPath;

}
