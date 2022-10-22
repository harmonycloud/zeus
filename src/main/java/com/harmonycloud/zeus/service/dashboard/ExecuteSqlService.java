package com.harmonycloud.zeus.service.dashboard;

import com.harmonycloud.caas.common.model.dashboard.ExecuteSqlDto;

/**
 * @author xutianhong
 * @Date 2022/10/22 9:44 上午
 */
public interface ExecuteSqlService {
    /**
     * 插入执行记录
     * @param executeSqlDto 执行记录业务对象
     *
     **/
    void insert(ExecuteSqlDto executeSqlDto);

}
