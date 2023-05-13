package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/11/3 4:39 下午
 */
@ApiModel("mysql数据表引擎")
@Accessors(chain = true)
@Data
public class MysqlEngineDto implements Comparable<MysqlEngineDto>{

    @ApiModelProperty("引擎")
    private String engine;

    @ApiModelProperty("索引类型列表")
    private List<String> indexTypes;

    @ApiModelProperty("存储类型列表")
    private List<String> storageTypes;

    @ApiModelProperty("是否支持外键")
    private Boolean supportForeignKey;

    @Override
    public int compareTo(MysqlEngineDto o) {
        return this.engine.compareTo(o.engine);
    }

}
