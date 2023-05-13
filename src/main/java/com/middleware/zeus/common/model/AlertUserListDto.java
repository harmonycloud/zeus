package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/5/8 2:39 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("告警用户列表")
public class AlertUserListDto {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("告警用户列表")
    private List<AlertUserDto> alertUserDtoList;

}
