package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.MailUserDTO;
import com.harmonycloud.caas.common.model.UploadImageFileDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareAlertsDTO;
import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.zeus.bean.PersonalizedConfiguration;
import com.harmonycloud.zeus.bean.user.BeanUser;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    UserDto getUserDto(String userName) throws Exception;

    /**
     * 查询用户原始信息
     * @param userName
     * @return
     */
    BeanUser get(String userName);

    /**
     * 获取用户信息
     * @param userName 账户
     * @param withPassword 携带密码
     *
     * @return UserDto
     */
    UserDto getUserDto(String userName, Boolean withPassword) throws Exception;

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
     * 创建用户
     * @param beanUser
     */
    void create(BeanUser beanUser);

    /**
     * 更新用户信息
     * @param userDto 用户对象
     */
    void update(UserDto userDto) throws Exception;

    /**
     * 更新用户信息
     * @param beanUser
     */
    void update(BeanUser beanUser) ;

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
     * @param clusterId
     * @return
     * @throws Exception
     */
    List<ResourceMenuDto> menu(String clusterId) throws Exception;

    /**
     * 个性化配置
     */
    void insertPersonalConfig(PersonalizedConfiguration configuration,String status) throws Exception;

    /**
     * 获取个性化配置信息
     * @return
     * @throws IOException
     */
    PersonalizedConfiguration getPersonalConfig() throws IOException;

    /**
     * 上传图片
     * @param file
     * @throws IOException
     */
    UploadImageFileDto uploadFile(MultipartFile file) throws IOException;

    MailUserDTO getUserList(String alertruleId);
}
