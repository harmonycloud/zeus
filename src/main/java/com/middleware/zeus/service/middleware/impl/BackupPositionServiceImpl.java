package com.middleware.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dtflys.forest.utils.StringUtils;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.BackupPositionDTO;
import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.zeus.bean.BeanBackupPosition;
import com.middleware.zeus.bean.BeanBackupServer;
import com.middleware.zeus.bean.BeanBackupServerDetail;
import com.middleware.zeus.bean.BeanMiddlewareBackupName;
import com.middleware.zeus.bean.user.BeanProject;
import com.middleware.zeus.dao.BeanBackupPositionMapper;
import com.middleware.zeus.integration.cluster.bean.Minio;
import com.middleware.zeus.service.k8s.NamespaceService;
import com.middleware.zeus.service.middleware.BackupPositionService;
import com.middleware.zeus.service.middleware.BackupServerDetailService;
import com.middleware.zeus.service.middleware.BackupServerService;
import com.middleware.zeus.service.middleware.MiddlewareBackupNameService;
import com.middleware.zeus.service.user.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2023/1/12 8:00 上午
 */
@Service
@Transactional
public class BackupPositionServiceImpl implements BackupPositionService {

    @Autowired
    private BeanBackupPositionMapper backupPositionMapper;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private BackupServerService backupServerService;
    @Autowired
    private BackupServerDetailService backupServerDetailService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private MiddlewareBackupNameService middlewareBackupNameService;

    /**
     * 查询指定备份服务器的全部备份位置
     *
     * @param backupServerId
     * @param projectId
     * @return
     */
    @Override
    public List<BackupPositionDTO> selectBackupPositionDTOList(Integer backupServerId, String projectId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
        if (StringUtils.isNotEmpty(projectId)) {
            wrapper.eq("project_id", projectId);
        }
        List<BeanBackupPosition> beanBackupPositions = backupPositionMapper.selectList(wrapper);
        return convert(beanBackupPositions);
    }

    /**
     * 查询指定项目的全部备份位置
     *
     * @param projectId
     * @return
     */
    @Override
    public List<BackupPositionDTO> selectBackupPositionDTOList(String projectId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        List<BeanBackupPosition> beanBackupPositions = backupPositionMapper.selectList(wrapper);
        return convert(beanBackupPositions);
    }

    /**
     * 查询项目备份位置列表
     *
     * @param projectId
     * @return
     */
    @Override
    public List<BackupServerDTO> listBackupServerDTO(String projectId) {
        return backupServerService.listBackupPosition(projectId);
    }

    @Override
    public List<BackupPositionDTO> list(String clusterId, String namespace) {
        String projectId = projectService.getProjectId(clusterId, namespace);
        List<BackupPositionDTO> backupPositionDTOS = selectBackupPositionDTOList(projectId);
        boolean openAvailableDomain = namespaceService.isOpenAvailableDomain(clusterId, namespace);
        return backupPositionDTOS.stream().filter(backupPositionDTO -> {
            BeanBackupServer beanBackupServer = backupServerService.get(backupPositionDTO.getBackupServerId());
            if (beanBackupServer == null) {
                return false;
            }
            backupPositionDTO.setBackupServerName(beanBackupServer.getName());
            if (!openAvailableDomain) {
                return beanBackupServer.getType() == 1;
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public List<BeanBackupPosition> listByBackupServerId(Integer backupServerId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
        return backupPositionMapper.selectList(wrapper);
    }

    @Override
    public void create(BackupPositionDTO backupPositionDTO) {
        // 校验该项目是否已使用该备份服务器创建备份位置
        if (this.getBackupPosition(backupPositionDTO.getBackupServerId(), backupPositionDTO.getProjectId()) != null) {
            throw new BusinessException(ErrorMessage.BACKUP_SERVER_ALREADY_USED);
        }
        BeanBackupPosition backupPosition = new BeanBackupPosition();
        BeanUtil.copyProperties(backupPositionDTO, backupPosition);
        backupPositionMapper.insert(backupPosition);
    }

    @Override
    public void update(BackupPositionDTO backupPositionDTO) {
        BeanBackupPosition backupPosition = new BeanBackupPosition();
        BeanUtil.copyProperties(backupPositionDTO, backupPosition);
        backupPositionMapper.updateById(backupPosition);
    }

    @Override
    public void delete(Integer id) {
        // 检查备份位置是否已被备份任务使用
        backupPositionDeletionCheck(id);
        backupPositionMapper.deleteById(id);
    }

    @Override
    public void deleteByBackupServer(Integer backupServerId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
        backupPositionMapper.delete(wrapper);
    }

    @Override
    public void deleteByProjectId(String projectId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        backupPositionMapper.delete(wrapper);
    }

    @Override
    public BeanBackupPosition getBackupPosition(Integer backupServerId, String projectId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
        wrapper.eq("project_id", projectId);
        List<BeanBackupPosition> beanBackupPositions = backupPositionMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanBackupPositions)) {
            return beanBackupPositions.get(0);
        }
        return null;
    }

    @Override
    public BeanBackupPosition getBackupPosition(Integer positionId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("id", positionId);
        List<BeanBackupPosition> beanBackupPositions = backupPositionMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanBackupPositions)) {
            return null;
        }
        return beanBackupPositions.get(0);
    }

