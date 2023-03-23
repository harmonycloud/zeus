package com.middleware.zeus.service.user;

import com.middleware.caas.common.model.user.UserRole;
import com.middleware.zeus.bean.user.BeanOrganizationUser;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/10 2:46 下午
 */
public interface OrganizationUserService {

    /**
     * 获取组织用户列表
     * @param organId 组织id
     *
     * @return List<BeanOrganizationUser>
     */
    List<BeanOrganizationUser> list(String organId);

    /**
     * 根据用户名查询组织列表
     * @param username 用户名
     *
     * @return List<BeanOrganizationUser>
     */
    List<BeanOrganizationUser> listByUsername(String username);

    /**
     * 绑定用户组织关系
     * @param organId 组织id
     * @param username 用户名
     * @param roleId 角色id
     *
     */
    void insert(String organId, String username, Integer roleId);

    /**
     * 更新用户组织内角色
     * @param organId 组织id
     * @param username 用户名
     * @param roleId 角色id
     *
     */
    void update(String organId, String username, Integer roleId);

    /**
     * 更新用户组织内角色
     * @param organId 组织id
     * @param username 用户名
     *
     */
    void delete(String organId, String username);

    /**
     * 获取组织用户列表
     * @param organId 组织id
     *
     * @return List<BeanOrganizationUser>
     */
    List<UserRole> listUserRole(String organId);


}
