package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.zeus.bean.BeanMiddlewareBackupName;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/2/9 2:51 下午
 */
public interface MiddlewareBackupNameService {

    /**
     * 查询指定备份位置相关联的备份任务列表
     * @param positionId
     * @return
     */
    List<BeanMiddlewareBackupName> listByPositionId(Integer positionId);

    /**
     * 根据backupId查询备份任务名称信息
     * @param backupId
     * @return
     */
    BeanMiddlewareBackupName getByBackupId(String backupId);

}
