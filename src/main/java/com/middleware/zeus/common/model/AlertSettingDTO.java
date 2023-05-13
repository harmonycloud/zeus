package com.middleware.zeus.common.model;

import com.middleware.zeus.common.model.user.UserDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

/**
 * @author liyinlong
 * @since 2022/7/8 5:39 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("告警设置")
public class AlertSettingDTO {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区名")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("告警类型")
    private String lay;

    @ApiModelProperty("是否开启钉钉告警")
    private Boolean enableDingAlert;

    @ApiModelProperty("是否开启邮件告警")
    private Boolean enableMailAlert;

    @ApiModelProperty("告警接收人列表")
    private List<UserDto> userList;

    @ApiModelProperty("告警接收人id列表")
    private Set<Integer> userIds;

}
