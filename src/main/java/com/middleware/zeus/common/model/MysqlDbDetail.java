package com.middleware.zeus.common.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 数据库详细信息
 * @author liyinlong
 * @since 2022/3/25 4:39 下午
 */
@Data
public class MysqlDbDetail {

    /**
     * 数据库id
     */
    private int id;

    /**
     * 数据库名
     */
    private String db;

    /**
     * mysql字符集
     */
    private String charset;

    /**
     * 数据库用户关联列表
     */
    private List<MysqlDbPrivilege> users;

    /**
     * 数据库描述
     */
    private String description;

    /**
     * 数据库创建时间
     */
    private Date createTime;

    /**
     * 数据库名称（在mysql中的数据库名称字段）
     */
    private String SCHEMA_NAME;

    /**
     * 数据库字符集（在mysql中的数据库字符集）
     */
    private String DEFAULT_CHARACTER_SET_NAME;

    public void setSCHEMA_NAME(String SCHEMA_NAME) {
        this.SCHEMA_NAME = SCHEMA_NAME;
        this.db = SCHEMA_NAME;
    }

    public void setDEFAULT_CHARACTER_SET_NAME(String DEFAULT_CHARACTER_SET_NAME) {
        this.DEFAULT_CHARACTER_SET_NAME = DEFAULT_CHARACTER_SET_NAME;
        this.charset = DEFAULT_CHARACTER_SET_NAME;
    }

}
