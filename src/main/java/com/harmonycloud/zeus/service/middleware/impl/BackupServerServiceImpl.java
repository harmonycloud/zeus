package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.util.StringUtil;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.BackupPositionDTO;
import com.harmonycloud.caas.common.model.BackupServerDTO;
import com.harmonycloud.caas.common.model.ProjectBackupServerDTO;
import com.harmonycloud.caas.common.model.middleware.BackupServerDetailDTO;
import com.harmonycloud.zeus.bean.BeanBackupServer;
import com.harmonycloud.zeus.bean.BeanMiddlewareCluster;
import com.harmonycloud.zeus.dao.BeanBackupServerMapper;
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

    @Override
    public List<BackupServerDTO> list(String clusterId, String keyword) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        if (StringUtil.isNotEmpty(keyword)) {
            wrapper.like("name", "%" + keyword + "%");
        }
        List<BeanBackupServer> serverList = backupServerMapper.selectList(wrapper);
        List<BackupServerDTO> serverDTOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(serverList)) {
            serverDTOList = convert(serverList);
        }
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
        return backupPositionDTOS.stream().map(backupPositionDTO -> {
            BackupServerDTO backupServerDTO = new BackupServerDTO();
            backupServerDTO.setServerDetailList(backupServerDetailService.selectBackupServerDetailDTOSByServerId(backupServerDTO.getId()));
            backupServerDTO.setPositionList(Collections.singletonList(backupPositionDTO));
            return backupServerDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<BackupServerDTO> listProjectEnableBackupServer(String projectId) {
        List<ProjectBackupServerDTO> projectBackupServerDTOS = projectBackupServerService.listByProjectId(projectId);
        List<Integer> backupServerIds = projectBackupServerDTOS.stream().
                map(ProjectBackupServerDTO::getBackupServerId).collect(Collectors.toList());
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        wrapper.notIn("id", backupServerIds);
        return convert(backupServerMapper.selectList(wrapper));
    }

    @Override
    public BeanBackupServer get(Integer id) {
        return backupServerMapper.selectById(id);
    }

    @Override
    public void create(BackupServerDTO backupServerDTO) {
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

    }

    @Override
    public void allocate(Integer id, String clusterId) {
        // TODO 先校验改备份服务器是否已被项目关联，若已关联项目，则需要先解除关联
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
        backupServerDetailService.deleteByServerId(id);
    }

    @Override
    public Map<String, Integer> getBackupServerCountInfo() {
        Map<String, Integer> clusterBackupServerNumMap = new HashMap<>();
        clusterBackupServerNumMap.put("all", getBackupServerCount("dsfasdf"));
        List<BeanMiddlewareCluster> clusters = middlewareClusterService.listClustersByClusterId(null);
        for (BeanMiddlewareCluster cluster : clusters) {
            Integer backupServerCount = getBackupServerCount(cluster.getClusterId());
            if (backupServerCount != 0) {
                clusterBackupServerNumMap.put(cluster.getClusterId(), backupServerCount);
            }
        }
        return clusterBackupServerNumMap;
    }

    @Override
    public Integer getBackupServerCount(String clusterId) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        return backupServerMapper.selectList(wrapper).size();
    }

    // 转换
    private List<BackupServerDTO> convert(List<BeanBackupServer> serverList) {
        return serverList.stream().map(backupServer -> {
            BackupServerDTO backupServerDTO = new BackupServerDTO();
            BeanUtil.copyProperties(backupServer, backupServerDTO);
            backupServerDTO.setPositionList(backupPositionService.selectBackupPositionDTOList(backupServer.getId()));
            backupServerDTO.setServerDetailList(backupServerDetailService.selectBackupServerDetailDTOSByServerId(backupServer.getId()));
            return backupServerDTO;
        }).collect(Collectors.toList());
    }

}
