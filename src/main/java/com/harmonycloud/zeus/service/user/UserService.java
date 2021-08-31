package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.caas.common.model.user.UserDto;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/7/22 1:50 下午
 */
public interface UserService {

    /**
     * 获取用户信息
     * @param userName 账户
     *
     * @return UserDto
     */
    UserDto get(String userName) throws Exception;

    /**
     * 获取用户信息
     * @param userName 账户
     * @param withPassword 携带密码
     *
     * @return UserDto
     */
    UserDto get(String userName, Boolean withPassword) throws Exception;

    /**
     * 获取用户列表
     * @param keyword 过滤词
     *
     * @return List<UserDto>
     */
    List<UserDto> list(String keyword) throws Exception;

    /**
     * 创建用户
     * @param userDto 用户对象
     *
     */
    void create(UserDto userDto) throws Exception;

    /**
     * 更新用户信息
     * @param userDto 用户对象
     */
    void update(UserDto userDto) throws Exception;

    /**
     * 删除用户
     * @param userName 账户
     *
     * @return boolean
     */
    Boolean delete(String userName);

    /**
     * 重制密码
     * @param userName 账户
     *
     * @return boolean
     */
    Boolean reset(String userName);

    /**
     * 绑定角色
     * @param userName 账户
     * @param role 角色
     *
     */
    void bind(String userName, String role);

    /**
     * 绑定角色
     * @param userName 账户
     * @param password 密码
     * @param newPassword 新密码
     * @param reNewPassword 二次新密码
     */
    void changePassword(String userName, String password, String newPassword, String reNewPassword) throws Exception;

    /**
     * 获取菜单栏
     */
    List<ResourceMenuDto> menu(HttpServletRequest request) throws Exception;

}
