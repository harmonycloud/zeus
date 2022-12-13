package com.middleware.zeus.config;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

public class MyLambdaUpdateWrapper<T> extends LambdaUpdateWrapper<T> {
    public MyLambdaUpdateWrapper(Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * 指定列⾃增
     *
     * 列引⽤
     * 
     * @param columns 增长值
     * @param value
     */
    public MyLambdaUpdateWrapper<T> incrField(SFunction<T, ?> columns, Object value) {
        String columnsToString = super.columnToString(columns);
        String format = String.format("%s =  %s + %s", columnsToString, columnsToString, formatSql("{0}", value));
        setSql(format);
        return this;
    }

    /**
     * 指定列⾃减
     *
     * 列引⽤
     * 
     * @param columns 减少值
     * @param value
     */
    public MyLambdaUpdateWrapper<T> descField(SFunction<T, ?> columns, Object value) {
        String columnsToString = super.columnToString(columns);
        String format = String.format("%s =  %s - %s", columnsToString, columnsToString, formatSql("{0}", value));
        setSql(format);
        return this;
    }

}
