package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @author dengyulong
 * @date 2020/12/11
 */
@ApiModel(description = "项目与制品服务关联")
@Accessors(chain = true)
@Data
public class RegistryProject {

    @ApiModelProperty("关联自增id")
    private Integer id;
    @ApiModelProperty("制品服务id")
    private String registryId;
    @ApiModelProperty("项目id")
    private String projectId;
    @ApiModelProperty("项目名称")
    private String projectName;
    @ApiModelProperty("用户名")
    private String username;
    @ApiModelProperty("密码")
    private String password;
    @ApiModelProperty("制品服务信息")
    private Registry registry;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RegistryProject that = (RegistryProject) o;
        return Objects.equals(id, that.id) &&
                registryId.equals(that.registryId) &&
                projectId.equals(that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, registryId, projectId);
    }

}
