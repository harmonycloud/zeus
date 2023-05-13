package com.middleware.zeus.common.model.dashboard;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author liyinlong
 * @since 2022/11/9 2:45 下午
 */
@Data
public class SqlQuery {

    private String sql;

    /**
     * 是否是查询语句,但sql以select或show开头时，则为查询语句
     */
    private boolean query;

    public SqlQuery(String sql) {
        this.sql = sql;
    }

    public boolean convertQuery() {
        if (!StringUtils.isEmpty(sql)) {
            String[] sqls = sql.trim().split(" ");
            return "select".equalsIgnoreCase(sqls[0]) || "show".equalsIgnoreCase(sqls[0]);
        }
        return false;
    }

    public void convertAndSetQuery(){
        this.query = convertQuery();
    }

}
