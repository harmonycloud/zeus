package com.middleware.zeus.service.user.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.zeus.common.model.user.RoleDto;
import com.middleware.zeus.bean.user.*;
import com.middleware.zeus.bean.user.BeanProject;
import com.middleware.zeus.bean.user.BeanUserRole;
import com.middleware.zeus.dao.user.BeanOrganizationMapper;
import com.middleware.zeus.dao.user.BeanProjectMapper;
import com.middleware.zeus.service.user.RoleService;
import com.middleware.zeus.service.user.UserRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.user.UserRole;
import com.middleware.zeus.dao.user.BeanUserRoleMapper;

/**
 * @author xutianhong
 * @Date 2021/7/27 8:44 下午
 */
@Service
public class UserRoleServiceImpl implements UserRoleService {

    @Autowired
    private RoleService roleService;
    @Autowired
    private BeanUserRoleMapper beanUserRoleMapper;
    @Autowired
    private BeanProjectMapper beanProjectMapper;
    @Autowired
    private BeanOrganizationMapper beanOrganizationMapper;

    @Override
    public List<UserRole> get(String userName, String organId, String projectId) {
        // 获取角色用户对应关系
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>().eq("username", userName);
        if (StringUtils.isNotEmpty(organId)){
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(projectId)){
            wrapper.eq("project_id", projectId);
        }
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(wrapper);
        // 获取角色信息
        Map<Integer,
            RoleDto> roleDtoMap = roleService.list(null).stream()
                .filter(roleDto -> beanUserRoleList.stream()
                    .anyMatch(beanUserRole -> beanUserRole.getRoleId().equals(roleDto.getId())))
                .collect(Collectors.toMap(RoleDto::getId, r -> r));
        return beanUserRoleList.stream().map(beanUserRole -> {
            UserRole userRole = new UserRole();
            BeanUtils.copyProperties(beanUserRole, userRole);
            userRole.setRoleName(roleDtoMap.get(beanUserRole.getRoleId()).getName());
            userRole.setWeight(roleDtoMap.get(beanUserRole.getRoleId()).getWeight());
            userRole.setPower(roleDtoMap.get(beanUserRole.getRoleId()).getPower());
            userRole.setRoleType(roleDtoMap.get(beanUserRole.getRoleId()).getType());
            return userRole;
        }).collect(Collectors.toList());
    }

