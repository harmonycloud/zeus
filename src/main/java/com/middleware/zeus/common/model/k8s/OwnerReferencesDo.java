package com.middleware.zeus.common.model.k8s;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/7/9 10:26
 */
@Data
@Accessors(chain = true)
public class OwnerReferencesDo {

    @ApiModelProperty("apiVersion")
    private String apiVersion;

    @ApiModelProperty("controller")
    private Boolean controller;

    @ApiModelProperty("kind")
    private String kind;

    @ApiModelProperty("name")
    private String name;

    @ApiModelProperty("uid")
    private String uid;

    @ApiModelProperty("blockOwnerDeletion")
    private Boolean blockOwnerDeletion;
}
