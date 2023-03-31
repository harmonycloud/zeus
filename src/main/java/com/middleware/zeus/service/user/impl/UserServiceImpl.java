package com.middleware.zeus.service.user.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.SystemConfigKeyEnum;
import com.middleware.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.MailUserDTO;
import com.middleware.caas.common.model.UploadImageFileDto;
import com.middleware.caas.common.model.user.ResourceMenuDto;
import com.middleware.caas.common.model.user.SystemConfigDto;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.common.model.user.UserRole;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.tool.encrypt.PasswordUtils;
import com.middleware.tool.encrypt.RSAUtils;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.bean.BeanClusterMiddlewareInfo;
import com.middleware.zeus.bean.BeanMailToUser;
import com.middleware.zeus.bean.BeanSystemConfig;
import com.middleware.zeus.bean.PersonalizedConfiguration;
import com.middleware.zeus.bean.user.BeanUser;
import com.middleware.zeus.dao.BeanMailToUserMapper;
import com.middleware.zeus.dao.user.BeanUserMapper;
import com.middleware.zeus.dao.user.PersonalMapper;
import com.middleware.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.middleware.zeus.service.system.SystemConfigService;
import com.middleware.zeus.service.user.*;
import com.middleware.zeus.util.ApplicationUtil;
import com.middleware.zeus.util.RequestUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.middleware.zeus.service.user.abstractService.AbstractUserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.middleware.caas.common.constants.CommonConstant.NUM_TWO;
import static com.middleware.caas.common.constants.user.UserConstant.USERNAME;
import static com.middleware.caas.filters.base.GlobalKey.NUM_ROLE_ADMIN;
import static com.middleware.caas.filters.base.GlobalKey.USER_TOKEN;


/**
 * @author xutianhong
 * @Date 2021/7/22 1:52 下午
 */
@Service
@Slf4j
@Skyview(target = "zeus")
public class UserServiceImpl extends AbstractUserService implements UserService {


    @Autowired
    protected BeanUserMapper beanUserMapper;
    @Autowired
    protected RoleService roleService;
    @Autowired
    protected PersonalMapper personalMapper;
    @Autowired
    protected BeanMailToUserMapper beanMailToUserMapper;
    @Autowired
    protected ClusterMiddlewareInfoService clusterMiddlewareInfoService;
    @Autowired
    protected SystemConfigService systemConfigService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private OrganizationUserService organizationUserService;

    @Value("${system.user.passwordExpiredDate:90}")
    private Integer defaultPasswordExpiredDate;


    @Override
    public UserDto getUserDto(String userName, String projectId) {
        if (StringUtils.isEmpty(userName)) {
            userName = getUsername();
        }
        return getUserDto(userName);
    }

