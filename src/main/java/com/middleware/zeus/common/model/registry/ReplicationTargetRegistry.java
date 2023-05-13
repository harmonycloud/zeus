package com.middleware.zeus.common.model.registry;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/20 3:08 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像复制目标制品库(备份服务器)")
// TODO 之后产品会优化关于镜像复制时选择目标服务器的逻辑，已经注册到观云台的仓库在业务上应该不需要进行二次注册
public class ReplicationTargetRegistry {
    @ApiModelProperty("目标编号")
    private String id;
    @ApiModelProperty("目标名")
    private String name;
    @ApiModelProperty("目标编号")
    private String type;
    @ApiModelProperty("目标编号")
    private String addr;
    @ApiModelProperty("目标编号")
    private JSONObject auth;
}
