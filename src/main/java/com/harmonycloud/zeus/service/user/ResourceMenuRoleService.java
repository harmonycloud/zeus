package com.harmonycloud.zeus.service.user;

import com.harmonycloud.zeus.bean.user.BeanResourceMenuRole;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/7/29 9:37 上午
 */
public interface ResourceMenuRoleService {

    /**
     * 初始化角色菜单权限
     * @param roleId 角色id
     *
     */
    void init(Integer roleId);

    /**
     * 获取admin角色菜单映照列表
     *
     * @return List<BeanResourceMenuRole>
     */
    List<BeanResourceMenuRole> listAdminMenu();

    /**
     * 获取角色菜单映照列表
     * @param roleId 角色id
     *
     * @return List<BeanResourceMenuRole>
     */
    List<BeanResourceMenuRole> list(String roleId);

    /**
     * 添加角色菜单映照列表
     *
     * @param roleId 角色id
     * @param resourceMenuId 菜单id
     *
     */
    void add(Integer roleId, Integer resourceMenuId, Boolean available);

    /**
     * 删除角色菜单映照列表
     *
     * @param roleId 角色id
     *
     */
    void delete(Integer roleId);

    /**
     * 更新角色菜单映照列表
     *
     * @param roleId 角色id
     * @param resourceMenuId 菜单id
     *
     */
    void update(Integer roleId, Integer resourceMenuId, Boolean available);

    /**
     * 根据是否拥有运维权限更新角色菜单绑定关系
     *
     * @param roleId 角色id
     * @param ops 是否拥有运维权限
     *
     */
    void updateOpsMenu(Integer roleId, boolean ops);

}
