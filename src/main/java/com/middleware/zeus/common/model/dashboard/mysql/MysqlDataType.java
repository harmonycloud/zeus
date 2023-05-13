package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/10/28 11:56 上午
 */
@ApiModel("mysql数据类型")
@Accessors(chain = true)
@Data
public class MysqlDataType {

    private int id;

    private String name;

    private boolean optionsAble;

    private boolean autoIncrement;

    public MysqlDataType() {
    }

    public MysqlDataType(int id, String name, boolean optionsAble, boolean autoIncrement) {
        this.id = id;
        this.name = name;
        this.optionsAble = optionsAble;
        this.autoIncrement = autoIncrement;
    }

}
