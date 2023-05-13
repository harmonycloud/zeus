package com.middleware.zeus.service.user.abstractService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.zeus.common.constants.CommonConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.BackupServerDTO;
import com.middleware.zeus.common.model.ProjectBackupServerDTO;
import com.middleware.zeus.common.model.user.OrganizationQuota;
import com.middleware.zeus.bean.user.BeanOrganizationBackupServer;
import com.middleware.zeus.dao.user.BeanOrganizationBackupServerMapper;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.middleware.BackupServerService;
import com.middleware.zeus.service.middleware.ProjectBackupServerService;
import com.middleware.zeus.service.user.ProjectService;

/**
 * @author xutianhong
 * @Date 2023/3/24 2:23 下午
 */
public abstract class AbstractOrganizationService {

    @Autowired
    protected BackupServerService backupServerService;
    @Autowired
    protected ProjectBackupServerService projectBackupServerService;
    @Autowired
    protected ClusterService clusterService;
    @Autowired
    protected ProjectService projectService;
    @Autowired
    protected BeanOrganizationBackupServerMapper beanOrganizationBackupServerMapper;

    public List<BackupServerDTO> getBackupServer(String organId, String clusterIds, boolean detail) {
        QueryWrapper<BeanOrganizationBackupServer> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        }
        List<BeanOrganizationBackupServer> list = beanOrganizationBackupServerMapper.selectList(wrapper);
        // 根据集群id进行过滤
        if (StringUtils.isNotEmpty(clusterIds)) {
            List<String> clusterIdList = getClusterIdList(clusterIds);
            list = list.stream()
                .filter(obs -> clusterIdList.stream().anyMatch(clusterId -> clusterId.equals(obs.getClusterId())))
                .collect(Collectors.toList());
        }
        List<Integer> idList =
            list.stream().map(BeanOrganizationBackupServer::getBackupServerId).collect(Collectors.toList());

        if(CollectionUtils.isEmpty(idList)){
            return new ArrayList<>();
        }
        // 查询备份服务器
        List<BackupServerDTO> backupServerDTOList = backupServerService.list(idList);
        // 查询备份服务器 组织下分配情况
        if (detail) {
            List<ProjectBackupServerDTO> projectBackupServerDTOList =
                    projectBackupServerService.listByProjectId(organId, null);
            for (BackupServerDTO backupServerDTO : backupServerDTOList) {
                if (projectBackupServerDTOList.stream().anyMatch(projectBackupServerDTO -> projectBackupServerDTO
                        .getBackupServerId().equals(backupServerDTO.getId()))) {
                    backupServerDTO.setUsing(true);
                }
            }
        }

        // 设置集群别名
        Map<String, String> clusterNickNameMap = clusterService.getClusterAliasName();
        return backupServerDTOList.stream().peek(backupServerDTO -> {
            if (StringUtils.isNotEmpty(backupServerDTO.getClusterId())
                    && clusterNickNameMap.containsKey(backupServerDTO.getClusterId())) {
                backupServerDTO.setClusterNickName(clusterNickNameMap.get(backupServerDTO.getClusterId()));
            }
        }).collect(Collectors.toList());
    }

    public void removeBackupServer(String organId, Integer backupServerId, String clusterId) {
        if (StringUtils.isNotEmpty(organId)){
            List<BackupServerDTO> backupServerDTOList = projectService.getBackupServer(organId, null, null, false, false);
            if (!CollectionUtils.isEmpty(backupServerDTOList) && backupServerDTOList.stream()
                    .anyMatch(backupServerDTO -> backupServerId.equals(backupServerDTO.getId()))) {
                throw new BusinessException(ErrorMessage.ORGANIZATION_BACKUP_SERVER_USING);
            }
        }
        QueryWrapper<BeanOrganizationBackupServer> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        }
        if (backupServerId != null) {
            wrapper.eq("backup_server_id", backupServerId);
        }
        if (StringUtils.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        beanOrganizationBackupServerMapper.delete(wrapper);
    }

    protected void allocateBackupServer(OrganizationQuota organizationQuota) {
        // 校验备份服务器是否被使用
        checkBackupServerUsed(organizationQuota.getOrganId(), organizationQuota.getBackupServerDTOList());
        // 删除当前所有绑定关系
        QueryWrapper<BeanOrganizationBackupServer> delete =
            new QueryWrapper<BeanOrganizationBackupServer>().eq("organ_id", organizationQuota.getOrganId());
        beanOrganizationBackupServerMapper.delete(delete);
        if (!CollectionUtils.isEmpty(organizationQuota.getBackupServerDTOList())) {
            for (BackupServerDTO backupServerDTO : organizationQuota.getBackupServerDTOList()) {
                BeanOrganizationBackupServer server = new BeanOrganizationBackupServer();
                server.setOrganId(organizationQuota.getOrganId());
                server.setClusterId(backupServerDTO.getClusterId());
                server.setBackupServerId(backupServerDTO.getId());
                beanOrganizationBackupServerMapper.insert(server);
            }
        }
    }

    public List<String> getClusterIdList(String clusterIds){
        if (clusterIds.endsWith(CommonConstant.COMMA)) {
            StringUtils.removeEnd(clusterIds, CommonConstant.COMMA);
        }
        return Arrays.asList(clusterIds.split(CommonConstant.COMMA));
    }

    public void checkBackupServerUsed(String organId, List<BackupServerDTO> backupServerDTOList) {
        // 查询当前绑定的备份服务器 并确认哪些是会被移除的
        List<BackupServerDTO> usedBackupServerList = getBackupServer(organId, null, false);
        if (!CollectionUtils.isEmpty(backupServerDTOList)) {
            usedBackupServerList = usedBackupServerList.stream()
                .filter(usedBackupServer -> backupServerDTOList.stream()
                    .noneMatch(backupServerDTO -> backupServerDTO.getId().equals(usedBackupServer.getId())))
                .collect(Collectors.toList());
        }
        // 查询项目下的备份服务器绑定情况
        List<ProjectBackupServerDTO> projectBackupServerDTOList =
            projectBackupServerService.listByProjectId(organId, null);
        // 判断会被移除的备份服务器是否存在绑定的备份位置
        for (BackupServerDTO backupServerDTO : usedBackupServerList) {
            boolean flag = projectBackupServerDTOList.stream()
                .anyMatch(backupPositionDTO -> backupPositionDTO.getBackupServerId().equals(backupServerDTO.getId()));
            if (flag) {
                throw new BusinessException(ErrorMessage.ORGANIZATION_BACKUP_SERVER_USING);
            }
        }
    }
}
