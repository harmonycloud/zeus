package com.middleware.zeus.skyview.v2.service;

import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.common.model.user.UserDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/21 7:04 下午
 */
public interface V2UserService {

    /**
     * 获取指定用户信息
     * @param username 用户名
     *
     * @return UserDto
     */
    UserDto get(String username);

    /**
     * 获取用户信息
     *
     * @return List<UserDto>
     */
    List<UserDto> list();



}
