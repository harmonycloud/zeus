package com.middleware.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.model.BackupServerDTO;
import com.middleware.zeus.common.model.ProjectBackupServerDTO;
import com.middleware.zeus.bean.BeanProjectBackupServer;
import com.middleware.zeus.dao.BeanProjectBackupServerMapper;
import com.middleware.zeus.service.middleware.BackupServerService;
import com.middleware.zeus.service.middleware.ProjectBackupServerService;
import org.apache.commons.lang3.StringUtils;
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
    public List<ProjectBackupServerDTO> listByProjectId(String organId, String projectId) {
        // 查询项目下备份服务器
        QueryWrapper<BeanProjectBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("organ_id", organId);
        if (StringUtils.isNotEmpty(projectId)) {
            wrapper.eq("project_id", projectId);
        }
        List<BeanProjectBackupServer> projectBackupServerList = projectBackupServerMapper.selectList(wrapper);
        // 封装数据
        return projectBackupServerList.stream().map(beanProjectBackupServer -> {
            ProjectBackupServerDTO projectBackupServerDTO = new ProjectBackupServerDTO();
            BeanUtil.copyProperties(beanProjectBackupServer, projectBackupServerDTO);
            return projectBackupServerDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public void save(String organId, String projectId, List<Integer> backupServerIds) {
        if (CollectionUtils.isEmpty(backupServerIds)) {
            return;
        }
        // 重新保存绑定关系
        for (Integer backupServerId : backupServerIds) {
            BeanProjectBackupServer beanProjectBackupServer = new BeanProjectBackupServer();
            beanProjectBackupServer.setOrganId(organId);
            beanProjectBackupServer.setProjectId(projectId);
            beanProjectBackupServer.setBackupServerId(backupServerId);
            projectBackupServerMapper.insert(beanProjectBackupServer);
        }
    }

    @Override
    public void deleteByProjectId(String organId, String projectId) {
        QueryWrapper<BeanProjectBackupServer> wrapper  = new QueryWrapper<>();
        wrapper.eq("organ_id", organId).eq("project_id", projectId);
        projectBackupServerMapper.delete(wrapper);
    }

    @Override
    public void delete(String organId, String projectId, Integer serverId) {
        QueryWrapper<BeanProjectBackupServer> wrapper  = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(organId)){
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(projectId)){
            wrapper.eq("project_id", projectId);
        }
        if (serverId != null){
            wrapper.eq("backup_server_id", serverId);
        }
        projectBackupServerMapper.delete(wrapper);
    }

}
