package com.middleware.zeus.service.middleware;

import com.middleware.zeus.common.model.middleware.BackupServerDetailDTO;
import com.middleware.zeus.bean.BeanBackupServerDetail;

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

    List<BeanBackupServerDetail> findByAddress(String protocol, String host, String port, Integer serverType);

}
