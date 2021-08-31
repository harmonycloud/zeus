package com.harmonycloud.zeus.service.user.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public UserRole get(String userName) throws Exception {
        // 获取角色用户对应关系
        QueryWrapper<BeanUserRole> roleUserWrapper = new QueryWrapper<BeanUserRole>().eq("username", userName);
        BeanUserRole beanUserRole = beanUserRoleMapper.selectOne(roleUserWrapper);
        if (ObjectUtils.isEmpty(beanUserRole)) {
            throw new BusinessException(ErrorMessage.USER_ROLE_NOT_EXIT);
        }
        // 获取角色信息
        BeanRole beanRole = roleService.get(beanUserRole.getRoleId());

        return new UserRole().setUserName(userName).setRoleId(beanRole.getId()).setRoleName(beanRole.getName());
    }

    @Override
    public List<UserRole> list(List<BeanUser> beanUserList) {
        // 获取所有角色用户对照关系
        QueryWrapper<BeanUserRole> roleUserWrapper = new QueryWrapper<>();
        List<BeanUserRole> beanUserRole = beanUserRoleMapper.selectList(roleUserWrapper);
        Map<String, Integer> beanSysRoleUserMap =
            beanUserRole.stream().collect(Collectors.toMap(BeanUserRole::getUserName, BeanUserRole::getRoleId));
        // 获取所有角色信息
        List<BeanRole> beanRoleList = roleService.list(true);
        Map<Integer, String> beanSysRoleMap =
            beanRoleList.stream().collect(Collectors.toMap(BeanRole::getId, BeanRole::getName));
        // 封装返回信息
        return beanUserList.stream().map(beanUser -> {
            UserRole userRole = new UserRole();
            userRole.setUserName(beanUser.getUserName()).setRoleId(beanSysRoleUserMap.get(beanUser.getUserName()))
                .setRoleName(beanSysRoleMap.get(beanSysRoleUserMap.get(beanUser.getUserName())));
            return userRole;
        }).collect(Collectors.toList());
    }

    @Override
    public void insert(UserDto userDto) {
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>().eq("username", userDto.getUserName());
        BeanUserRole existBind = beanUserRoleMapper.selectOne(wrapper);
        if (!ObjectUtils.isEmpty(existBind)) {
            throw new BusinessException(ErrorMessage.USER_ROLE_EXIST);
        }
        BeanUserRole beanUserRole = new BeanUserRole();
        beanUserRole.setRoleId(userDto.getRoleId());
        beanUserRole.setUserName(userDto.getUserName());
        beanUserRoleMapper.insert(beanUserRole);
    }

    @Override
    public void delete(String userName) {
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>().eq("username", userName);
        beanUserRoleMapper.delete(wrapper);
    }

    @Override
    public void update(UserDto userDto) {
        QueryWrapper<BeanUserRole> wrapper = new QueryWrapper<BeanUserRole>().eq("username", userDto.getUserName());
        BeanUserRole beanUserRole = new BeanUserRole();
        beanUserRole.setUserName(userDto.getUserName());
        beanUserRole.setRoleId(userDto.getRoleId());
        beanUserRoleMapper.update(beanUserRole, wrapper);
    }
}
