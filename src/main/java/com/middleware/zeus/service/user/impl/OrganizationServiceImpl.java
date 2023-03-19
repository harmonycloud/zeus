package com.middleware.zeus.service.user.impl;

import static com.middleware.caas.common.constants.NameConstant.*;
import static com.middleware.caas.common.constants.user.UserConstant.USERNAME;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.user.*;
import com.middleware.zeus.bean.user.BeanOrganizationBackupServer;
import com.middleware.zeus.bean.user.BeanPlatformQuota;
import com.middleware.zeus.dao.user.BeanOrganizationBackupServerMapper;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.middleware.BackupServerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.StorageDto;
import com.middleware.caas.common.model.StorageQuota;
import com.middleware.caas.common.model.middleware.StorageClassInfo;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.tool.uuid.UUIDUtils;
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
public class OrganizationServiceImpl implements OrganizationService {

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
    private BeanOrganizationBackupServerMapper beanOrganizationBackupServerMapper;
    @Autowired
    private BackupServerService backupServerService;
    @Autowired
    private StorageService storageService;
    @Autowired
    private ClusterService clusterService;

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
    }

    @Override
    public List<OrganizationDto> list(String keyword) {
        // 查询所有组织
        QueryWrapper<BeanOrganization> wrapper = new QueryWrapper<>();
        List<BeanOrganization> list = beanOrganizationMapper.selectList(wrapper);
        // 根据当前用户进行组织过滤
        filterByCurrentUser(list);
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
        if (CollectionUtils.isEmpty(projectDtoList)){
            throw new BusinessException(ErrorMessage.ORGANIZATION_INCLUDE_PROJECT);
        }
        // 删除组织下成员
        organizationUserService.delete(organId, null);
        // 删除组织
        QueryWrapper<BeanOrganization> wrapper = new QueryWrapper<BeanOrganization>().eq("organ_id", organId);
        beanOrganizationMapper.delete(wrapper);

        // 删除组织下分配资源记录
        platformQuotaService.remove(ORGAN, organId, null);
    }

    @Override
    public void allocateQuota(OrganizationQuota organizationQuota) {
        // 处理cpu\memory\storage
        if (organizationQuota.getQuotas() != null) {
            platformQuotaService.allocate(ORGAN, organizationQuota.getOrganId(), organizationQuota.getQuotas());
        }
        // 记录备份服务器
        // todo 校验备份服务器是否已被使用
        if (!CollectionUtils.isEmpty(organizationQuota.getBackupServerDTOList())) {
            // 删除当前所有绑定关系
            QueryWrapper<BeanOrganizationBackupServer> delete =
                new QueryWrapper<BeanOrganizationBackupServer>().eq("organ_id", organizationQuota.getOrganId());
            beanOrganizationBackupServerMapper.delete(delete);
            for (BackupServerDTO backupServerDTO : organizationQuota.getBackupServerDTOList()) {
                BeanOrganizationBackupServer server = new BeanOrganizationBackupServer();
                server.setOrganId(organizationQuota.getOrganId());
                server.setClusterId(backupServerDTO.getClusterId());
                server.setBackupServerId(backupServerDTO.getId());
                beanOrganizationBackupServerMapper.insert(server);
            }
        }
    }

    @Override
    public List<ResourceQuotaDo> getStorageQuota(String organId, boolean detail) {
        // 获取租户自身存储配额
        List<ResourceQuotaDo> organResourceQuotaDoList = platformQuotaService.getQuota(ORGAN, organId, STORAGE);
        // 设置存储名称
        platformQuotaService.convertStorageName(organResourceQuotaDoList);
        // 获取项目列表 和 项目资源分配总额
        if (detail) {
            // 获取项目id列表
            List<ProjectDto> projectDtoList = projectService.list(organId);
            List<String> uidList = projectDtoList.stream().map(ProjectDto::getProjectId).collect(Collectors.toList());
            // 查询项目申请配额总和
            List<ResourceQuotaDo> projectResourceQuotaDoList = platformQuotaService.getQuota(PROJECT, uidList, STORAGE);
            // 封装数据
            organResourceQuotaDoList =
                platformQuotaService.convertUsedResource(organResourceQuotaDoList, projectResourceQuotaDoList);
        }
        return organResourceQuotaDoList;
    }

    @Override
    public void removeStorageQuota(String organId, String storageId, String clusterId) {
        // 获取项目id列表
        List<ProjectDto> projectDtoList = projectService.list(organId);
        List<String> uidList = projectDtoList.stream().map(ProjectDto::getProjectId).collect(Collectors.toList());
        // 校验存储是否能存在已分配
        List<BeanPlatformQuota> beanPlatformQuotaList = platformQuotaService.findQuota(PROJECT, uidList, clusterId, storageId, STORAGE);
        if (CollectionUtils.isEmpty(beanPlatformQuotaList)){
            throw new BusinessException(ErrorMessage.ORGANIZATION_STORAGE_USING);
        }
        // 删除存储
        platformQuotaService.remove(ORGAN, organId, storageId, STORAGE);
    }

    @Override
    public List<ResourceQuotaDo> getCpuMemoryQuota(String organId, boolean detail) {
        // 获取租户自身cpu memory 配额
        List<ResourceQuotaDo> resourceQuotaDoList = platformQuotaService.getQuota(ORGAN, organId, CPU, MEMORY);
        if (detail) {
            // 获取项目id列表
            List<ProjectDto> projectDtoList = projectService.list(organId);
            List<String> uidList = projectDtoList.stream().map(ProjectDto::getProjectId).collect(Collectors.toList());
            // 查询项目申请配额总和
            List<ResourceQuotaDo> projectResourceQuotaDoList =
                platformQuotaService.getQuota(PROJECT, uidList, CPU, MEMORY);
            // 封装数据
            resourceQuotaDoList =
                platformQuotaService.convertUsedResource(resourceQuotaDoList, projectResourceQuotaDoList);
        }
        return resourceQuotaDoList;
    }

    @Override
    public void removeCpuMemoryQuota(String organId, String clusterId) {
        // 获取项目id列表
        List<ProjectDto> projectDtoList = projectService.list(organId);
        List<String> uidList = projectDtoList.stream().map(ProjectDto::getProjectId).collect(Collectors.toList());
        // 校验cpu memory是否能存在已分配
        List<BeanPlatformQuota> beanPlatformQuotaList = platformQuotaService.findQuota(PROJECT, uidList, clusterId, null, CPU, MEMORY);
        if (CollectionUtils.isEmpty(beanPlatformQuotaList)){
            throw new BusinessException(ErrorMessage.ORGANIZATION_CPU_MEMORY_USING);
        }
        // 删除cpu memory
        platformQuotaService.remove(ORGAN, organId, null, CPU, MEMORY);
    }

    @Override
    public List<BackupServerDTO> getBackupServer(String organId) {
        QueryWrapper<BeanOrganizationBackupServer> wrapper =
            new QueryWrapper<BeanOrganizationBackupServer>().eq("organ_id", organId);
        List<BeanOrganizationBackupServer> list = beanOrganizationBackupServerMapper.selectList(wrapper);
        List<Integer> idList = list.stream().map(BeanOrganizationBackupServer::getBackupServerId).collect(Collectors.toList());

        // 设置集群别名
        List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
        Map<String, String> clusterNickNameMap = clusterList.stream().collect(Collectors.toMap(MiddlewareClusterDTO::getId, MiddlewareClusterDTO::getNickname));

        return backupServerService.list(idList).stream().peek(backupServerDTO -> {
            if (StringUtils.isNotEmpty(backupServerDTO.getClusterId()) && clusterNickNameMap.containsKey(backupServerDTO.getClusterId())){
                backupServerDTO.setClusterNickName(clusterNickNameMap.get(backupServerDTO.getClusterId()));
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void removeBackupServer(String organId, Integer backupServerId, String clusterId) {
        List<BackupServerDTO> backupServerDTOList = projectService.getBackupServer(organId, null);
        if (!CollectionUtils.isEmpty(backupServerDTOList) && backupServerDTOList.stream()
            .anyMatch(backupServerDTO -> backupServerId.equals(backupServerDTO.getId()))) {
            throw new BusinessException(ErrorMessage.ORGANIZATION_BACKUP_SERVER_USING);
        }
        QueryWrapper<BeanOrganizationBackupServer> wrapper =
            new QueryWrapper<BeanOrganizationBackupServer>().eq("backupServerId", backupServerId);
        beanOrganizationBackupServerMapper.delete(wrapper);
    }

    @Override
    public List<UserDto> listOrganUser(String organId, Boolean allocatable) {
        List<BeanOrganizationUser> userList = organizationUserService.list(organId);
        List<UserDto> userDtoList = userService.list(null);
        return userDtoList.stream().filter(userDto -> {
            if (allocatable) {
                return userList.stream().noneMatch(user -> userDto.getUserName().equals(user.getUsername()))
                    && userDto.getUserRoleList().stream().noneMatch(userRole -> userRole.getRoleId() == 1);
            } else {
                return userList.stream().anyMatch(user -> userDto.getUserName().equals(user.getUsername()));
            }
        }).peek(userDto -> {
            if (!CollectionUtils.isEmpty(userDto.getUserRoleList())) {
                List<UserRole> userRoleList = userDto.getUserRoleList().stream()
                    .filter(ur -> StringUtils.isNotEmpty(ur.getOrganId()) && ur.getOrganId().equals(organId))
                    .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(userRoleList)) {
                    UserRole userRole = userRoleList.get(0);
                    userDto.setRoleId(userRole.getRoleId());
                    userDto.setRoleName(userRole.getRoleName());
                }
                userDto.setUserRoleList(null);
            } else {
                userDto.setRoleName("普通用户");
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void addOrganUser(String organId, String username, Integer roleId) {
        if (!checkExist(organId, null)){
            throw new BusinessException(ErrorMessage.ORGANIZATION_NOT_EXIST);
        }
        organizationUserService.insert(organId, username, roleId);
    }

    @Override
    public void updateOrganUser(String organId, String username, Integer roleId) {
        if (!checkExist(organId, null)){
            throw new BusinessException(ErrorMessage.ORGANIZATION_NOT_EXIST);
        }
        organizationUserService.update(organId, username, roleId);
    }

    @Override
    public void deleteOrganUser(String organId, String username) {
        // 查询组织下项目
        List<ProjectDto> projectDtoList = projectService.list(organId);
        // 查询该用户所拥有角色信息
        List<UserRole> userRoleList = userRoleService.get(username);
        if (userRoleList.stream().anyMatch(userRole -> projectDtoList.stream()
            .anyMatch(projectDto -> userRole.getProjectId().equals(projectDto.getProjectId())))) {
            throw new BusinessException(ErrorMessage.ORGANIZATION_USER_USED_IN_PROJECT);
        }
        organizationUserService.delete(organId, username);
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

    public void filterByCurrentUser(List<BeanOrganization> list) {
        // 获取当前用户
        CurrentUser currentUser = CurrentUserRepository.getUserExistNull();
        JSONObject user = JwtTokenComponent.checkToken(currentUser.getToken()).getValue();
        // 获取当前用户所在所有项目内的角色信息
        String username = user.getString(USERNAME);
        UserDto userDto = userService.getUserDto(username);
        if (userDto.getIsAdmin() != null && !userDto.getIsAdmin()) {
            List<BeanOrganizationUser> currentUserOrganList = organizationUserService.listByUsername(username);
            list = list.stream()
                .filter(beanOrganization -> currentUserOrganList.stream()
                    .anyMatch(currentUserOrgan -> currentUserOrgan.getOrganId().equals(beanOrganization.getOrganId())))
                .collect(Collectors.toList());
        }
    }

}
