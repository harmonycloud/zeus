package com.middleware.zeus.service.user.abstractService;

import com.middleware.caas.common.enums.ComponentsEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.BackupPositionDTO;
import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.caas.common.model.ProjectBackupServerDTO;
import com.middleware.caas.common.model.middleware.MiddlewareResourceInfo;
import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.caas.common.model.middleware.ProjectMiddlewareResourceInfo;
import com.middleware.caas.common.model.user.ProjectDto;
import com.middleware.caas.common.model.user.ProjectQuota;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.bean.BeanClusterMiddlewareInfo;
import com.middleware.zeus.bean.BeanMiddlewareInfo;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.*;
import com.middleware.zeus.service.user.PlatformQuotaService;
import com.middleware.zeus.service.user.RoleService;
import com.middleware.zeus.service.user.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.middleware.caas.common.constants.user.UserConstant.USERNAME;

/**
 * @author xutianhong
 * @Date 2023/3/26 11:43 上午
 */
public abstract class AbstractProjectService {

    /**
     * 查询project列表
     * @param organId 组织id
     *
     * @return List<ProjectDto>
     */
    protected abstract List<ProjectDto> list(String organId);

    /**
     * 查询项目下分区
     * @param organId 组织id
     * @param projectId 项目id
     *
     * @return List<Namespace>
     */
    protected abstract List<Namespace> getNamespace(String organId, String projectId);

    @Autowired
    protected ResourceQuotaService resourceQuotaService;
    @Autowired
    protected ProjectBackupServerService projectBackupServerService;
    @Autowired
    protected BackupServerService backupServerService;
    @Autowired
    protected BackupPositionService backupPositionService;
    @Autowired
    protected ClusterService clusterService;
    @Autowired
    protected UserService userService;
    @Autowired
    protected MiddlewareInfoService middlewareInfoService;
    @Autowired
    protected ClusterMiddlewareInfoService clusterMiddlewareInfoService;
    @Autowired
    protected ClusterComponentService clusterComponentService;
    @Autowired
    protected MiddlewareCRService middlewareCrService;
    @Autowired
    protected NamespaceService namespaceService;
    @Autowired
    protected RoleService roleService;

