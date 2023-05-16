package com.middleware.zeus.service.middleware;

import com.middleware.zeus.common.model.BackupPositionDTO;
import com.middleware.zeus.bean.BeanBackupPosition;
import com.middleware.zeus.bean.BeanBackupServer;
import com.middleware.zeus.integration.cluster.bean.Minio;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/12 7:59 上午
 */
public interface BackupPositionService {

    /**
     * 查询备份位置列表
     * @param organId 组织id
     * @param projectId  项目id
     * @param backupServerId  备份服务器id
     *
     * @return List<BackupPositionDTO>
     */
    List<BackupPositionDTO> list(String organId, String projectId, Integer backupServerId);

    /**
     * 获取可被使用的备份位置
     * @param organId 组织id
     * @param projectId  项目id
     * @param clusterId  集群id
     * @param namespace 分区
     *
     * @param middlewareName
     * @param type
     * @return List<BackupPositionDTO>
     */
    List<BackupPositionDTO> list(String organId, String projectId, String clusterId, String namespace, String middlewareName, String type);

    /**
     * 创建备份位置
     * @param backupPositionDTO 备份位置对象
     */
    void create(BackupPositionDTO backupPositionDTO);

    /**
     * 更新备份位置信息
     * @param backupPositionDTO 备份位置对象
     */
    void update(BackupPositionDTO backupPositionDTO);

    /**
     * 根据备份位置id删除备份位置
     * @param id 备份位置id
     */
    void delete(Integer id);

    /**
     * 根据备份服务器id删除备份位置
     * @param backupServerId 备份服务器id
     */
    void deleteByBackupServer(Integer backupServerId);

    /**
     * 删除备份位置
     * @param organId 组织id
     * @param projectId 项目id
     */
    void deleteByProjectId(String organId, String projectId);

    /**
     * 查询备份位置
     * @param positionId 备份位置id
     */
    BeanBackupPosition getBackupPosition(Integer positionId);

    /**
     * 获取minio
     * @param positionId 备份位置id
     * @param serverUsage 服务器用途，A:可用区A，B可用区B nulL或""：普通minio
     * @return
     */
    Minio getMinio(Integer positionId, String serverUsage);

    /**
     * 获取备份位置所属的备份服务器
     * @param positionId
     * @return
     */
    BeanBackupServer getBackupServer(Integer positionId);

}
