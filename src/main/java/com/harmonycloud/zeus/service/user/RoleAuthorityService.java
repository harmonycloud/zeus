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

}