    @Override
    public UserRole getUserRole(String userName, String organId, String projectId) {
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>().eq("username", userName)
            .eq("organ_id", organId).eq("project_id", projectId);
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanUserRoleList)) {
            UserRole userRole = new UserRole();
            BeanUtils.copyProperties(beanUserRoleList.get(0), userRole);
            return userRole;
        }
        return null;
    }

    @Override
    public List<UserRole> list() {
        // 获取所有角色用户对照关系
        QueryWrapper<BeanUserRole> roleUserWrapper = new QueryWrapper<>();
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(roleUserWrapper);
        // 获取项目数据
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<>();
        Map<String, BeanProject> beanProjectMap =
            beanProjectMapper.selectList(wrapper).stream().collect(Collectors.toMap(BeanProject::getProjectId, b -> b));
        // 获取组织名称
        QueryWrapper<BeanOrganization> organWrapper = new QueryWrapper<>();
        Map<String, String> organNameMap =
                beanOrganizationMapper.selectList(organWrapper).stream().collect(Collectors.toMap(BeanOrganization::getOrganId, BeanOrganization::getName));
        // 获取所有角色信息
        List<RoleDto> beanRoleList = roleService.list(null);
        Map<Integer, String> beanSysRoleMap =
            beanRoleList.stream().collect(Collectors.toMap(RoleDto::getId, RoleDto::getName));
        // 封装返回信息
        return beanUserRoleList.stream().map(beanUser -> {
            UserRole userRole = new UserRole();
            BeanUtils.copyProperties(beanUser, userRole);
            userRole.setUserName(beanUser.getUserName()).setRoleName(beanSysRoleMap.get(beanUser.getRoleId()));
            if (StringUtils.isNotEmpty(userRole.getOrganId()) && organNameMap.containsKey(userRole.getOrganId())){
                userRole.setOrganName(organNameMap.get(userRole.getOrganId()));
            }
            if (StringUtils.isNotEmpty(userRole.getProjectId()) && beanProjectMap.containsKey(userRole.getProjectId())) {
                userRole.setProjectName(beanProjectMap.get(userRole.getProjectId()).getAliasName());
            }
            return userRole;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserRole> list(String organId, String projectId) {
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(projectId)) {
            wrapper.eq("project_id", projectId);
        }
        // 获取所有角色信息
        List<RoleDto> beanRoleList = roleService.list(null);
        Map<Integer, String> beanSysRoleMap =
            beanRoleList.stream().collect(Collectors.toMap(RoleDto::getId, RoleDto::getName));
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(wrapper);
        return beanUserRoleList.stream().map(beanUser -> {
            UserRole userRole = new UserRole();
            BeanUtils.copyProperties(beanUser, userRole);
            userRole.setUserName(beanUser.getUserName());
            if (beanSysRoleMap.containsKey(beanUser.getRoleId())) {
                userRole.setRoleName(beanSysRoleMap.get(beanUser.getRoleId()));
            }
            return userRole;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserRole> findByRoleId(Integer roleId) {
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>().eq("role_id", roleId);
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanUserRoleList)){
            return new ArrayList<>();
        }
        return beanUserRoleList.stream().map(beanUserRole -> {
            UserRole userRole = new UserRole();
            BeanUtils.copyProperties(beanUserRole, userRole);
            return userRole;
        }).collect(Collectors.toList());
    }

    @Override
    public void insert(String organId, String projectId, String username, Integer roleId) {
        QueryWrapper<BeanUserRole> wrapper =
            new QueryWrapper<BeanUserRole>().eq("username", username);
        if (StringUtils.isNotEmpty(organId)){
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(projectId)){
            wrapper.eq("project_id", projectId);
        }
        List<BeanUserRole> existBind = beanUserRoleMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(existBind) && roleId != 1) {
            throw new BusinessException(ErrorMessage.USER_ROLE_EXIST);
        }
        BeanUserRole beanUserRole = new BeanUserRole();
        beanUserRole.setOrganId(organId);
        beanUserRole.setProjectId(projectId);
        beanUserRole.setRoleId(roleId);
        beanUserRole.setUserName(username);
        beanUserRoleMapper.insert(beanUserRole);
    }

    @Override
    public void delete(String userName, String organId, String projectId, Integer roleId) {
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>();
        if (StringUtils.isNotEmpty(userName)){
            wrapper.eq("username", userName);
        }
        if(StringUtils.isNotEmpty(organId)){
            wrapper.eq("organ_id", organId);
        }
        if(StringUtils.isNotEmpty(projectId)){
            wrapper.eq("project_id", projectId);
        }
        if (roleId != null ){
            wrapper.eq("role_id", roleId);
        }
        beanUserRoleMapper.delete(wrapper);
    }

    @Override
    public void update(UserRole userRole) {
        String organId = userRole.getOrganId();
        String projectId = userRole.getProjectId();
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>().eq("username", userRole.getUserName());
        if (StringUtils.isNotEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        } else {
            wrapper.isNull("organ_id");
        }
        if (StringUtils.isNotEmpty(projectId)) {
            wrapper.eq("project_id", projectId);
        } else {
            wrapper.isNull("project_id");
        }
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(wrapper);
        BeanUserRole beanUserRole = new BeanUserRole();
        beanUserRole.setUserName(userRole.getUserName());
        beanUserRole.setRoleId(userRole.getRoleId());
        if (StringUtils.isNotEmpty(projectId)){
            beanUserRole.setProjectId(projectId);
        }
        if (CollectionUtils.isEmpty(beanUserRoleList)) {
            beanUserRoleMapper.insert(beanUserRole);
        } else {
            beanUserRoleMapper.update(beanUserRole, wrapper);
        }
    }

    @Override
    public void updateProjectManager2Normal(String organId, String projectId) {
        QueryWrapper<BeanUserRole> wrapper =
                new QueryWrapper<BeanUserRole>().eq("organ_id", organId).eq("project_id", projectId).eq("role_id", 2);
        BeanUserRole beanUserRole = new BeanUserRole();
        beanUserRole.setRoleId(4);
        beanUserRoleMapper.update(beanUserRole, wrapper);
    }

}
