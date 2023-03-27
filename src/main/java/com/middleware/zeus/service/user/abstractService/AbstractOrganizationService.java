package com.middleware.zeus.service.user.abstractService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.caas.common.model.ProjectBackupServerDTO;
import com.middleware.caas.common.model.user.OrganizationQuota;
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

    public List<BackupServerDTO> getBackupServer(String organId, String clusterId, boolean detail) {
        QueryWrapper<BeanOrganizationBackupServer> wrapper =
                new QueryWrapper<BeanOrganizationBackupServer>().eq("organ_id", organId);
        List<BeanOrganizationBackupServer> list = beanOrganizationBackupServerMapper.selectList(wrapper);
        List<Integer> idList = list.stream()
                .filter(beanOrganizationBackupServer -> StringUtils.isEmpty(clusterId)
                        || beanOrganizationBackupServer.getClusterId().equals(clusterId))
                .map(BeanOrganizationBackupServer::getBackupServerId).collect(Collectors.toList());

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
        List<BackupServerDTO> backupServerDTOList = projectService.getBackupServer(organId, null, null, false);
        if (!CollectionUtils.isEmpty(backupServerDTOList) && backupServerDTOList.stream()
                .anyMatch(backupServerDTO -> backupServerId.equals(backupServerDTO.getId()))) {
            throw new BusinessException(ErrorMessage.ORGANIZATION_BACKUP_SERVER_USING);
        }
        QueryWrapper<BeanOrganizationBackupServer> wrapper =
                new QueryWrapper<BeanOrganizationBackupServer>().eq("backupServerId", backupServerId);
        beanOrganizationBackupServerMapper.delete(wrapper);
    }

    protected void allocateBackupServer(OrganizationQuota organizationQuota){
        // 删除当前所有绑定关系
        QueryWrapper<BeanOrganizationBackupServer> delete =
                new QueryWrapper<BeanOrganizationBackupServer>().eq("organ_id", organizationQuota.getOrganId());
        beanOrganizationBackupServerMapper.delete(delete);
        for (BackupServerDTO backupServerDTO : organizationQuota.getBackupServerDTOList()) {
            BeanOrganizationBackupServer server = new BeanOrganizationBackupServer();
            server.setOrganId(organizationQuota.getOrganId());
            server.setClusterId(backupServerDTO.getClusterId());
            server.setBackupServerId(backupServerDTO.getId());
            beanOrganizationBackupServerMapper.insert(server);
        }
    }
}
