package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author wangpenglei
 * @Date 2023/1/9 上午9:39
 **/
@NoArgsConstructor
@Accessors(chain = true)
@Data
@ApiModel("自动切换信息")
public class SwitchInfo {

    @ApiModelProperty("是否自动切换")
    private Boolean isAuto;

    @ApiModelProperty("自动切换获取状态")
    private Boolean status;

    @ApiModelProperty("最后切换时间")
    private Date lastAutoSwitchTime;

    @ApiModelProperty("新的主节点名称")
    private String newMasterName;
}