    public List<BackupServerDTO> getBackupServer(String organId, String projectId, String clusterId, boolean detail, boolean position) {
        List<ProjectBackupServerDTO> projectBackupServerDTOList =
                projectBackupServerService.listByProjectId(organId, projectId);
        List<Integer> idList = projectBackupServerDTOList.stream().map(ProjectBackupServerDTO::getBackupServerId)
                .collect(Collectors.toList());
        // 判空
        if (CollectionUtils.isEmpty(idList)){
            return new ArrayList<>();
        }
        // 查询备份服务器信息
        List<BackupServerDTO> backupServerDTOList = backupServerService.list(idList);
        
        // 根据集群id过滤
        if (StringUtils.isNotEmpty(clusterId)) {
            backupServerDTOList = backupServerDTOList.stream()
                    .filter(backupServerDTO -> StringUtils.isNotEmpty(backupServerDTO.getClusterId())
                            && backupServerDTO.getClusterId().equals(clusterId))
                    .collect(Collectors.toList());
        }
        
        // 获取备份位置
        if (position) {
            List<BackupPositionDTO> backupPositionDTOList = backupPositionService.list(organId, projectId, null);
            Map<Integer, List<BackupPositionDTO>> backupPositionMap =
                backupPositionDTOList.stream().collect(Collectors.groupingBy(BackupPositionDTO::getBackupServerId));
            for (BackupServerDTO backupServerDTO : backupServerDTOList) {
                backupServerDTO.setPositionList(backupPositionMap.get(backupServerDTO.getId()));
            }
        }
        
        // 查询 备份服务器使用情况
        if (detail) {
            List<BackupPositionDTO> backupPositionDTOList = backupPositionService.list(organId, projectId, null);
            for (BackupServerDTO backupServerDTO : backupServerDTOList) {
                if (backupPositionDTOList.stream().anyMatch(
                        backupPositionDTO -> backupPositionDTO.getBackupServerId().equals(backupServerDTO.getId()))) {
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

    public void allocateBackupServer(ProjectQuota projectQuota){
        // 校验备份服务器是否已被使用
        checkPositionUsed(projectQuota.getOrganId(), projectQuota.getProjectId(), projectQuota.getBackupServerDTOList());
        projectBackupServerService.delete(projectQuota.getOrganId(), projectQuota.getProjectId(), null);
        if (!CollectionUtils.isEmpty(projectQuota.getBackupServerDTOList())) {
            // 删除当前所有绑定关系
            projectBackupServerService.save(projectQuota.getOrganId(), projectQuota.getProjectId(), projectQuota
                    .getBackupServerDTOList().stream().map(BackupServerDTO::getId).collect(Collectors.toList()));
        }
    }

    public void removeBackupServer(String organId, String projectId, Integer backupServerId, String clusterId) {
        List<BackupPositionDTO> backupPositionDTOList = backupPositionService.list(organId, projectId, backupServerId);
        if(!CollectionUtils.isEmpty(backupPositionDTOList)){
            throw new BusinessException(ErrorMessage.PROJECT_BACKUP_SERVER_USING);
        }
        projectBackupServerService.delete(organId, projectId, backupServerId);
    }


    public List<ProjectMiddlewareResourceInfo> middlewareResource(String organId, String projectId) throws Exception {
        List<Namespace> nsList = getNamespace(organId, projectId);
        // 获取集群
        Set<String> clusterIdSet = new HashSet<>();
        nsList.forEach(ns -> clusterIdSet.add(ns.getClusterId()));
        // 获取集群下已安装中间件并集
        Set<String> mwTypeSet = new HashSet<>();
        for (String clusterId : clusterIdSet) {
            mwTypeSet.addAll(clusterMiddlewareInfoService.list(clusterId, true).stream()
                    .map(BeanClusterMiddlewareInfo::getChartName).collect(Collectors.toList()));
        }
        // 查询用户角色项目权限
        String username =
                JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue().getString(USERNAME);
        UserDto userDto = userService.getUserDto(username);
        Map<String, String> power = new HashMap<>();
        if (!userDto.getIsAdmin()
                && userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getProjectId().equals(projectId))) {
            power
                    .putAll(userDto.getUserRoleList().stream().filter(userRole -> userRole.getProjectId().equals(projectId))
                            .collect(Collectors.toList()).get(0).getPower());
        }
        // 过滤获取拥有权限的中间件
        // todo 确认开启观云台时逻辑是否自洽
        if (!CollectionUtils.isEmpty(power)) {
            mwTypeSet = mwTypeSet.stream().filter(
                    mwType -> power.keySet().stream().anyMatch(key -> !"0000".equals(power.get(key)) && mwType.equals(key)))
                    .collect(Collectors.toSet());
        }
        // 查询数据
        List<MiddlewareResourceInfo> all = new ArrayList<>();
        for (String clusterId : clusterIdSet) {
            all.addAll(clusterService.getMwResource(clusterId));
        }
        // 根据分区过滤
        all = all.stream().filter(middlewareResourceInfo -> nsList.stream().anyMatch(
                ns -> ns.getName().equals(middlewareResourceInfo.getNamespace())))
                .collect(Collectors.toList());
        // 获取image.path
        Map<String,
                String> middlewareImagePathMap = middlewareInfoService.list(false).stream()
                .filter(beanMiddlewareInfo -> beanMiddlewareInfo.getImagePath() != null)
                .collect(Collectors.toMap(BeanMiddlewareInfo::getChartName, BeanMiddlewareInfo::getImagePath));
        // 封装数据
        Map<String, List<MiddlewareResourceInfo>> map =
                all.stream().collect(Collectors.groupingBy(MiddlewareResourceInfo::getType));
        List<ProjectMiddlewareResourceInfo> infoList = new ArrayList<>();
        for (String mwType : mwTypeSet) {
            ProjectMiddlewareResourceInfo projectMiddlewareResourceInfo = new ProjectMiddlewareResourceInfo()
                    .setType(mwType).setAliasName(MiddlewareOfficialNameEnum.findByChartName(mwType))
                    .setMiddlewareResourceInfoList(map.getOrDefault(mwType, null))
                    .setImagePath(middlewareImagePathMap.getOrDefault(mwType, null));
            infoList.add(projectMiddlewareResourceInfo);
        }
        infoList.sort(Comparator.comparing(ProjectMiddlewareResourceInfo::getType));
        return infoList;
    }

    public List<ProjectDto> getMiddlewareCount(String organId, String projectId) {
        // 获取项目下分区列表
        List<Namespace> nsList = getNamespace(organId, projectId);

        String username = CurrentUserRepository.getUser().getUsername();
        // 查询用户信息
        UserDto userDto = userService.getUserDto(username);
        if (!userDto.getIsAdmin()) {
            List<ProjectDto> projectDtoList = list(organId);
            // 获取该用户所属的各个项目
            List<ProjectDto> filteredProjectList = projectDtoList.stream()
                .filter(projectDto -> userDto.getUserRoleList().stream()
                    .anyMatch(userRole -> StringUtils.isNotEmpty(userRole.getOrganId())
                        && userRole.getOrganId().equals(organId)
                        && ((userRole.getRoleId() != null
                            && userRole.getRoleId().equals(roleService.getOrganManagerRoleId().getId()))
                            || (StringUtils.isNotEmpty(userRole.getProjectId())
                                && userRole.getProjectId().equals(projectDto.getProjectId())))))
                .collect(Collectors.toList());
            // 获取n个项目的分区
            if (!CollectionUtils.isEmpty(filteredProjectList)) {
                nsList = nsList.stream()
                    .filter(ns -> filteredProjectList.stream()
                        .anyMatch(projectDto -> projectDto.getProjectId().equals(ns.getProjectId())))
                    .collect(Collectors.toList());
            }
        }
        // 分区为空
        if (CollectionUtils.isEmpty(nsList)) {
            return null;
        }
        Set<String> clusterIdList = nsList.stream().map(Namespace::getClusterId).collect(Collectors.toSet());
        Map<String, List<MiddlewareCR>> middlewareCrListMap = new HashMap<>();
        for (String clusterId : clusterIdList) {
            if (!clusterComponentService.checkInstalled(clusterId, ComponentsEnum.MIDDLEWARE_CONTROLLER.getName())) {
                continue;
            }
            List<MiddlewareCR> middlewareCrList = middlewareCrService.listCR(clusterId, null, null);
            middlewareCrListMap.put(clusterId, middlewareCrList);
        }
        Map<String, List<Namespace>> projectNamespaceListMap =
                nsList.stream().collect(Collectors.groupingBy(Namespace::getProjectId));
        List<ProjectDto> projectDtoList = new ArrayList<>();
        for (String key : projectNamespaceListMap.keySet()) {
            ProjectDto projectDto = new ProjectDto();
            projectDto.setProjectId(key);
            int count = 0;
            for (String clusterId : middlewareCrListMap.keySet()) {
                for (MiddlewareCR middlewareCr : middlewareCrListMap.get(clusterId)) {
                    if (projectNamespaceListMap.get(key).stream()
                            .anyMatch(ns -> ns.getName().equals(middlewareCr.getMetadata().getNamespace())
                                    && ns.getClusterId().equals(clusterId))) {
                        count = count + 1;
                    }
                }
            }
            projectDto.setMiddlewareCount(count);
            projectDtoList.add(projectDto);
        }
        return projectDtoList;
    }

    public void checkPositionUsed(String organId, String projectId, List<BackupServerDTO> backupServerDTOList){
        // 查询当前绑定的备份服务器  并确认哪些是会被移除的
        List<ProjectBackupServerDTO> usedBackupServerList = projectBackupServerService.listByProjectId(organId, projectId);
        if (!CollectionUtils.isEmpty(backupServerDTOList)) {
            usedBackupServerList = usedBackupServerList.stream()
                .filter(usedBackupServer -> backupServerDTOList.stream()
                    .noneMatch(backupServerDTO -> backupServerDTO.getId().equals(usedBackupServer.getBackupServerId())))
                .collect(Collectors.toList());
        }
        // 查询备份位置
        List<BackupPositionDTO> backupPositionDTOList = backupPositionService.list(organId, projectId, null);
        // 判断会被移除的备份服务器是否存在绑定的备份位置
        for (ProjectBackupServerDTO projectBackupServerDTO : usedBackupServerList){
            boolean flag = backupPositionDTOList.stream().anyMatch(backupPositionDTO -> backupPositionDTO.getBackupServerId().equals(projectBackupServerDTO.getBackupServerId()));
            if (flag){
                throw new BusinessException(ErrorMessage.PROJECT_BACKUP_SERVER_USING);
            }
        }
    }

}
