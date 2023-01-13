package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.middleware.BackupServerDetailDTO;
import com.harmonycloud.zeus.bean.BeanBackupServerDetail;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/11 11:35 上午
 */
public interface BackupServerDetailService {

    List<BackupServerDetailDTO> listBackupServerDetailDTOS(Integer backupServerId);

    List<BeanBackupServerDetail> listByBackupServerId(Integer backupServerId);

    void create(int serverId, List<BackupServerDetailDTO> serverDetailDTOS);

    void update(List<BackupServerDetailDTO> serverDetailDTOS);

    void deleteByServerId(Integer serverId);

    BeanBackupServerDetail getBackupServerDetail(Integer serverId, String usage);

}
