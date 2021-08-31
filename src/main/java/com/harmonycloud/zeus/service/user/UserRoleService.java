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
    UserRole get(String userName) throws Exception;

    /**
     * 获取角色
     *
     * @return List<BeanSysRole>
     */
    List<UserRole> list(List<BeanUser> beanUserList);

    /**
     * 创建用户角色关联
     *
     * @param userDto 用户信息
     */
    void insert(UserDto userDto);

    /**
     * 删除用户角色关联
     *
     * @param userName 账户
     */
    void delete(String userName);

    /**
     * 创建用户角色关联
     *
     * @param userDto 用户信息
     */
    void update(UserDto userDto);

}
