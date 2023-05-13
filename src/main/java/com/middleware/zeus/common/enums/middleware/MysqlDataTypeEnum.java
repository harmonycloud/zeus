package com.middleware.zeus.common.enums.middleware;

import com.middleware.zeus.common.model.dashboard.mysql.MysqlDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/10/25 8:10 下午
 */
public enum MysqlDataTypeEnum {

    T_INT(1, "int", true, true),
    T_VARCHAR(2, "varchar", true, false),
    T_DECIMAL(3, "decimal", true, true),
    T_BLOB(5, "blob", false, false),
    T_BINARY(6, "binary", true, false),
    T_TINYBLOB(7, "tinyblob", false, false),
    T_LONGBLOB(8, "longblob", false, false),
    T_MEDIUMBLOB(9, "mediumblob", false, false),
    T_DATE(10, "date", false, false),
    T_DATETIME2(11, "datetime", true, false),
    T_TIME(12, "time", false, false),
    T_TIMESTAMP(13, "timestamp", true, false),
    T_YEAR(14, "year", true, false),
    T_GEOMETRY(15, "geometry", false, false),
    T_GEOMETRYCOLLECTION(16, "geometrycollection", false, false),
    T_LINESTRING(17, "linestring", false, false),
    T_MULTILINESTRING(18, "multilinestring", false, false),
    T_MULTIPOINT(19, "multipoint", false, false),
    T_MULTIPOLYGON(20, "multipolygon", false, false),
    T_POINT(21, "point", false, false),
    T_POLYGON(22, "polygon", false, false),
    T_BIGINT(23, "bigint", true, true),
    T_DOUBLE(24, "double", false, true),
    T_FLOAT(25, "float", false, true),
    T_MEDIUMINT(26, "mediumint", true, true),
    T_SMALLINT(27, "smallint", true, true),
    T_TINYINT(28, "tinyint", true, true),
    T_CHAR(29, "char", true, false),
    T_JSON(30, "json", false, false),
    T_LONGTEXT(31, "longtext", false, false),
    T_MEDIUMTEXT(32, "mediumtext", false, false),
    T_TINYTEXT(33, "tinytext", false, false),
    T_BIT(34, "bit", false, false);

    private final int id;

    private final String name;

    private final boolean optionsAble;

    private final boolean autoIncrement;

    MysqlDataTypeEnum(int id, String name, boolean optionsAble, boolean autoIncrement) {
        this.id = id;
        this.name = name;
        this.optionsAble = optionsAble;
        this.autoIncrement = autoIncrement;
    }

    public static List<MysqlDataType> dataTypeList = new ArrayList<>();

    static {
        for (MysqlDataTypeEnum value : MysqlDataTypeEnum.values()) {
            dataTypeList.add(new MysqlDataType(value.id,value.name,value.optionsAble,value.autoIncrement));
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isOptionsAble() {
        return optionsAble;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }
}
