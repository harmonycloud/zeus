package com.harmonycloud.zeus.service.user;

import com.harmonycloud.zeus.bean.user.BeanResourceMenuRole;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/7/29 9:37 上午
 */
public interface ResourceMenuRoleService {

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
    void add(Integer roleId, Integer resourceMenuId);

}
