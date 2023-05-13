package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author chwetion
 * @since 2020/12/18 2:23 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "包内漏洞详情")
public class LeakDetail {
    @ApiModelProperty("CVE编号")
    private String cve;
    @ApiModelProperty("当前版本")
    private String currentVersion;
    @ApiModelProperty("描述")
    private String description;
    @ApiModelProperty("修复版本")
    private String fixedVersion;
    @ApiModelProperty("漏洞等级")
    private String level;
    @ApiModelProperty("漏洞关联urls")
    private List<String> links;
}
