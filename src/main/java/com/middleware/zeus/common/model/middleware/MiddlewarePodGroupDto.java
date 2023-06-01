package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/6/1 3:54 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("中间件pod group信息")
public class MiddlewarePodGroupDto {

    @ApiModelProperty("角色")
    private String role;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("pod列表")
    private List<String> podList;

}
