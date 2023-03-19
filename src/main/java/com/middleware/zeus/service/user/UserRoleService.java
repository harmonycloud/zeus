package com.middleware.zeus.service.user;

import java.util.List;

import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.common.model.user.UserRole;
import com.middleware.zeus.bean.user.BeanUserRole;

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
     * 获取角色id
     *
     * @param userName 账户
     * @param organId 组织id
     * @param projectId 项目id
     * @return BeanSysRole
     */
    Integer getRoleId(String userName, String organId, String projectId);

    /**
     * 校验是否为超级管理员权限
     *
     * @param username 用户名
     * @return Boolean
     */
    Boolean checkAdmin(String username);

    /**
     * 获取绑定指定角色的用户
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
     * 获取用户角色关联关系
     *
     * @return List<BeanSysRole>
     */
    List<UserRole> list(String organId, String projectId);

    /**
     * 创建用户角色关联
     *
     * @param projectId  项目id
     * @param username   用户名
     * @param roleId     角色id
     */
    void insert(String organId, String projectId, String username, Integer roleId);

    /**
     * 删除用户角色关联
     *
     * @param userName 账户
     * @param organId 组织id
     * @param projectId  项目id
     * @param roleId 角色id
     */
    void delete(String userName, String organId, String projectId, Integer roleId);

    /**
     * 更新用户角色关联
     *
     * @param userRole 用户角色信息
     */
    void update(UserRole userRole);

    /**
     * 查询用户是否存在普通角色（即非超级管理员、非项目管理员）
     * @param userName
     * @return
     */
    boolean checkExistsNormalRole(String userName);

    /**
     * 查询用户项目角色
     * @param userName
     * @param projectId
     * @return
     */
    BeanUserRole get(String userName, String organId, String projectId);

    /**
     * 删除项目不存在角色信息
     * @param userName
     * @param projectIds
     */
    void deleteRedundantRole(String userName, List<String> projectIds);

}
