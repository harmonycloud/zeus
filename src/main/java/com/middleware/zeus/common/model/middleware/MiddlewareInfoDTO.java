package com.middleware.zeus.common.model.middleware;

import java.sql.Blob;
import java.util.Date;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/04/08
 */
@ApiModel("中间件信息")
@Accessors(chain = true)
@Data
public class MiddlewareInfoDTO {

    @ApiModelProperty("自增id")
    private Integer id;

    @ApiModelProperty("中间件名称")
    private String name;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("类型")
    private String type;

    @ApiModelProperty("版本")
    private String version;

    @ApiModelProperty("图片")
    private Blob image;

    @ApiModelProperty("图片地址")
    private String imagePath;

    @ApiModelProperty("状态：0-创建中 1-创建成功  2-待安装  3-运行异常")
    private Integer status;

    @ApiModelProperty
    private Date createTime;

    @ApiModelProperty
    private String versionStatus;

    @ApiModelProperty("chart包名称")
    private String chartName;

    @ApiModelProperty("chart包版本")
    private String chartVersion;

    @ApiModelProperty("grafanna的id")
    private String grafanaId;

    @ApiModelProperty("实例数")
    private Integer replicas;

    @ApiModelProperty("实例整体状态，只有有一个实例异常就是异常")
    private Boolean replicasStatus;

    @ApiModelProperty("实例列表")
    private List<Middleware> middlewares;

    @ApiModelProperty("是否官方：0-非官方 1-官方")
    private Boolean official;

    @Override
    public int hashCode() {
        return chartName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        MiddlewareInfoDTO middlewareInfoDTO = (MiddlewareInfoDTO) obj;
        return this.getChartName().equals(middlewareInfoDTO.getChartName());
    }

}
