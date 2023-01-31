package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dtflys.forest.utils.StringUtils;
import com.github.pagehelper.util.StringUtil;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.BackupPositionDTO;
import com.harmonycloud.caas.common.model.BackupServerDTO;
import com.harmonycloud.caas.common.model.ProjectBackupServerDTO;
import com.harmonycloud.caas.common.model.middleware.BackupServerDetailDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.bean.BeanBackupServer;
import com.harmonycloud.zeus.dao.BeanBackupServerMapper;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.middleware.BackupPositionService;
import com.harmonycloud.zeus.service.middleware.BackupServerDetailService;
import com.harmonycloud.zeus.service.middleware.BackupServerService;
import com.harmonycloud.zeus.service.middleware.ProjectBackupServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2023/1/9 2:29 下午
 */
@Service
@Slf4j
public class BackupServerServiceImpl implements BackupServerService {

    @Autowired
    private BeanBackupServerMapper backupServerMapper;
    @Autowired
    private BackupServerDetailService backupServerDetailService;
    @Autowired
    private BackupPositionService backupPositionService;
    @Autowired
    private ProjectBackupServerService projectBackupServerService;
    @Autowired
    private MiddlewareClusterService middlewareClusterService;
    @Autowired
    private ClusterService clusterService;

    @Override
    public List<BackupServerDTO> list(List<String> clusterIds, String keyword, Boolean withDetail) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(clusterIds)) {
            wrapper.in("cluster_id", clusterIds);
        }
        if (StringUtil.isNotEmpty(keyword)) {
            wrapper.like("name", "%" + keyword + "%");
        }
        List<BeanBackupServer> serverList = backupServerMapper.selectList(wrapper);
        List<BackupServerDTO> serverDTOList = new ArrayList<>();
        // TODO 待优化 withDetail为false时不需要查询备份服务器详细信息
        if (!CollectionUtils.isEmpty(serverList)) {
            serverDTOList = addDetail(serverList, null);
        }
        // 添加集群别名
        serverDTOList.forEach(backupServerDTO -> {
            if (StringUtils.isNotEmpty(backupServerDTO.getClusterId())) {
                MiddlewareClusterDTO clusterDTO = clusterService.findById(backupServerDTO.getClusterId());
                backupServerDTO.setClusterNickName(clusterDTO.getNickname());
            }
        });
        return serverDTOList;
    }

    /**
     * 查询项目备份位置列表
     *
     * @param projectId
     * @return
     */
    @Override
    public List<BackupServerDTO> listBackupPosition(String projectId) {
        List<BackupPositionDTO> backupPositionDTOS = backupPositionService.selectBackupPositionDTOList(projectId);
        List<BackupServerDTO> backupServerDTOS = new ArrayList<>();
        for (BackupPositionDTO backupPositionDTO : backupPositionDTOS) {
            List<BackupServerDetailDTO> backupServerDetailDTOS = backupServerDetailService.listBackupServerDetailDTOS(backupPositionDTO.getId());
            if (!CollectionUtils.isEmpty(backupPositionDTOS)) {
                BackupServerDTO backupServerDTO = new BackupServerDTO();
                backupServerDTO.setServerDetailList(backupServerDetailDTOS);
                backupServerDTO.setPositionList(Collections.singletonList(backupPositionDTO));
                backupServerDTOS.add(backupServerDTO);
            }
        }
        return backupServerDTOS;
    }

    @Override
    public List<BackupServerDTO> listProjectBackupServer(String projectId) {
        List<ProjectBackupServerDTO> projectBackupServerDTOS = projectBackupServerService.listByProjectId(projectId);
        List<Integer> backupServerIds = projectBackupServerDTOS.stream().
                map(ProjectBackupServerDTO::getBackupServerId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(backupServerIds)) {
            return Collections.emptyList();
        }
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        wrapper.in("id", backupServerIds);
        return addDetail(backupServerMapper.selectList(wrapper), projectId);
    }

    @Override
    public BeanBackupServer get(Integer id) {
        return backupServerMapper.selectById(id);
    }

    @Override
    public void create(BackupServerDTO backupServerDTO) {
        // TODO 检查名称是否已存在
        BeanBackupServer backupServer = new BeanBackupServer();
        backupServer.setName(backupServerDTO.getName());
        backupServer.setType(backupServerDTO.getType());
        backupServer.setCreateTime(new Date());
        backupServerMapper.insert(backupServer);

        Integer serverId = backupServer.getId();
        List<BackupServerDetailDTO> serverDetailList = backupServerDTO.getServerDetailList();
        if (!CollectionUtils.isEmpty(serverDetailList)) {
            backupServerDetailService.create(serverId, serverDetailList);
        } else {
            throw new BusinessException(ErrorMessage.PARAMETER_NOT_COMPLETE);
        }
    }

    @Override
    public void update(BackupServerDTO backupServerDTO) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("id", backupServerDTO.getId());
        BeanBackupServer beanBackupServer = new BeanBackupServer();
        BeanUtil.copyProperties(backupServerDTO, beanBackupServer);
        backupServerMapper.updateById(beanBackupServer);
        List<BackupServerDetailDTO> serverDetailList = backupServerDTO.getServerDetailList();
        backupServerDetailService.update(serverDetailList);
    }

    @Override
    public void allocate(Integer id, String clusterId) {
        // TODO 先校验该备份服务器是否已被项目关联，若已关联项目，则需要先解除关联
        BeanBackupServer beanBackupServer = get(id);
        beanBackupServer.setClusterId(clusterId);
        backupServerMapper.updateById(beanBackupServer);
    }

    @Override
    public void delete(Integer id) {
        // TODO 删除备份服务器之前需要先是否有校验关联的备份位置
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        backupServerMapper.delete(wrapper);
        // 删除备份服务器详细信息
        backupServerDetailService.deleteByServerId(id);
        // TODO 删除备份位置等，需要软删除
    }

    @Override
    public List<Map<String, String>> getBackupServerCountInfo() {
        List<Map<String, String>> groupList = new ArrayList<>();
        Map<String, String> clusterBackupServerNumMap = new HashMap<>();
        clusterBackupServerNumMap.put("clusterId", "");
        clusterBackupServerNumMap.put("clusterName", "全部");
        clusterBackupServerNumMap.put("clusterServerCount", getBackupServerCount(null).toString());
        groupList.add(clusterBackupServerNumMap);
        List<MiddlewareClusterDTO> clusterDTOS = middlewareClusterService.listClusterDtos();
        for (MiddlewareClusterDTO cluster : clusterDTOS) {
            Integer backupServerCount = getBackupServerCount(cluster.getId());
            if (backupServerCount != 0) {
                clusterBackupServerNumMap = new HashMap<>();
                clusterBackupServerNumMap.put("clusterId", cluster.getId());
                clusterBackupServerNumMap.put("clusterName", cluster.getNickname());
                clusterBackupServerNumMap.put("clusterServerCount", backupServerCount.toString());
                groupList.add(clusterBackupServerNumMap);
            }
        }
        return groupList;
    }

    @Override
    public Integer getBackupServerCount(String clusterId) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        return backupServerMapper.selectList(wrapper).size();
    }

    // 添加备份位置和备份服务器信息
    private List<BackupServerDTO> addDetail(List<BeanBackupServer> serverList, String projectId) {
        return serverList.stream().map(backupServer -> {
            BackupServerDTO backupServerDTO = new BackupServerDTO();
            BeanUtil.copyProperties(backupServer, backupServerDTO);
            backupServerDTO.setPositionList(backupPositionService.selectBackupPositionDTOList(backupServer.getId(), projectId));
            backupServerDTO.setServerDetailList(backupServerDetailService.listBackupServerDetailDTOS(backupServer.getId()));
            return backupServerDTO;
        }).collect(Collectors.toList());
    }

}
