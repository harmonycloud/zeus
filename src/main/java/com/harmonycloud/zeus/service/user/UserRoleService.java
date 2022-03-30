package com.harmonycloud.zeus.service.user;

import java.util.List;

import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.caas.common.model.user.UserRole;
import com.harmonycloud.zeus.bean.user.BeanUser;

/**
 * @author xutianhong
 * @Date 2021/7/27 8:43 下午
 */
public interface UserRoleService {

    /**
     * 获取角色
     *
     * @param userName 账户
     * @return BeanSysRole
     */
    List<UserRole> get(String userName);

    /**
     * 校验是否为超级管理员权限
     *
     * @param username 用户名
     * @return Boolean
     */
    Boolean checkAdmin(String username);

    /**
     * 获取绑定制定角色的用户
     *
     * @param roleId 角色id
     * @return BeanSysRole
     */
    List<UserRole> findByRoleId(Integer roleId);

    /**
     * 获取用户角色关联关系
     *
     * @return List<BeanSysRole>
     */
    List<UserRole> list();

    /**
     * 创建用户角色关联
     *
     * @param projectId  项目id
     * @param username   用户名
     * @param roleId     角色id
     */
    void insert(String projectId, String username, Integer roleId);

    /**
     * 删除用户角色关联
     *
     * @param userName 账户
     */
    void delete(String userName, String projectId);

    /**
     * 创建用户角色关联
     *
     * @param userDto 用户信息
     */
    void update(UserDto userDto, String projectId);

}
