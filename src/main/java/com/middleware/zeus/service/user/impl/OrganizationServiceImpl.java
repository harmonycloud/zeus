package com.middleware.zeus.service.user.impl;

import static com.middleware.zeus.common.constants.CommonConstant.NUM_ONE;
import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.user.UserConstant.USERNAME;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.common.model.StorageQuota;
import com.middleware.zeus.common.model.user.*;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.bean.user.BeanPlatformQuota;
import com.middleware.zeus.dao.user.BeanOrganizationBackupServerMapper;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.middleware.BackupServerService;
import com.middleware.zeus.service.middleware.OpsManagerService;
import com.middleware.zeus.service.middleware.ProjectBackupServerService;
import com.middleware.zeus.service.user.abstractService.AbstractOrganizationService;
import com.skyview.language.annotations.TranslateAfterResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.util.uuid.UUIDUtils;
import com.middleware.zeus.bean.user.BeanOrganization;
import com.middleware.zeus.bean.user.BeanOrganizationUser;
import com.middleware.zeus.dao.user.BeanOrganizationMapper;
import com.middleware.zeus.service.k8s.StorageService;
import com.middleware.zeus.service.user.*;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/3/7 2:09 下午
 */
@Slf4j
@Service
@Skyview(target = "zeus")
public class OrganizationServiceImpl extends AbstractOrganizationService implements OrganizationService {

    @Value("${system.opsManager.enable:false}")
    private Boolean opsManager;

    @Autowired
    private BeanOrganizationMapper beanOrganizationMapper;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private PlatformQuotaService platformQuotaService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrganizationUserService organizationUserService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private OpsManagerService opsManagerService;

    @Override
    public void add(OrganizationDto organizationDto) {
        if (checkNameExist(null, organizationDto.getName())) {
            throw new BusinessException(ErrorMessage.ORGANIZATION_NAME_EXIST);
        }
        BeanOrganization beanOrganization = new BeanOrganization();
        beanOrganization.setName(organizationDto.getName());
        beanOrganization.setDescription(organizationDto.getDescription());
        beanOrganization.setOrganId(UUIDUtils.get16UUID());
        // 保存组织信息
        beanOrganizationMapper.insert(beanOrganization);
        // 保存组织用户信息
        if (StringUtils.isNotEmpty(organizationDto.getOrganizationManager())
            && organizationDto.getOrganizationManagerRoleId() != null) {
            organizationUserService.insert(beanOrganization.getOrganId(), organizationDto.getOrganizationManager(),
                organizationDto.getOrganizationManagerRoleId());
        }
        if (opsManager){
            opsManagerService.createOrgan(null, beanOrganization.getOrganId(), beanOrganization.getName());
        }
    }

    @Override
    public void update(OrganizationDto organizationDto) {
        if (!checkExist(organizationDto.getOrganId(), null)) {
            throw new BusinessException(ErrorMessage.ORGANIZATION_NOT_EXIST);
        }
        if (checkNameExist(organizationDto.getOrganId(), organizationDto.getName())){
            throw new BusinessException(ErrorMessage.ORGANIZATION_NAME_EXIST);
        }

        QueryWrapper<BeanOrganization> wrapper = new QueryWrapper<>();
        wrapper.eq("organ_id", organizationDto.getOrganId());

        BeanOrganization beanOrganization = new BeanOrganization();
        beanOrganization.setName(organizationDto.getName());
        beanOrganization.setDescription(organizationDto.getDescription());

        beanOrganizationMapper.update(beanOrganization, wrapper);
        // 更新ops manager组织名称
        if (opsManager){
            opsManagerService.updateOrgan(null, organizationDto.getOrganId(), organizationDto.getName());
        }
    }

