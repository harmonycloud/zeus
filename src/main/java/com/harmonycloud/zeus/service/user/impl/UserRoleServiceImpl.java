package com.harmonycloud.zeus.service.user.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.model.user.RoleDto;
import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.caas.common.model.user.UserRole;
import com.harmonycloud.zeus.bean.user.BeanRole;
import com.harmonycloud.zeus.bean.user.BeanUserRole;
import com.harmonycloud.zeus.bean.user.BeanUser;
import com.harmonycloud.zeus.dao.user.BeanUserRoleMapper;

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

    @Override
    public List<UserRole> get(String userName) {
        // 获取角色用户对应关系
        QueryWrapper<BeanUserRole> roleUserWrapper = new QueryWrapper<BeanUserRole>().eq("username", userName);
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(roleUserWrapper);
        // 获取角色信息
        RoleDto roleDto = roleService.get(beanUserRoleList.get(0).getRoleId());
        return beanUserRoleList.stream().map(beanUserRole -> {
            UserRole userRole = new UserRole();
            BeanUtils.copyProperties(beanUserRole, userRole);
            userRole.setRoleName(roleDto.getName());
            return userRole;
        }).collect(Collectors.toList());
    }

    @Override
    public Boolean checkAdmin(String username) {
        // 获取角色用户对应关系
        QueryWrapper<BeanUserRole> roleUserWrapper =
            new QueryWrapper<BeanUserRole>().eq("username", username).eq("role_id", "1");
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(roleUserWrapper);
        return !CollectionUtils.isEmpty(beanUserRoleList);
    }

    @Override
    public List<UserRole> list() {
        // 获取所有角色用户对照关系
        QueryWrapper<BeanUserRole> roleUserWrapper = new QueryWrapper<>();
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(roleUserWrapper);
        // 获取所有角色信息
        List<RoleDto> beanRoleList = roleService.list(null);
        Map<Integer, String> beanSysRoleMap =
            beanRoleList.stream().collect(Collectors.toMap(RoleDto::getId, RoleDto::getName));
        // 封装返回信息
        return beanUserRoleList.stream().map(beanUser -> {
            UserRole userRole = new UserRole();
            BeanUtils.copyProperties(beanUser, userRole);
            userRole.setUserName(beanUser.getUserName())
                .setRoleName(beanSysRoleMap.get(beanUser.getRoleId()));
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
    public void insert(String projectId, String username, Integer roleId) {
        QueryWrapper<BeanUserRole> wrapper =
            new QueryWrapper<BeanUserRole>().eq("project_id", projectId).eq("username", username);
        BeanUserRole existBind = beanUserRoleMapper.selectOne(wrapper);
        if (!ObjectUtils.isEmpty(existBind)) {
            throw new BusinessException(ErrorMessage.USER_ROLE_EXIST);
        }
        BeanUserRole beanUserRole = new BeanUserRole();
        beanUserRole.setProjectId(projectId);
        beanUserRole.setRoleId(roleId);
        beanUserRole.setUserName(username);
        beanUserRoleMapper.insert(beanUserRole);
    }

    @Override
    public void delete(String userName, String projectId) {
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>().eq("username", userName);
        if(StringUtils.isNotEmpty(projectId)){
            wrapper.eq("project_id", projectId);
        }
        beanUserRoleMapper.delete(wrapper);
    }

    @Override
    public void update(UserDto userDto, String projectId) {
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>().eq("username", userDto.getUserName());
        List<BeanUserRole> beanUserRoleList = beanUserRoleMapper.selectList(wrapper);
        BeanUserRole beanUserRole = new BeanUserRole();
        beanUserRole.setUserName(userDto.getUserName());
        beanUserRole.setRoleId(userDto.getRoleId());
        if (StringUtils.isNotEmpty(projectId)){
            beanUserRole.setProjectId(projectId);
        }
        if (CollectionUtils.isEmpty(beanUserRoleList)) {
            beanUserRoleMapper.insert(beanUserRole);
        } else {
            beanUserRoleMapper.update(beanUserRole, wrapper);
        }
    }
}
