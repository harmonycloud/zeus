package com.harmonycloud.zeus.service.user;

import com.harmonycloud.zeus.bean.user.BeanRoleAuthority;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/3/28 5:21 下午
 */
public interface RoleAuthorityService {

    /**
     * 添加
     *
     * @param roleId     角色id
     * @param type       类型
     * @param power      能力
     */
    void insert(Integer roleId, String type, String power);

    /**
     * 添加
     *
     * @param roleId     角色id
     */
    void delete(Integer roleId);

    /**
     * 获取列表
     *
     * @param roleId     角色id
     */
    List<BeanRoleAuthority> list(Integer roleId);

    /**
     * 更新角色权限
     *
     * @param roleId     角色id
     * @param power      能力
     */
    void update(Integer roleId, Map<String, String> power);

    /**
     * 通过类型校验该权限是否已初始化
     *
     * @param type       类型
     * @return Boolean
     */
    Boolean checkExistByType(String type);

    /**
     * 通过角色id确认是否拥有运维权限
     *
     * @param roleId     角色id
     * @param type       类型
     * @return Boolean
     */
    Boolean checkOps(String roleId, String type);

}
