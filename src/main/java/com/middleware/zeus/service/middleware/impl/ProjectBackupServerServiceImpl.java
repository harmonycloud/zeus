package com.middleware.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.model.ProjectBackupServerDTO;
import com.middleware.zeus.bean.BeanBackupServer;
import com.middleware.zeus.bean.BeanProjectBackupServer;
import com.middleware.zeus.dao.BeanProjectBackupServerMapper;
import com.middleware.zeus.service.middleware.BackupServerService;
import com.middleware.zeus.service.middleware.ProjectBackupServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
            BeanUtil.copyProperties(beanProjectBackupServer, projectBackupServerDTO);
            BeanBackupServer beanBackupServer = backupServerService.get(beanProjectBackupServer.getBackupServerId());
            projectBackupServerDTO.setBackupServerName(beanBackupServer.getName());
            return projectBackupServerDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public void save(String projectId, List<Integer> backupServerIds) {
        // 先删除已有的绑定关系
        deleteByProjectId(projectId);
        if (CollectionUtils.isEmpty(backupServerIds)) {
            return;
        }
        // 重新保存绑定关系
        for (Integer backupServerId : backupServerIds) {
            BeanProjectBackupServer beanProjectBackupServer = new BeanProjectBackupServer();
            beanProjectBackupServer.setProjectId(projectId);
            beanProjectBackupServer.setBackupServerId(backupServerId);
            projectBackupServerMapper.insert(beanProjectBackupServer);
        }
    }

    @Override
    public void deleteByProjectId(String projectId) {
        QueryWrapper<BeanProjectBackupServer> wrapper  = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        projectBackupServerMapper.delete(wrapper);
    }

    @Override
    public void deleteByServerId(Integer serverId) {
        QueryWrapper<BeanProjectBackupServer> wrapper  = new QueryWrapper<>();
        wrapper.eq("backup_server_id", serverId);
        projectBackupServerMapper.delete(wrapper);
    }

}