    @Override
    public List<OrganizationDto> list(String keyword) {
        // 查询所有组织
        QueryWrapper<BeanOrganization> wrapper = new QueryWrapper<>();
        List<BeanOrganization> list = beanOrganizationMapper.selectList(wrapper);
        // 根据当前用户进行组织过滤
        list = filterByCurrentUser(list);
        // 获取组织下用户数
        List<BeanOrganizationUser> organizationUserList = organizationUserService.list(null);
        Map<String, List<BeanOrganizationUser>> userCountMap =
            organizationUserList.stream().collect(Collectors.groupingBy(BeanOrganizationUser::getOrganId));

        // 获取组织下项目数
        List<ProjectDto> projectDtoList = projectService.list(null);
        Map<String, List<ProjectDto>> projectDtoMap =
            projectDtoList.stream().collect(Collectors.groupingBy(ProjectDto::getOrganId));
        // 封装数据
        return list.stream().map(bean -> {
            OrganizationDto organizationDto = new OrganizationDto();
            BeanUtils.copyProperties(bean, organizationDto);
            if (userCountMap.containsKey(bean.getOrganId())) {
                organizationDto.setUserCount(userCountMap.get(bean.getOrganId()).size());
            }
            // 设置项目数
            if (projectDtoMap.containsKey(bean.getOrganId())) {
                organizationDto.setProjectCount(projectDtoMap.get(bean.getOrganId()).size());
            }
            return organizationDto;
            // 根据关键词进行过滤
        }).filter(organizationDto -> StringUtils.isEmpty(keyword)
            || StringUtils.containsIgnoreCase(organizationDto.getName(), keyword)).collect(Collectors.toList());
    }

    @Override
    public OrganizationDto get(String organId) {
        QueryWrapper<BeanOrganization> wrapper = new QueryWrapper<BeanOrganization>().eq("organ_id", organId);
        List<BeanOrganization> list = beanOrganizationMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        OrganizationDto organizationDto = new OrganizationDto();
        BeanUtils.copyProperties(list.get(0), organizationDto);

        return organizationDto;
    }

    @Override
    public void delete(String organId) {
        // 校验组织下项目
        List<ProjectDto> projectDtoList = projectService.list(organId);
        if (!CollectionUtils.isEmpty(projectDtoList)){
            throw new BusinessException(ErrorMessage.ORGANIZATION_INCLUDE_PROJECT);
        }
        // 删除组织下成员
        organizationUserService.delete(organId, null);
        // 删除组织
        QueryWrapper<BeanOrganization> wrapper = new QueryWrapper<BeanOrganization>().eq("organ_id", organId);
        beanOrganizationMapper.delete(wrapper);

        // 删除组织下分配资源记录
        platformQuotaService.remove(ORGAN, organId, null, null);

        // 删除ops manager组织
        if (opsManager){
            opsManagerService.deleteOrgan(null, organId);
        }
    }

    @Override
    public void allocateQuota(OrganizationQuota organizationQuota) {
        // 处理cpu\memory\storage
        if (!CollectionUtils.isEmpty(organizationQuota.getQuotaList())) {
            // todo check resource
            //checkResource(organizationQuota.getOrganId(), organizationQuota.getQuotaList());
            platformQuotaService.remove(ORGAN, organizationQuota.getOrganId(), null, null, CPU, MEMORY, STORAGE);
            for (ResourceQuotaDo resourceQuotaDo : organizationQuota.getQuotaList()){
                platformQuotaService.allocate(ORGAN, organizationQuota.getOrganId(), resourceQuotaDo);
            }
        }
        // 记录备份服务器
        allocateBackupServer(organizationQuota);
    }

