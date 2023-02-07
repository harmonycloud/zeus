package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dtflys.forest.utils.StringUtils;
import com.harmonycloud.caas.common.enums.BackupServerTypeEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.BackupPositionDTO;
import com.harmonycloud.caas.common.model.BackupServerDTO;
import com.harmonycloud.zeus.bean.BeanBackupPosition;
import com.harmonycloud.zeus.bean.BeanBackupServer;
import com.harmonycloud.zeus.bean.BeanBackupServerDetail;
import com.harmonycloud.zeus.bean.user.BeanProject;
import com.harmonycloud.zeus.dao.BeanBackupPositionMapper;
import com.harmonycloud.zeus.integration.cluster.bean.Minio;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.middleware.BackupPositionService;
import com.harmonycloud.zeus.service.middleware.BackupServerDetailService;
import com.harmonycloud.zeus.service.middleware.BackupServerService;
import com.harmonycloud.zeus.service.user.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
            if(!openAvailableDomain && beanBackupServer.getType() == 2){
                return false;
            }
            if (beanBackupServer != null) {
                backupPositionDTO.setBackupServerName(beanBackupServer.getName());
                return true;
            }
            return false;
        }).collect(Collectors.toList());
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
        backupPositionMapper.deleteById(id);
    }

    @Override
    public void deleteByBackupServer(Integer backupServerId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
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

    // 转换数据类型
    private List<BackupPositionDTO> convert(List<BeanBackupPosition> beanBackupPositions) {
        return beanBackupPositions.stream().map(beanBackupPosition -> {
            BackupPositionDTO backupPositionDTO = new BackupPositionDTO();
            BeanUtil.copyProperties(beanBackupPosition, backupPositionDTO);
            BeanProject beanProject = projectService.get(beanBackupPosition.getProjectId());
            backupPositionDTO.setProjectName(beanProject.getAliasName());
            return backupPositionDTO;
        }).collect(Collectors.toList());
    }

}
