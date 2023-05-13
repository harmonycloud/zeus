package com.middleware.zeus.common.enums.middleware;

/**
 * @author xutianhong
 * @Date 2022/10/25 8:10 下午
 */
public enum PostgresqlDataTypeEnum {

    BIGINT("bigint"),
    BIGSERIAL("bigserial"),
    BIT("bit"),
    BIT_VARYING("bit varying"),
    BOOLEAN("boolean"),
    BOX("box"),
    BYTEA("bytea"),
    CHARACTER("character"),
    CHARACTER_VARYING("character varying"),
    CIDR("cidr"),
    CIRCLE("circle"),
    DATE("date"),
    DOUBLE_PRECISION("double precision"),
    INET("inet"),
    INTEGER("integer"),
    INTERVAL("interval"),
    JSON("json"),
    JSONB("jsonb"),
    LINE("line"),
    LSEGNA("lsegna"),
    MACADDR("macaddr"),
    MACADDR8("macaddr8"),
    MONEY("money"),
    NUMERIC("numeric"),
    PATH("path"),
    PG_LSN("pg_lsn"),
    PG_SNAPSHOT("pg_snapshot"),
    POINT("point"),
    POLOYGON("polygon"),
    REAL("real"),
    SMALLINT("smallint"),
    SMALLSERIAL("smallserial"),
    SERIAL("serial"),
    TEXT("text"),
    TIME("time"),
    TIME_WITH_TIME_ZONE("time with time zone"),
    TIMESTAMP("timestamp"),
    TIMESTAMP_WITH_TIME_ZONE("timestamp with time zone"),
    TSQUERY("tsquery"),
    TSVECTOR("tsvector"),
    TXID_SNAPSHOT("txid_snapshot"),
    UUID("uuid"),
    XML("xml"),
    ;

    private String name;


    PostgresqlDataTypeEnum(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

}
