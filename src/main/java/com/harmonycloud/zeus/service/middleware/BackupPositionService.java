package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.BackupPositionDTO;
import com.harmonycloud.zeus.bean.BeanBackupPosition;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/12 7:59 上午
 */
public interface BackupPositionService {

    List<BackupPositionDTO> selectBackupPositionDTOList(Integer backupServerId);

    void create(BackupPositionDTO backupPositionDTO);

    void delete(Integer id);
    
}
