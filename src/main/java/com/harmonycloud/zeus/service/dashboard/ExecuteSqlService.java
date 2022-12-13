package com.harmonycloud.zeus.service.dashboard;

import com.github.pagehelper.PageInfo;
import com.middleware.caas.common.model.SqlRecordQueryDto;
import com.middleware.caas.common.model.dashboard.ExecuteSqlDto;
import com.harmonycloud.zeus.bean.BeanSqlExecuteRecord;

/**
 * @author xutianhong
 * @Date 2022/10/22 9:44 上午
 */
public interface ExecuteSqlService {
    /**
     * 插入执行记录
     * @param  beanSqlExecuteRecord 执行记录业务对象
     *
     **/
    void insert(BeanSqlExecuteRecord beanSqlExecuteRecord);

    /**
     * 查询执行记录
     * @param keyword 关键词
     * @param current 当前页
     * @param size 页大小
     * @param order 排序
     *
     * @return PageInfo<ExecuteSqlDto>
     **/
    PageInfo<ExecuteSqlDto> list(String keyword, Integer current, Integer size, String order);

    PageInfo<BeanSqlExecuteRecord> listExecuteSql(String clusterId, String namespace, String middlewareName, String database, SqlRecordQueryDto sqlRecordQueryDto);

}
