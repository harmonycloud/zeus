package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.ProjectBackupServerDTO;
import com.harmonycloud.zeus.bean.BeanBackupServer;
import com.harmonycloud.zeus.bean.BeanProjectBackupServer;
import com.harmonycloud.zeus.dao.BeanProjectBackupServerMapper;
import com.harmonycloud.zeus.service.middleware.BackupServerService;
import com.harmonycloud.zeus.service.middleware.ProjectBackupServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2023/1/11 3:04 下午
 */
@Service
@Transactional
public class ProjectBackupServerServiceImpl implements ProjectBackupServerService {

    @Autowired
    private BeanProjectBackupServerMapper projectBackupServerMapper;
    @Autowired
    private BackupServerService backupServerService;

    @Override
    public List<BeanProjectBackupServer> listByBackupServerId(Integer serverId) {
        QueryWrapper<BeanProjectBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("server_id", serverId);
        return projectBackupServerMapper.selectList(wrapper);
    }

    @Override
    public List<ProjectBackupServerDTO> listByProjectId(String projectId) {
        QueryWrapper<BeanProjectBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        List<BeanProjectBackupServer> backupServerList = projectBackupServerMapper.selectList(wrapper);
        return backupServerList.stream().map(beanProjectBackupServer -> {
            ProjectBackupServerDTO projectBackupServerDTO = new ProjectBackupServerDTO();
            BeanBackupServer beanBackupServer = backupServerService.get(beanProjectBackupServer.getBackupServerId());
            projectBackupServerDTO.setBackupServerName(beanBackupServer.getName());
            return projectBackupServerDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public void save(String projectId, List<ProjectBackupServerDTO> projectBackupServerDTOList) {
        // 先删除已有的绑定关系
        deleteByProjectId(projectId);
        // 重新保存绑定关系
        for (ProjectBackupServerDTO projectBackupServerDTO : projectBackupServerDTOList) {
            BeanProjectBackupServer beanProjectBackupServer = new BeanProjectBackupServer();
            beanProjectBackupServer.setProjectId(projectId);
            beanProjectBackupServer.setBackupServerId(projectBackupServerDTO.getBackupServerId());
            projectBackupServerMapper.insert(beanProjectBackupServer);
        }
    }

    @Override
    public void deleteByProjectId(String projectId) {
        QueryWrapper<BeanProjectBackupServer> wrapper  = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        projectBackupServerMapper.delete(wrapper);
    }
}
