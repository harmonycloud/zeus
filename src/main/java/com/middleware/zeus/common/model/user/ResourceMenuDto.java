package com.middleware.zeus.common.model.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * @author xutianhong
 * @Date 2021/7/28 4:08 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("菜单栏")
public class ResourceMenuDto implements Comparable<ResourceMenuDto> {

    @ApiModelProperty("id")
    private Integer id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("中文名称")
    private String aliasName;

    @ApiModelProperty("权重排序")
    private Integer weight;

    @ApiModelProperty("父菜单id")
    private Integer parentId;

    @ApiModelProperty("链接")
    private String url;

    @ApiModelProperty("icon名称")
    private String iconName;

    @ApiModelProperty("模块")
    private String module;

    @ApiModelProperty("是否可用")
    private Boolean available;

    @ApiModelProperty("子菜单")
    private List<ResourceMenuDto> subMenu;

    @ApiModelProperty("角色权限")
    private Boolean own;

    @Override
    public int compareTo(ResourceMenuDto menu) {
        if (Objects.isNull(menu)){
            return -1;
        }
        return this.getWeight().compareTo(menu.getWeight());
    }
}
