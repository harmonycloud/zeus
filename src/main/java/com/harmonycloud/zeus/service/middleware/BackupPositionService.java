package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.BackupPositionDTO;
import com.harmonycloud.caas.common.model.BackupServerDTO;
import com.harmonycloud.zeus.bean.BeanBackupPosition;
import com.harmonycloud.zeus.integration.cluster.bean.Minio;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/12 7:59 上午
 */
public interface BackupPositionService {

    List<BackupPositionDTO> selectBackupPositionDTOList(Integer backupServerId, String projectId);

    List<BackupPositionDTO> selectBackupPositionDTOList(String projectId);

    List<BackupServerDTO> listBackupServerDTO(String projectId);

    List<BackupPositionDTO> list(String clusterId, String namespace);

    List<BeanBackupPosition> listByBackupServerId(Integer backupServerId);

    void create(BackupPositionDTO backupPositionDTO);

    void update(BackupPositionDTO backupPositionDTO);

    void delete(Integer id);

    void deleteByBackupServer(Integer backupServerId);

    BeanBackupPosition getBackupPosition(Integer backupServerId, String projectId);

    BeanBackupPosition getBackupPosition(Integer positionId);

    /**
     * 获取minio
     * @param positionId 备份位置id
     * @param serverUsage 服务器用途，A:可用区A，B可用区B nulL或""：普通minio
     * @return
     */
    Minio getMinio(Integer positionId, String serverUsage);

}
