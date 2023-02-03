package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dtflys.forest.utils.StringUtils;
import com.github.pagehelper.util.StringUtil;
import com.harmonycloud.caas.common.enums.BackupServerTypeEnum;
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
        if (CollectionUtils.isEmpty(serverList)) {
            return Collections.emptyList();
        }
        // TODO 待优化 withDetail为false时不需要查询备份服务器详细信息
        List<BackupServerDTO> serverDTOList = convertToBackupServerDTO(serverList, null, withDetail);
        // 添加集群别名
        addClusterNickName(serverDTOList);
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
        return convertToBackupServerDTO(backupServerMapper.selectList(wrapper), projectId, true);
    }

    @Override
    public BeanBackupServer get(Integer id) {
        return backupServerMapper.selectById(id);
    }

    @Override
    public void create(BackupServerDTO backupServerDTO) {
        if (checkNameExists(backupServerDTO.getName())) {
            throw new BusinessException(ErrorMessage.SERVER_NAME_ALREADY_EXISTS);
        }
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
        BeanBackupServer beanBackupServer = get(id);
        beanBackupServer.setClusterId(clusterId);
        backupServerMapper.updateById(beanBackupServer);
    }

    /**
     * 删除备份服务器和集群的关联关系
     * @param clusterId
     */
    @Override
    public void unbinding(String clusterId) {
        List<BeanBackupServer> beanBackupServers = listByClusterId(clusterId);
        for (BeanBackupServer beanBackupServer : beanBackupServers) {
            beanBackupServer.setClusterId("");
            backupServerMapper.updateById(beanBackupServer);
        }
    }

    @Override
    public void delete(Integer id) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        backupServerMapper.delete(wrapper);
        // 删除备份服务器详细信息
        backupServerDetailService.deleteByServerId(id);
        // 删除项目备份服务器关联信息
        projectBackupServerService.deleteByServerId(id);
        // 删除备份位置
        backupPositionService.deleteByBackupServer(id);
    }

    @Override
    public List<Map<String, String>> getBackupServerCountInfo() {
        List<Map<String, String>> groupList = new ArrayList<>();
        Map<String, String> clusterBackupServerNumMap = new HashMap<>();
        clusterBackupServerNumMap.put("clusterId", "");
        clusterBackupServerNumMap.put("clusterName", "全部");
        clusterBackupServerNumMap.put("clusterServerCount", Integer.toString(listByClusterId(null).size()));
        groupList.add(clusterBackupServerNumMap);
        List<MiddlewareClusterDTO> clusterDTOS = middlewareClusterService.listClusterDtos();
        for (MiddlewareClusterDTO cluster : clusterDTOS) {
            int backupServerCount = listByClusterId(cluster.getId()).size();
            if (backupServerCount != 0) {
                clusterBackupServerNumMap = new HashMap<>();
                clusterBackupServerNumMap.put("clusterId", cluster.getId());
                clusterBackupServerNumMap.put("clusterName", cluster.getNickname());
                clusterBackupServerNumMap.put("clusterServerCount", Integer.toString(backupServerCount));
                groupList.add(clusterBackupServerNumMap);
            }
        }
        return groupList;
    }

    @Override
    public List<BeanBackupServer> listByClusterId(String clusterId) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        return backupServerMapper.selectList(wrapper);
    }

    /**
     * 添加备份位置和备份服务器信息
     * @param serverList
     * @param projectId
     * @return
     */
    private List<BackupServerDTO> convertToBackupServerDTO(List<BeanBackupServer> serverList, String projectId, Boolean withDetail) {
        return serverList.stream().map(backupServer -> {
            BackupServerDTO backupServerDTO = new BackupServerDTO();
            BeanUtil.copyProperties(backupServer, backupServerDTO);
            if (withDetail) {
                backupServerDTO.setPositionList(backupPositionService.selectBackupPositionDTOList(backupServer.getId(), projectId));
                backupServerDTO.setServerDetailList(backupServerDetailService.listBackupServerDetailDTOS(backupServer.getId()));
                backupServerDTO.setServerType(getServerType(backupServerDTO.getServerDetailList()));
            }
            return backupServerDTO;
        }).collect(Collectors.toList());
    }

    /**
     * 获取全部备份服务器介质类型
     * @param detailDTOS
     * @return
     */
    private String getServerType(List<BackupServerDetailDTO> detailDTOS) {
        String str = Arrays.toString(detailDTOS.stream().
                map(backupServerDetailDTO -> BackupServerTypeEnum.findByType(backupServerDetailDTO.getType())).distinct().toArray());
        return str.substring(1, str.length() - 1);
    }

    /**
     * 检查备份服务器名称是否已存在
     * @param name
     * @return 存在则返回true，否则返回false
     */
    public boolean checkNameExists(String name) {
        QueryWrapper<BeanBackupServer> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        List<BeanBackupServer> beanBackupServers = backupServerMapper.selectList(wrapper);
        return !CollectionUtils.isEmpty(beanBackupServers);
    }

    /**
     * 添加集群别名
     * @param servers
     */
    private void addClusterNickName(List<BackupServerDTO> servers) {
        servers.forEach(backupServerDTO -> {
            String clusterId = backupServerDTO.getClusterId();
            if (StringUtils.isNotEmpty(clusterId)) {
                MiddlewareClusterDTO clusterDTO;
                try {
                    clusterDTO = clusterService.findById(backupServerDTO.getClusterId());
                    backupServerDTO.setClusterNickName(clusterDTO.getNickname());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