    @Override
    public Minio getMinio(Integer positionId, String serverUsage) {
        BeanBackupPosition backupPosition = getBackupPosition(positionId);
        BeanBackupServer beanBackupServer = backupServerService.get(backupPosition.getBackupServerId());
        BeanBackupServerDetail backupServerDetail = backupServerDetailService.getBackupServerDetail(beanBackupServer.getId(), serverUsage);
        Minio minio = new Minio();
        String port = StringUtils.isEmpty(backupServerDetail.getPort()) ? "" : ":" + backupServerDetail.getPort();
        String endPoint = backupServerDetail.getProtocol() + "://" + backupServerDetail.getHost() + port;
        minio.setBucketName(backupPosition.getBackupPosition().replace("/",""));
        minio.setAccessKeyId(backupServerDetail.getUsername());
        minio.setSecretAccessKey(backupServerDetail.getPassword());
        minio.setEndpoint(endPoint);
        return minio;
    }

    @Override
    public BeanBackupServer getBackupServer(Integer positionId) {
        BeanBackupPosition backupPosition = getBackupPosition(positionId);
        BeanBackupServer beanBackupServer = backupServerService.get(backupPosition.getBackupServerId());
        return beanBackupServer;
    }

    // 转换数据类型
    private List<BackupPositionDTO> convert(List<BeanBackupPosition> beanBackupPositions) {
        List<BackupPositionDTO> positionList = new ArrayList<>();
        for (BeanBackupPosition beanBackupPosition : beanBackupPositions) {
            BackupPositionDTO backupPositionDTO = new BackupPositionDTO();
            BeanUtil.copyProperties(beanBackupPosition, backupPositionDTO);
            BeanProject beanProject = projectService.get(beanBackupPosition.getProjectId());
            if (beanProject != null) {
                backupPositionDTO.setProjectName(beanProject.getAliasName());
                backupPositionDTO.setBackupTaskNum(middlewareBackupNameService.listByPositionId(beanBackupPosition.getId()).size());
                positionList.add(backupPositionDTO);
            }
        }
        return positionList;
    }

    /**
     * 检查指定备份位置是否已被备份任务使用
     * @param positionId
     */
    private void backupPositionDeletionCheck(Integer positionId) {
        List<BeanMiddlewareBackupName> middlewareBackupNames = middlewareBackupNameService.listByPositionId(positionId);
        if (!CollectionUtils.isEmpty(middlewareBackupNames)) {
            throw new BusinessException(ErrorMessage.FAILED_TO_DELETE_BACKUP_POSITION);
        }
    }

}
