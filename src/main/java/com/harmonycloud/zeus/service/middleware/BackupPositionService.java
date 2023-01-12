package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.BackupPositionDTO;

/**
 * @author liyinlong
 * @since 2023/1/12 7:59 上午
 */
public interface BackupPositionService {

    void create(BackupPositionDTO backupPositionDTO);

    void delete(Integer id);
    
}
