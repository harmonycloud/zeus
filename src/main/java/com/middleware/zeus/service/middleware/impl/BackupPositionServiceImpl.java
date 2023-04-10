package com.middleware.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dtflys.forest.utils.StringUtils;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.BackupPositionDTO;
import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.caas.common.model.user.ProjectDto;
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

    @Override
    public List<BackupPositionDTO> list(String organId, String projectId, Integer backupServerId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();

        if (StringUtils.isNotEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(projectId)) {
            wrapper.eq("project_id", projectId);
        }
        if (backupServerId != null) {
            wrapper.eq("backup_server_id", backupServerId);
        }
        List<BeanBackupPosition> beanBackupPositions = backupPositionMapper.selectList(wrapper);
        return convert(beanBackupPositions);
    }

    @Override
    public List<BackupPositionDTO> usable(String organId, String projectId, String clusterId, String namespace) {
        List<BackupPositionDTO> backupPositionDTOList = this.list(organId, projectId, null);
        boolean openAvailableDomain = namespaceService.isOpenAvailableDomain(clusterId, namespace);

        return backupPositionDTOList.stream().filter(backupPositionDTO -> {
            BeanBackupServer beanBackupServer = backupServerService.get(backupPositionDTO.getBackupServerId());
            if (beanBackupServer == null) {
                return false;
            }
            if (!beanBackupServer.getClusterId().equals(clusterId)){
                return false;
            }
            backupPositionDTO.setBackupServerName(beanBackupServer.getName());
            // 过滤双活备份服务器
            if (!openAvailableDomain) {
                return beanBackupServer.getType() == 1;
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public void create(BackupPositionDTO backupPositionDTO) {
        // 校验该项目是否已使用该备份服务器创建备份位置
        if (this.getBackupPosition(backupPositionDTO.getBackupServerId(), backupPositionDTO.getOrganId(), backupPositionDTO.getProjectId()) != null) {
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
    public void deleteByProjectId(String organId, String projectId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("organ_id", organId).eq("project_id", projectId);
        backupPositionMapper.delete(wrapper);
    }

    public BeanBackupPosition getBackupPosition(Integer backupServerId, String organId, String projectId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
        wrapper.eq("organ_id", organId);
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
            ProjectDto projectDto = projectService.get(beanBackupPosition.getOrganId(), beanBackupPosition.getProjectId());
            if (projectDto != null) {
                backupPositionDTO.setProjectName(projectDto.getAliasName());
                // todo 修改备份任务引用数量的查询逻辑
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
