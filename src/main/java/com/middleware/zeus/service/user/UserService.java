package com.middleware.zeus.service.user;

import com.middleware.caas.common.model.MailUserDTO;
import com.middleware.caas.common.model.UploadImageFileDto;
import com.middleware.caas.common.model.user.ResourceMenuDto;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.zeus.bean.PersonalizedConfiguration;
import com.middleware.zeus.bean.user.BeanUser;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    UserDto getUserDto(String userName, String projectId);

    /**
     * 查询用户原始信息
     * @param userName
     * @return
     */
    BeanUser get(String userName);

    /**
     * 获取用户信息
     * @param userName 账户
     *
     * @return UserDto
     */
    UserDto getUserDto(String userName);

    /**
     * 获取用户列表
     * @param keyword 过滤词
     *
     * @return List<UserDto>
     */
    List<UserDto> list(String keyword);

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
     * @param projectId 项目id
     * @return List<ResourceMenuDto>
     */
    List<ResourceMenuDto> menu(String projectId) ;

    /**
     * 获取服务列表
     * @param clusterId 集群id
     * @param projectId 项目id
     * @return List<ResourceMenuDto>
     */
    List<ResourceMenuDto> listMiddlewareMenu(String clusterId, String projectId) ;

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

    /**
     * 切换项目
     * @param projectId 项目id
     *
     */
    void switchProject(String projectId, HttpServletResponse response);

    Map<String, String> getPower();
}
