package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/3/25 3:19 下午
 */

@Accessors(chain = true)
@Data
@ApiModel("mysql数据库信息")
public class MysqlDbDTO {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区名称")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("数据库id")
    private String id;

    @ApiModelProperty("数据库名称")
    private String db;

    @ApiModelProperty("数据库字符集")
    private String charset;

    @ApiModelProperty("数据库描述")
    private String description;

    /**
     * mysql中的字符集
     */
    private String Charset;

    /**
     * mysql中的字符集描述
     */
    private String Description;

    @ApiModelProperty("用户数据库关联列表")
    private List<MysqlDbPrivilege> privileges;

    public void setCharset(String charset) {
        Charset = charset;
        this.charset = charset;
    }

    public void setDescription(String description) {
        Description = description;
        this.description = description;
    }

}