    @Override
    public BeanUser get(String userName) {
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userName);
        BeanUser beanUser = beanUserMapper.selectOne(wrapper);
        return beanUser;
    }

    @Override
    public void create(BeanUser beanUser) {
        beanUserMapper.insert(beanUser);
    }

    @Override
    public UserDto getUserDto(String userName) {
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userName);
        BeanUser beanUser = beanUserMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(beanUser)) {
            throw new BusinessException(ErrorMessage.USER_NOT_EXIT);
        }
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(beanUser, userDto);
        // 设置用户角色权限
        setUserRoleList(userName, userDto);
        if (!CollectionUtils.isEmpty(userDto.getUserRoleList())) {
            userDto.setIsAdmin(userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getRoleId() == 1));
        }
        return userDto;
    }

    @Override
    public List<UserDto> list(String keyword) {
        QueryWrapper<BeanUser> userWrapper = new QueryWrapper<>();
        // 非超级管理员角色用户 获取创建者为自身的用户
        List<BeanUser> beanUserList = beanUserMapper.selectList(userWrapper);
        // 获取用户项目下角色
        List<UserRole> userRoleList = userRoleService.list();
        // 获取用户组织下角色
        userRoleList.addAll(organizationUserService.listUserRole(null));
        Map<String, List<UserRole>> userRoleMap =
                userRoleList.stream().collect(Collectors.groupingBy(UserRole::getUserName));
        // 封装数据
        List<UserDto> userDtoList = beanUserList.stream().map(beanUser -> {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(beanUser, userDto, "password");
            userDto.setUserRoleList(userRoleMap.getOrDefault(beanUser.getUserName(), new ArrayList<>()));
            return userDto;
        }).collect(Collectors.toList());
        // 过滤
        if (StringUtils.isNotEmpty(keyword)) {
            userDtoList = userDtoList.stream()
                    .filter(userDto -> StringUtils.containsIgnoreCase(userDto.getUserName(), keyword)
                            || StringUtils.containsIgnoreCase(userDto.getAliasName(), keyword)
                            || StringUtils.containsIgnoreCase(userDto.getEmail(), keyword)
                            || StringUtils.containsIgnoreCase(userDto.getPhone(), keyword))
                    .collect(Collectors.toList());
        }
        return userDtoList;
    }

    @Override
    public void create(UserDto userDto) throws Exception {
        // 校验参数
        checkParams(userDto);
        // 校验用户是否已存在
        if (checkExist(userDto.getUserName())) {
            throw new BusinessException(ErrorMessage.USER_EXIST);
        }
        // 写入用户表
        insertUser(userDto);
        // 分配超级管理员角色
        if (userDto.getIsAdmin() != null) {
            bindAdmin(userDto);
        }
    }

    @Override
    public void update(UserDto userDto) throws Exception {
        // 校验参数
        checkParams(userDto);
        // 修改用户基本信息
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userDto.getUserName());
        BeanUser beanUser = new BeanUser();
        beanUser.setUserName(userDto.getUserName());
        beanUser.setAliasName(userDto.getAliasName());
        beanUser.setEmail(userDto.getEmail());
        beanUser.setPhone(userDto.getPhone());
        beanUserMapper.update(beanUser, wrapper);
        // 分配或删除超级管理员角色
        if (userDto.getIsAdmin() != null) {
            bindAdmin(userDto);
        }
    }

    @Override
    public void update(BeanUser beanUser) {
        beanUserMapper.updateById(beanUser);
    }

    @Override
    public Boolean delete(String userName) {
        // 校验用户是否存在
        if (!checkExist(userName)) {
            throw new BusinessException(ErrorMessage.USER_NOT_EXIT);
        }
        // 删除用户
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userName);
        beanUserMapper.delete(wrapper);
        // 删除用户角色关系
        userRoleService.delete(userName, null, null, null);
        // 从项目中移除
        projectService.unbindUser(null, null, userName);
        return true;
    }

    @Override
    public Boolean reset(String userName) {
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userName);
        BeanUser beanUser = beanUserMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(beanUser)) {
            throw new BusinessException(ErrorMessage.USER_NOT_EXIT);
        }
        beanUser.setPassword(PasswordUtils.md5("zeus123.com"));
        beanUser.setPasswordTime(new Date());
        beanUserMapper.updateById(beanUser);
        return true;
    }

    @Override
    public void bind(String userName, String role) {

    }

    @Override
    public void changePassword(String userName, String password, String newPassword, String reNewPassword)
            throws Exception {
        String dePassword = RSAUtils.decryptByPrivateKey(password);
        String deNewPassword = RSAUtils.decryptByPrivateKey(newPassword);
        String deReNewPassword = RSAUtils.decryptByPrivateKey(reNewPassword);
        if (!deNewPassword.equals(deReNewPassword)) {
            throw new BusinessException(ErrorMessage.PASSWORD_DO_NOT_MATCH);
        }

        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userName);
        BeanUser beanUser = beanUserMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(beanUser)) {
            throw new BusinessException(ErrorMessage.USER_NOT_EXIT);
        }
        String md5Password = PasswordUtils.md5(dePassword);
        if (!md5Password.equals(beanUser.getPassword())) {
            throw new BusinessException(ErrorMessage.WRONG_PASSWORD);
        }
        // 更新密码
        beanUser.setPassword(PasswordUtils.md5(deNewPassword));
        beanUser.setPasswordTime(new Date());
        beanUserMapper.updateById(beanUser);
    }

    @Override
    public MailUserDTO getUserList(String alertRuleId) {
        List<UserDto> userDtos = null;
        try {
            userDtos = list(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<UserDto> userDtoList = new ArrayList<>();
        if (StringUtils.isNotBlank(alertRuleId)) {
            QueryWrapper<BeanMailToUser> mailToUserQueryWrapper = new QueryWrapper<>();
            Integer alertId = Integer.parseInt(alertRuleId.replace("GJ", ""));
            mailToUserQueryWrapper.eq("alert_rule_id", alertId);
            List<BeanMailToUser> beanMailToUsers = beanMailToUserMapper.selectList(mailToUserQueryWrapper);
            userDtoList = beanMailToUsers.stream().map(mailToUser -> {
                BeanUser beanUser = beanUserMapper.selectOne(new QueryWrapper<BeanUser>().eq("id", mailToUser.getUserId()));
                List<UserRole> userRoleList = userRoleService.get(beanUser.getUserName());
                UserDto userDto = new UserDto();
                BeanUtils.copyProperties(beanUser, userDto);
                userDto.setUserRoleList(userRoleList);
                return userDto;
            }).collect(Collectors.toList());
        }
        MailUserDTO mailUserDTO = new MailUserDTO();
        return mailUserDTO.setUsers(userDtos).setUserBy(userDtoList);
    }

    @Override
    public void savePasswordExpiredDay(String days) {
        systemConfigService.saveConfig(SystemConfigKeyEnum.PASSWORD_EXPIRED_DAY.getNameKey(), days);
    }

    @Override
    public SystemConfigDto getPasswordExpiredDay() {
        BeanSystemConfig config = systemConfigService.getConfig(SystemConfigKeyEnum.PASSWORD_EXPIRED_DAY.getNameKey());
        if (config == null) {
            initDefaultPasswordExpiredDate();
            config = systemConfigService.getConfig(SystemConfigKeyEnum.PASSWORD_EXPIRED_DAY.getNameKey());
        }
        SystemConfigDto systemConfigDto = new SystemConfigDto();
        systemConfigDto.setConfigName(SystemConfigKeyEnum.PASSWORD_EXPIRED_DAY.getNameKey());
        systemConfigDto.setConfigValue(config.getConfigValue());
        return systemConfigDto;
    }

    @Override
    public UserRole getUserRole(String username, String organId, String projectId) {
        return userRoleService.getUserRole(username, organId, projectId);
    }

    @Override
    public Boolean checkAdmin(String username) {
        return null;
    }

    /**
     * 初始化密码有效期
     */
    private void initDefaultPasswordExpiredDate() {
        systemConfigService.saveConfig(SystemConfigKeyEnum.PASSWORD_EXPIRED_DAY.getNameKey(), String.valueOf(defaultPasswordExpiredDate));
    }

    /**
     * 写入用户表
     */
    public void insertUser(UserDto userDto) {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        BeanUser beanUser = new BeanUser();
        BeanUtils.copyProperties(userDto, beanUser);
        if (StringUtils.isEmpty(beanUser.getPassword())) {
            beanUser.setPassword(PasswordUtils.md5("zeus123.com"));
        }
        beanUser.setCreator(currentUser.getUsername());
        beanUser.setCreateTime(new Date());
        beanUser.setPasswordTime(new Date());
        beanUserMapper.insert(beanUser);
    }

    /**
     * 绑定或解绑超级管理员
     */
    public void bindAdmin(UserDto userDto) {
        //todo 注释权限判断代码   使所有超级管理员用户可操作分配超级管理员角色
        /*String username =
                JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue().getString(USERNAME);
        if (!ADMIN.equals(username)) {
            throw new BusinessException(ErrorMessage.NO_AUTHORITY);
        }*/
        if (userDto.getIsAdmin()) {
            userRoleService.insert(null, null, userDto.getUserName(), NUM_ROLE_ADMIN);
        } else {
            userRoleService.delete(userDto.getUserName(), null, null, NUM_ROLE_ADMIN);
        }
    }

    /**
     * 设置用户的角色列表
     *
     * @param userName 用户名
     * @param userDto
     */
    public void setUserRoleList(String userName, UserDto userDto) {
        // 获取项目下角色
        List<UserRole> userRoleList = userRoleService.get(userName);
        // 获取用户组织下角色
        userRoleList.addAll(organizationUserService.listByUsername(userName).stream()
                .filter(organizationUser -> organizationUser.getRoleId() != null).map(organizationUser -> {
                    UserRole userRole = new UserRole();
                    userRole.setUserName(organizationUser.getUsername());
                    userRole.setRoleName("组织管理员");
                    userRole.setOrganId(organizationUser.getOrganId());
                    userRole.setRoleId(organizationUser.getRoleId());
                    userRole.setWeight(2);
                    return userRole;
                }).collect(Collectors.toList()));
        if (!CollectionUtils.isEmpty(userRoleList)) {
            userDto.setUserRoleList(userRoleList);
            userDto.setIsAdmin(userRoleList.stream().anyMatch(userRole -> userRole.getRoleId() == 1));
        }
    }


}
