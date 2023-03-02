package com.middleware.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dtflys.forest.utils.StringUtils;
import com.github.pagehelper.util.StringUtil;
import com.middleware.caas.common.enums.BackupServerTypeEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.BackupPositionDTO;
import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.caas.common.model.ProjectBackupServerDTO;
import com.middleware.caas.common.model.middleware.BackupServerDetailDTO;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.bean.BeanBackupPosition;
import com.middleware.zeus.bean.BeanBackupServer;
import com.middleware.zeus.bean.BeanBackupServerDetail;
import com.middleware.zeus.bean.BeanMiddlewareBackupName;
import com.middleware.zeus.dao.BeanBackupServerMapper;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.MiddlewareClusterService;
import com.middleware.zeus.service.middleware.*;
import com.middleware.zeus.service.middleware.*;
import com.middleware.zeus.util.MinioUtils;
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
    @Autowired
    private MiddlewareBackupNameService middlewareBackupNameService;

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
            // 查询使用该备份位置的备份记录数
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
        if (checkAddressExists(backupServerDTO)) {
            throw new BusinessException(ErrorMessage.SERVER_ADDRESS_ALREADY_EXISTS);
        }
        // 校验备份服务器用户名和密码
        checkServerAuthorization(backupServerDTO.getServerDetailList());

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
        // 校验备份服务器用户名和密码
        checkServerAuthorization(backupServerDTO.getServerDetailList());
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
        // 校验服务器关联的备份位置是否已被使用
        backupServerDeletionCheck(id);
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
     * 校验备份地址是否已存在
     * @param backupServerDTO
     * @return
     */
    private boolean checkAddressExists(BackupServerDTO backupServerDTO) {
        List<BackupServerDetailDTO> serverDetailList = backupServerDTO.getServerDetailList();
        for (BackupServerDetailDTO serverDetail : serverDetailList) {
            List<BeanBackupServerDetail> serverDetails = backupServerDetailService.findByAddress(serverDetail.getProtocol(),
                    serverDetail.getHost(), serverDetail.getPort(), backupServerDTO.getType());
            if(!CollectionUtils.isEmpty(serverDetails)){
                return true;
            }
        }
        return false;
    }

    /**
     * 校验服务器用户名或密码
     * @param servers
     */
    private void checkServerAuthorization(List<BackupServerDetailDTO> servers) {
        for (BackupServerDetailDTO server : servers) {
            int code = MinioUtils.checkConnection(server.getProtocol(), server.getHost(), server.getPort(), server.getUsername(), server.getPassword());
            String portStr = StringUtils.isNotEmpty(server.getPort()) ? ":" + server.getPort() : "";
            String url = server.getProtocol() + "://" + server.getHost() + portStr;
            if (code == 2) {
                throw new BusinessException(ErrorMessage.AUTHORIZATION_FAILED, "服务器: " + url + " 用户名或密码错误");
            }
        }
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
                    // todo
                    clusterDTO = clusterService.findById(backupServerDTO.getClusterId());
                    backupServerDTO.setClusterNickName(clusterDTO.getNickname());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 检查服务器关联的地址是否已被备份任务使用
     */
    private void backupServerDeletionCheck(Integer backupServerId){
        List<BeanBackupPosition> beanBackupPositions = backupPositionService.listByBackupServerId(backupServerId);
        for (BeanBackupPosition position : beanBackupPositions) {
            List<BeanMiddlewareBackupName> middlewareBackupNames = middlewareBackupNameService.listByPositionId(position.getId());
            if(!CollectionUtils.isEmpty(middlewareBackupNames)){
                throw new BusinessException(ErrorMessage.FAILED_TO_DELETE_BACKUP_SERVER);
            }
        }
    }

}
