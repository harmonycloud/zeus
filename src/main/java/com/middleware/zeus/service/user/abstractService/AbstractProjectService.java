package com.middleware.zeus.service.user.abstractService;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.zeus.common.enums.ComponentsEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.middleware.BackupServerDetailDTO;
import com.middleware.zeus.common.model.middleware.Middleware;
import com.middleware.zeus.common.model.middleware.MiddlewareResourceInfo;
import com.middleware.zeus.common.model.middleware.Namespace;
import com.middleware.zeus.common.model.middleware.ProjectMiddlewareResourceInfo;
import com.middleware.zeus.common.model.user.ProjectDto;
import com.middleware.zeus.common.model.user.ProjectQuota;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.bean.BeanClusterMiddlewareInfo;
import com.middleware.zeus.bean.BeanMiddlewareInfo;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.*;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.user.RoleService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.ThreadPoolExecutorFactory;
import com.middleware.zeus.util.page.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.CommonConstant.DOT;
import static com.middleware.zeus.common.constants.CommonConstant.LINE;
import static com.middleware.zeus.common.constants.registry.HelmChartConstant.SVG;
import static com.middleware.zeus.common.constants.user.UserConstant.USERNAME;
import static org.apache.ibatis.ognl.DynamicSubscript.all;

/**
 * @author xutianhong
 * @Date 2023/3/26 11:43 上午
 */
@Slf4j
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
    @Autowired
    protected HelmChartService helmChartService;

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
            List<BackupPositionDTO> backupPositionDTOList = backupPositionService.list(organId, projectId, null, true);
            Map<Integer, List<BackupPositionDTO>> backupPositionMap =
                backupPositionDTOList.stream().collect(Collectors.groupingBy(BackupPositionDTO::getBackupServerDetailId));
            for (BackupServerDTO backupServerDTO : backupServerDTOList) {
                for (BackupServerDetailDTO backupServerDetailDTO:backupServerDTO.getServerDetailList()) {
                    backupServerDetailDTO.setPositionList(backupPositionMap.get(backupServerDetailDTO.getId()));
                }
            }
        }
        
        // 查询 备份服务器使用情况
        if (detail) {
            List<BackupPositionDTO> backupPositionDTOList = backupPositionService.list(organId, projectId, null, true);
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
        List<BackupPositionDTO> backupPositionDTOList = backupPositionService.list(organId, projectId, backupServerId, true);
        if(!CollectionUtils.isEmpty(backupPositionDTOList)){
            throw new BusinessException(ErrorMessage.PROJECT_BACKUP_SERVER_USING);
        }
        projectBackupServerService.delete(organId, projectId, backupServerId);
    }


    public PageInfo<MiddlewareResourceInfo> middlewareResource(String organId, String projectId,
        MiddlewareResourceQueryDto queryDto) throws Exception{
        List<Namespace> nsList = getNamespace(organId, projectId);
        // 根据项目下命名空间获取集群id集合
        List<String> clusterList = new ArrayList<>();
        clusterList = nsList.stream().map(Namespace::getClusterId).distinct().collect(Collectors.toList());
        // 查询数据
        List<Middleware> middlewareList = new ArrayList<>();
        // 统计各个集群下的middlewareList
        for (String clusterId : clusterList) {
            middlewareList.addAll(middlewareCrService.list(clusterId, null, null, false));
            // 根据分区进行过滤
            middlewareList = middlewareList.stream()
                .filter(mw -> nsList.stream().anyMatch(ns -> ns.getName().equals(mw.getNamespace())))
                .collect(Collectors.toList());
        }
        // 根据中间件类型进行过滤
        if (StringUtils.isNotEmpty(queryDto.getType())) {
            middlewareList = middlewareList.stream().filter(mwCrd -> mwCrd.getType().equals(queryDto.getType()))
                .collect(Collectors.toList());
        }

        // 进一步封装middleware信息
        middlewareList = helmChartService.convertMiddlewareList(middlewareList);
        // 对中间件进行关键词过滤
        if (StringUtils.isNotEmpty(queryDto.getKeyword())) {
            middlewareList = middlewareList.stream()
                .filter(mw -> (StringUtils.isNotEmpty(mw.getName()) && mw.getName().contains(queryDto.getKeyword()))
                    || (StringUtils.isNotEmpty(mw.getAliasName()) && mw.getAliasName().contains(queryDto.getKeyword())))
                .collect(Collectors.toList());
        }

        // 获取中间件监控信息
        List<MiddlewareResourceInfo> middlewareResourceInfoList =
            clusterService.getMwResource(middlewareList, queryDto.getTarget());

        // 对监控数据进行排序筛选
        queryDto.sortMiddlewareResourceInfo(middlewareResourceInfoList);
        // 封装page对象
        return PageUtil.convertPage(middlewareResourceInfoList, queryDto.getCurrent(), queryDto.getSize());
    }

    public List<String> userMiddlewareType(String organId, String projectId) {
        List<Namespace> nsList = getNamespace(organId, projectId);
        // 获取集群
        Set<String> clusterIdSet = new HashSet<>();
        nsList.forEach(ns -> clusterIdSet.add(ns.getClusterId()));

        List<Middleware> middlewareList = new ArrayList<>();
        for (String clusterId : clusterIdSet) {
            middlewareList.addAll(middlewareCrService.list(clusterId, null, null, false));
            // 根据分区进行过滤
            middlewareList = middlewareList.stream()
                .filter(mw -> nsList.stream().anyMatch(ns -> ns.getName().equals(mw.getNamespace())))
                .collect(Collectors.toList());
        }
        // 判断当前用户是否为超级管理员，如果不是超级管理员 对用户中间件权限进行校验
        Map<String, String> power = userService.getPower();
        // 过滤获取拥有权限的中间件
        if (!CollectionUtils.isEmpty(power)) {
            middlewareList = middlewareList.stream()
                .filter(mw -> power.keySet().stream()
                    .anyMatch(key -> !"0000".equals(power.get(key)) && mw.getType().equals(key)))
                .collect(Collectors.toList());
        }
        return middlewareList.stream().map(Middleware::getType).distinct().collect(Collectors.toList());
    }

    public List<ProjectDto> getMiddlewareCount(String organId, String projectId) {
        // 获取项目下分区列表
        List<Namespace> nsList = getNamespace(organId, projectId);

        String username = CurrentUserRepository.getUser().getUsername();
        // 查询用户信息
        UserDto userDto = userService.getUserDto(username, true);
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
        List<BackupPositionDTO> backupPositionDTOList = backupPositionService.list(organId, projectId, null, true);
        // 判断会被移除的备份服务器是否存在绑定的备份位置
        for (ProjectBackupServerDTO projectBackupServerDTO : usedBackupServerList){
            boolean flag = backupPositionDTOList.stream().anyMatch(backupPositionDTO -> backupPositionDTO.getBackupServerId().equals(projectBackupServerDTO.getBackupServerId()));
            if (flag){
                throw new BusinessException(ErrorMessage.PROJECT_BACKUP_SERVER_USING);
            }
        }
    }

}
