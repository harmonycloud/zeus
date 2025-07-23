package com.middleware.zeus.service.middleware.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dtflys.forest.utils.StringUtils;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.BackupPositionDTO;
import com.middleware.zeus.common.model.dashboard.BackupPositionDTOList;
import com.middleware.zeus.common.model.middleware.PodInfo;
import com.middleware.zeus.common.model.user.ProjectDto;
import com.middleware.zeus.bean.BeanBackupPosition;
import com.middleware.zeus.bean.BeanBackupServer;
import com.middleware.zeus.bean.BeanBackupServerDetail;
import com.middleware.zeus.bean.BeanMiddlewareBackupName;
import com.middleware.zeus.dao.BeanBackupPositionMapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackup;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupSchedule;
import com.middleware.zeus.integration.cluster.bean.Minio;
import com.middleware.zeus.service.k8s.MiddlewareBackupCRService;
import com.middleware.zeus.service.k8s.MiddlewareBackupScheduleCRDService;
import com.middleware.zeus.service.k8s.NamespaceService;
import com.middleware.zeus.service.k8s.PodService;
import com.middleware.zeus.service.middleware.*;
import com.middleware.zeus.service.user.ProjectService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.NameConstant.POSITION_ID;

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
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private MiddlewareBackupCRService middlewareBackupCrService;
    @Autowired
    private MiddlewareBackupScheduleCRDService middlewareBackupScheduleCrService;

    @Override
    public List<BackupPositionDTO> list(String organId, String projectId, Integer backupServerId, Boolean all) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();

        if (StringUtils.isNotEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(projectId)) {
            wrapper.eq("project_id", projectId);
        }
        if (backupServerId != null) {
            wrapper.eq("backup_server_id", backupServerId);
        }
        List<BeanBackupPosition> beanBackupPositions = backupPositionMapper.selectList(wrapper);
        return convert(beanBackupPositions, all);
    }

    @Override
    public List<BackupPositionDTO> list(String organId, String projectId, String clusterId, String namespace, String middlewareName, String type) {
        List<BackupPositionDTO> backupPositionDTOList = this.list(organId, projectId, null, false);
        boolean openAvailableDomain = namespaceService.isOpenAvailableDomain(clusterId, namespace);
        boolean activeMiddleware = middlewareService.activeActiveMiddlewareCheck(clusterId, namespace, middlewareName, type);

        return backupPositionDTOList.stream().filter(backupPositionDTO -> {
            BeanBackupServer beanBackupServer = backupServerService.get(backupPositionDTO.getBackupServerId());
            if (beanBackupServer == null) {
                return false;
            }
            if (!beanBackupServer.getClusterId().equals(clusterId)) {
                return false;
            }
            backupPositionDTO.setBackupServerName(beanBackupServer.getName());

            // 双活分区的双活中间件可以使用所有备份服务器
            if (openAvailableDomain && activeMiddleware) {
                return true;
            }
            // 非双活分区的中间件只能使用普通备份服务器
            return 1 == beanBackupServer.getType();
        }).collect(Collectors.toList());
    }

    @Override
    public void create(String organId, String projectId, BackupPositionDTOList backupPositionDTOList) {
        // 校验该项目是否已使用该备份服务器创建备份位置
        if (this.getBackupPosition(backupPositionDTOList.getBackupPositionDTOList().get(0).getBackupServerId(), organId, projectId) != null) {
            throw new BusinessException(ErrorMessage.BACKUP_SERVER_ALREADY_USED);
        }
        for (BackupPositionDTO backupPositionDTO: backupPositionDTOList.getBackupPositionDTOList()) {
            BeanBackupPosition backupPosition = new BeanBackupPosition();
            BeanUtil.copyProperties(backupPositionDTO, backupPosition);
            backupPosition.setOrganId(organId);
            backupPosition.setProjectId(projectId);
            backupPositionMapper.insert(backupPosition);
        }
    }

    @Override
    public void update(String organId, String projectId, BackupPositionDTOList backupPositionDTOList) {
        for (BackupPositionDTO backupPositionDTO : backupPositionDTOList.getBackupPositionDTOList()) {
            BeanBackupPosition backupPosition = new BeanBackupPosition();
            BeanUtil.copyProperties(backupPositionDTO, backupPosition);
            backupPositionMapper.updateById(backupPosition);
        }
    }

    @Override
    public void delete(String organId, String projectId, Integer backupServerId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("organ_id", organId).eq("project_id", projectId).eq("backup_server_id", backupServerId);
        List<BeanBackupPosition> beanBackupPositions = backupPositionMapper.selectList(wrapper);
        for (BeanBackupPosition beanBackupPosition : beanBackupPositions) {
            Integer count = getBackupPositionBindCount(backupServerId, beanBackupPosition.getId());
            if (count > 0) {
                throw new BusinessException(ErrorMessage.FAILED_TO_DELETE_BACKUP_POSITION);
            }
        }
        // 检查备份位置是否已被备份任务使用
        backupPositionMapper.deleteBatchIds(beanBackupPositions.stream().map(BeanBackupPosition::getId).collect(Collectors.toList()));
    }

    @Override
    public void deleteByBackupServer(Integer backupServerId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
        backupPositionMapper.delete(wrapper);
    }

    @Override
    public void deleteByProjectId(String organId, String projectId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("organ_id", organId).eq("project_id", projectId);
        backupPositionMapper.delete(wrapper);
    }

    public BeanBackupPosition getBackupPosition(Integer backupServerId, String organId, String projectId) {
        QueryWrapper<BeanBackupPosition> wrapper = new QueryWrapper<>();
        wrapper.eq("backup_server_id", backupServerId);
        wrapper.eq("organ_id", organId);
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

    @Override
    public BeanBackupServer getBackupServer(Integer positionId) {
        BeanBackupPosition backupPosition = getBackupPosition(positionId);
        BeanBackupServer beanBackupServer = backupServerService.get(backupPosition.getBackupServerId());
        return beanBackupServer;
    }

    // 转换数据类型
    private List<BackupPositionDTO> convert(List<BeanBackupPosition> beanBackupPositions, Boolean all) {
        List<BackupPositionDTO> positionList = new ArrayList<>();
        if (CollectionUtils.isEmpty(beanBackupPositions)) {
            return positionList;
        }
        // 查询项目列表
        List<ProjectDto> projectDtoList = projectService.list(null);
        Map<String, ProjectDto> projectNameMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(projectDtoList)) {
            projectNameMap = projectDtoList.stream().collect(Collectors.toMap(ProjectDto::getProjectId, Function.identity()));
        }
        
        Map<Integer, List<BeanBackupPosition>> postionMap = beanBackupPositions.stream().collect(Collectors.groupingBy(BeanBackupPosition::getBackupServerId));
        for (List<BeanBackupPosition> positions : postionMap.values()) {
            for (int i = 0; i < positions.size(); i++) {
                if (i > 0 && !all) {
                    continue;
                }
                BackupPositionDTO backupPositionDTO = new BackupPositionDTO();
                BeanBackupPosition beanBackupPosition = positions.get(i);
                BeanUtil.copyProperties(beanBackupPosition, backupPositionDTO);
                if (projectNameMap.get(beanBackupPosition.getProjectId()) != null) {
                    // 设置项目名称
                    backupPositionDTO.setProjectName(projectNameMap.get(beanBackupPosition.getProjectId()).getAliasName());
                    positionList.add(backupPositionDTO);
                }
            }
        }
        setBackupPositionBindCount(positionList);
        return positionList;
    }

    public Integer getBackupPositionBindCount(Integer backupServerId, Integer positionId){
        // 根据backupServerId确认所属集群
        BeanBackupServer beanBackupServer = backupServerService.get(backupServerId);

        String clusterId = beanBackupServer.getClusterId();

        Map<String, String> labels = new HashMap<>(1);
        labels.put(POSITION_ID, String.valueOf(positionId));
        List<MiddlewareBackup> middlewareBackupList = middlewareBackupCrService.list(clusterId, null, labels);
        List<MiddlewareBackupSchedule> middlewareBackupScheduleList = middlewareBackupScheduleCrService.listByLabels(clusterId, null, labels);

        int count = 0;
        if (!CollectionUtils.isEmpty(middlewareBackupList)){
            count += middlewareBackupList.size();
        }
        if (!CollectionUtils.isEmpty(middlewareBackupScheduleList)){
            count += middlewareBackupScheduleList.size();
        }
        return count;
    }

    public void setBackupPositionBindCount(List<BackupPositionDTO> backupPositionDTOList){
        // 根据备份服务器id进行group，变相收缩相同集群
        Map<Integer, List<BackupPositionDTO>> map =  backupPositionDTOList.stream().collect(Collectors.groupingBy(BackupPositionDTO::getBackupServerId));

        // 被备份数据进行临时缓存，避免反复查询
        Map<String, List<MiddlewareBackup>> middlewareBackupCache = new HashMap<>();
        Map<String, List<MiddlewareBackupSchedule>> middlewareBackupScheduleCache = new HashMap<>();

        for (Integer backupServerId : map.keySet()) {
            // 获取备份服务器信息，通过此方法获取集群id
            BeanBackupServer beanBackupServer = backupServerService.get(backupServerId);

            List<MiddlewareBackup> middlewareBackupList;
            List<MiddlewareBackupSchedule> middlewareBackupScheduleList;
            // 根据集群情况，缓存查询到的备份和周期备份数据
            String clusterId = beanBackupServer.getClusterId();
            if (middlewareBackupCache.containsKey(clusterId)) {
                middlewareBackupList = middlewareBackupCache.get(beanBackupServer.getClusterId());
            } else {
                middlewareBackupList = middlewareBackupCrService.list(clusterId, null, null);
                middlewareBackupCache.put(clusterId, middlewareBackupList);
            }
            if (middlewareBackupScheduleCache.containsKey(clusterId)) {
                middlewareBackupScheduleList =  middlewareBackupScheduleCache.get(beanBackupServer.getClusterId());
            } else {
                middlewareBackupScheduleList = middlewareBackupScheduleCrService.listByLabels(clusterId, null, null);
                middlewareBackupScheduleCache.put(clusterId, middlewareBackupScheduleList);
            }

            for (BackupPositionDTO backupPositionDTO : map.get(backupServerId)) {
                int count = 0;
                // 计算备份任务的数量
                count += (int)middlewareBackupList.stream()
                    .filter(middlewareBackup -> middlewareBackup.getMetadata().getLabels() != null
                        && middlewareBackup.getMetadata().getLabels().get(POSITION_ID) != null
                        && middlewareBackup.getMetadata().getLabels().get(POSITION_ID)
                            .equals(String.valueOf(backupPositionDTO.getId())))
                    .count();
                // 计算周期备份任务的数量
                count += (int)middlewareBackupScheduleList.stream()
                    .filter(middlewareBackupSchedule -> middlewareBackupSchedule.getMetadata().getLabels() != null
                        && middlewareBackupSchedule.getMetadata().getLabels().get(POSITION_ID) != null
                        && middlewareBackupSchedule.getMetadata().getLabels().get(POSITION_ID)
                            .equals(String.valueOf(backupPositionDTO.getId())))
                    .count();

                backupPositionDTO.setBackupTaskNum(count);
            }
            
        }
    }


}
