package com.middleware.zeus.service.user;

import java.util.List;

import com.middleware.caas.common.model.user.ResourceMenuDto;
import com.middleware.caas.common.model.user.RoleDto;
import com.middleware.caas.common.model.user.UserDto;

/**
 * @author xutianhong
 * @Date 2021/7/27 2:54 下午
 */
public interface RoleService {

    /**
     * 新建角色
     * @param roleDto 角色对象
     *
     */
    void add(RoleDto roleDto);

    /**
     * 获取角色
     *
     * @param roleId 角色id
     * @return BeanSysRole
     */
    RoleDto get(Integer roleId);

    /**
     * 删除角色
     *
     * @param roleId 角色id
     */
    void delete(Integer roleId);

    /**
     * 获取角色
     * @param key 关键字查找
     *
     * @return List<BeanSysRole>
     */
    List<RoleDto> list(String key);

    /**
     * 更新角色
     *
     * @param roleDto 角色对象
     */
    void update(RoleDto roleDto);

    /**
     * 获取角色
     * @param userDto 用户
     * @param projectId 项目id
     *
     * @return List<ResourceMenuDto>
     */
    List<ResourceMenuDto> listMenuByRoleId(UserDto userDto, String projectId);


    /**
     * 初始化中间件权限
     *
     * @param type       类型
     */
    void initMiddlewareAuthority(String type);


}