    @Override
    public List<ResourceQuotaDo> getStorageQuota(String organId, String clusterIds, boolean detail) {
        // 获取租户自身存储配额
        List<ResourceQuotaDo> organResourceQuotaDoList = platformQuotaService.getQuota(ORGAN, organId, null, STORAGE);
        // 根据集群id进行过滤
        if (StringUtils.isNotEmpty(clusterIds)) {
            List<String> clusterIdList = getClusterIdList(clusterIds);
            organResourceQuotaDoList = organResourceQuotaDoList.stream()
                .filter(org -> clusterIdList.stream().anyMatch(clusterId -> clusterId.equals(org.getClusterId())))
                .collect(Collectors.toList());
        }
        
        // 设置存储名称
        platformQuotaService.convertStorageName(organResourceQuotaDoList);
        // 获取项目列表 和 项目资源分配总额
        if (detail) {
            // 获取项目id列表
            List<ProjectDto> projectDtoList = projectService.list(organId);
            List<String> uidList = projectDtoList.stream().map(ProjectDto::getProjectId).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(uidList)){
                // 查询项目申请配额总和
                List<ResourceQuotaDo> projectResourceQuotaDoList = platformQuotaService.getQuota(PROJECT, uidList, STORAGE);
                // 封装数据
                organResourceQuotaDoList =
                        platformQuotaService.convertUsedResource(organResourceQuotaDoList, projectResourceQuotaDoList);
            }
        }
        
        
        // 设置集群名称
        Map<String, String> clusterNickNameMap = clusterService.getClusterAliasName();
        for (ResourceQuotaDo resourceQuotaDo : organResourceQuotaDoList) {
            if (StringUtils.isNotEmpty(resourceQuotaDo.getClusterId())
                && clusterNickNameMap.containsKey(resourceQuotaDo.getClusterId())) {
                resourceQuotaDo.setClusterNickName(clusterNickNameMap.get(resourceQuotaDo.getClusterId()));
            }
        }
        return organResourceQuotaDoList;
    }

    @Override
    public void removeStorageQuota(String organId, String storageId, String clusterId) {
        // 获取项目id列表
        List<ProjectDto> projectDtoList = projectService.list(organId);
        List<String> uidList = projectDtoList.stream().map(ProjectDto::getProjectId).collect(Collectors.toList());
        // 校验存储是否能存在已分配
        if (!CollectionUtils.isEmpty(uidList)){
            List<BeanPlatformQuota> beanPlatformQuotaList = platformQuotaService.findQuota(PROJECT, uidList, clusterId, storageId, STORAGE);
            if (!CollectionUtils.isEmpty(beanPlatformQuotaList)){
                throw new BusinessException(ErrorMessage.ORGANIZATION_STORAGE_USING);
            }
        }
        // 删除存储
        platformQuotaService.remove(ORGAN, organId, null, storageId, STORAGE);
    }

    @Override
    public List<ResourceQuotaDo> getCpuMemoryQuota(String organId, boolean detail) {
        // 获取租户自身cpu memory 配额
        List<ResourceQuotaDo> resourceQuotaDoList = platformQuotaService.getQuota(ORGAN, organId, null, CPU, MEMORY);
        if (detail) {
            // 获取项目id列表
            List<ProjectDto> projectDtoList = projectService.list(organId);
            List<String> uidList = projectDtoList.stream().map(ProjectDto::getProjectId).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(uidList)){
                // 查询项目申请配额总和
                List<ResourceQuotaDo> projectResourceQuotaDoList =
                        platformQuotaService.getQuota(PROJECT, uidList, CPU, MEMORY);
                // 封装数据
                resourceQuotaDoList =
                        platformQuotaService.convertUsedResource(resourceQuotaDoList, projectResourceQuotaDoList);
            }
        }
        // 设置集群别名
        Map<String, String> clusterNickNameMap = clusterService.getClusterAliasName();
        return resourceQuotaDoList.stream().peek(resourceQuotaDo -> {
            if (StringUtils.isNotEmpty(resourceQuotaDo.getClusterId())
                    && clusterNickNameMap.containsKey(resourceQuotaDo.getClusterId())) {
                resourceQuotaDo.setClusterNickName(clusterNickNameMap.get(resourceQuotaDo.getClusterId()));
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void removeCpuMemoryQuota(String organId, String clusterId) {
        // 获取项目id列表
        List<ProjectDto> projectDtoList = projectService.list(organId);
        List<String> uidList = projectDtoList.stream().map(ProjectDto::getProjectId).collect(Collectors.toList());
        // 校验cpu memory是否能存在已分配
        if (!CollectionUtils.isEmpty(uidList)){
            List<BeanPlatformQuota> beanPlatformQuotaList = platformQuotaService.findQuota(PROJECT, uidList, clusterId, null, CPU, MEMORY);
            if (CollectionUtils.isEmpty(beanPlatformQuotaList)){
                throw new BusinessException(ErrorMessage.ORGANIZATION_CPU_MEMORY_USING);
            }
        }
        // 删除cpu memory
        platformQuotaService.remove(ORGAN, organId, null, null, CPU, MEMORY);
    }

    @Override
    @TranslateAfterResult
    public List<UserDto> listOrganUser(String organId, Boolean allocatable) {
        // 获取组织下用户列表
        Map<String, BeanOrganizationUser> userMap = organizationUserService.list(organId).stream().collect(Collectors.toMap(BeanOrganizationUser::getUsername, u -> u));
        // 获取所有用户列表
        List<UserDto> userDtoList = userService.list(null);
        // 获取超级管理员用户列表
        Map<String, UserRole> adminMap = userRoleService.findByRoleId(NUM_ONE).stream().collect(Collectors.toMap(UserRole::getUserName, ur -> ur));

        userDtoList = userDtoList.stream().filter(userDto -> {
            if (allocatable) {
                return !userMap.containsKey(userDto.getUserName()) && !adminMap.containsKey(userDto.getUserName());
            } else {
                if (userMap.containsKey(userDto.getUserName())){
                    BeanOrganizationUser organizationUser = userMap.get(userDto.getUserName());
                    if (organizationUser.getRoleId() == null){
                        userDto.setRoleName("普通用户");
                    }else {
                        userDto.setRoleId(organizationUser.getRoleId()).setRoleName("组织管理员");
                    }
                    return true;
                }
                return false;
            }
        }).collect(Collectors.toList());

        // 刷新ops manager中组织下的用户
        if (opsManager && allocatable != null && !allocatable){
            opsManagerService.refreshOrganUser(null, organId, userDtoList);
        }

        return userDtoList;
    }

    @Override
    public void addOrganUser(OrganizationDto organizationDto) {
        String organId = organizationDto.getOrganId();
        if (!checkExist(organId, null)) {
            throw new BusinessException(ErrorMessage.ORGANIZATION_NOT_EXIST);
        }
        if (CollectionUtils.isEmpty(organizationDto.getUserDtoList())){
            throw new BusinessException(ErrorMessage.ORGANIZATION_ADD_USER_EMPTY_LIST);
        }
        organizationDto.getUserDtoList()
            .forEach(userDto -> organizationUserService.insert(organId, userDto.getUserName(), userDto.getRoleId()));
        // 同步更新至ops manager
        if (opsManager){
            for (UserDto userDto : organizationDto.getUserDtoList()){
                opsManagerService.allocateOrganUser(null, organId, userDto.getUserName(), userDto.getRoleId());
            }
        }
    }

    @Override
    public void updateOrganUser(String organId, String username, Integer roleId) {
        if (!checkExist(organId, null)){
            throw new BusinessException(ErrorMessage.ORGANIZATION_NOT_EXIST);
        }
        organizationUserService.update(organId, username, roleId);

        // todo 同步更新至ops manager(暂不支持)
        if (opsManager){
        }
    }

    @Override
    public void deleteOrganUser(String organId, String username) {
        // 查询组织下项目
        List<ProjectDto> projectDtoList = projectService.list(organId);
        // 查询该用户所拥有角色信息
        List<UserRole> userRoleList = userRoleService.get(username, organId, null);
        if (userRoleList.stream().anyMatch(userRole -> projectDtoList.stream()
            .anyMatch(projectDto -> StringUtils.isNotEmpty(userRole.getProjectId()) && userRole.getProjectId().equals(projectDto.getProjectId())))) {
            throw new BusinessException(ErrorMessage.ORGANIZATION_USER_USED_IN_PROJECT);
        }
        organizationUserService.delete(organId, username);

        // todo 同步更新至ops manager(暂不支持)
        if (opsManager){
        }
    }

    @Override
    public void clear(String clusterId) {
        // 移除项目下的资源分配
        platformQuotaService.remove(PROJECT, null, clusterId, null, CPU, MEMORY, STORAGE);
        // 移除组织下的资源分配
        platformQuotaService.remove(ORGAN, null, clusterId, null, CPU, MEMORY, STORAGE);
    }


    private boolean checkExist(String organId, String name) {
        QueryWrapper<BeanOrganization> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(name)) {
            wrapper.eq("name", name);
        }
        List<BeanOrganization> list = beanOrganizationMapper.selectList(wrapper);
        return !CollectionUtils.isEmpty(list);
    }
    
    private boolean checkNameExist(String organId, String name) {
        QueryWrapper<BeanOrganization> wrapper =
            new QueryWrapper<BeanOrganization>().ne("organ_id", organId).eq("name", name);
        if (StringUtils.isNotEmpty(organId)){
            wrapper.ne("organ_id", organId);
        }
        List<BeanOrganization> list = beanOrganizationMapper.selectList(wrapper);
        return !CollectionUtils.isEmpty(list);
    }

    public List<BeanOrganization> filterByCurrentUser(List<BeanOrganization> list) {
        // 获取当前用户
         CurrentUser currentUser = CurrentUserRepository.getUserExistNull();
        JSONObject user = JwtTokenComponent.checkToken(currentUser.getToken()).getValue();
        // 获取当前用户所在所有项目内的角色信息
        String username = user.getString(USERNAME);
        UserDto userDto = userService.getUserDto(username, true);
        if (userDto.getIsAdmin() != null && !userDto.getIsAdmin() && userDto.getManager() == null) {
            List<BeanOrganizationUser> currentUserOrganList = organizationUserService.listByUsername(username);
            list = list.stream()
                .filter(beanOrganization -> currentUserOrganList.stream()
                    .anyMatch(currentUserOrgan -> currentUserOrgan.getOrganId().equals(beanOrganization.getOrganId())))
                .collect(Collectors.toList());
        }
        return list;
    }

    /**
     * 校验资源是否足够
     */
    public void checkResource(String organId, List<ResourceQuotaDo> resourceQuotaDoList){
        boolean flag = false;

        // 获取当前分配情况
        List<ResourceQuotaDo> resourceQuotaDos = platformQuotaService.getQuota(ORGAN, organId, null, CPU, MEMORY, STORAGE);
        Map<String, ResourceQuotaDo> currentResourceQuotaMap = resourceQuotaDos.stream().collect(Collectors.toMap(ResourceQuotaDo::getClusterNickName, Function.identity()));

        for (ResourceQuotaDo resourceQuotaDo : resourceQuotaDoList){
            ResourceQuotaDo clusterQuota = clusterService.getResourceQuotaInfo(resourceQuotaDo.getClusterId(), true);


            // todo
            double cpu = clusterQuota.getCpu().getRequest() - clusterQuota.getCpu().getUsed() - resourceQuotaDo.getCpu().getRequest();
            double memory = clusterQuota.getMemory().getRequest() - clusterQuota.getMemory().getUsed() - resourceQuotaDo.getMemory().getRequest();

            Map<String, Double> currentStorageMap = resourceQuotaDo.getStorageList().stream().collect(Collectors.toMap(StorageQuota::getStorageId, storageQuota -> storageQuota.getStorage().getRequest()));
            Map<String, Double> storageMap = new HashMap<>();
            for (StorageQuota storageQuota : clusterQuota.getStorageList()) {
                storageMap.put(storageQuota.getStorageId(),
                    storageQuota.getStorage().getRequest() - storageQuota.getStorage().getUsed()
                        - (currentStorageMap.containsKey(storageQuota.getStorageId())
                            ? currentStorageMap.get(storageQuota.getStorageId()) : 0));
            }

        }
    }

}
