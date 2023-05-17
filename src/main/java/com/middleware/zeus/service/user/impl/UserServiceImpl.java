package com.middleware.zeus.service.user.impl;

import static com.middleware.zeus.common.constants.CommonConstant.NUM_ONE;
import static com.middleware.zeus.common.constants.user.UserConstant.ADMIN;
import static com.middleware.zeus.common.constants.user.UserConstant.USERNAME;
import static com.middleware.caas.filters.base.GlobalKey.NUM_ROLE_ADMIN;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.zeus.bean.user.BeanOrganization;
import com.middleware.zeus.bean.user.BeanProject;
import com.middleware.zeus.dao.user.BeanOrganizationMapper;
import com.middleware.zeus.dao.user.BeanProjectMapper;
import com.middleware.zeus.service.user.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.SystemConfigKeyEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.MailUserDTO;
import com.middleware.zeus.common.model.user.SystemConfigDto;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.common.model.user.UserRole;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.util.encrypt.PasswordUtils;
import com.middleware.zeus.util.encrypt.RSAUtils;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.bean.BeanMailToUser;
import com.middleware.zeus.bean.BeanSystemConfig;
import com.middleware.zeus.bean.user.BeanUser;
import com.middleware.zeus.dao.BeanMailToUserMapper;
import com.middleware.zeus.dao.user.BeanUserMapper;
import com.middleware.zeus.dao.user.PersonalMapper;
import com.middleware.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.middleware.zeus.service.system.SystemConfigService;
import com.middleware.zeus.service.user.abstractService.AbstractUserService;

import lombok.extern.slf4j.Slf4j;


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
    @Autowired
    private BeanOrganizationMapper beanOrganizationMapper;
    @Autowired
    private BeanProjectMapper beanProjectMapper;

    @Value("${system.user.passwordExpiredDate:90}")
    private Integer defaultPasswordExpiredDate;


    @Override
    public UserDto getUserDto(String userName, String projectId, boolean roleDetail) {
        if (StringUtils.isEmpty(userName)) {
            userName = getUsername();
        }
        return getUserDto(userName, roleDetail).setPassword(null);
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
    public UserDto getUserDto(String userName, boolean roleDetail) {
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userName);
        BeanUser beanUser = beanUserMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(beanUser)) {
            throw new BusinessException(ErrorMessage.USER_NOT_EXIT);
        }
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(beanUser, userDto);
        // 设置用户角色权限
        if (roleDetail) {
            setUserRoleList(userName, userDto);
            if (!CollectionUtils.isEmpty(userDto.getUserRoleList())) {
                userDto.setIsAdmin(userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getRoleId() == 1));
            }
        }
        return userDto;
    }

    @Override
    public List<UserDto> list(String keyword) {
        QueryWrapper<BeanUser> userWrapper = new QueryWrapper<>();
        // 非超级管理员角色用户 获取创建者为自身的用户
        List<BeanUser> beanUserList = beanUserMapper.selectList(userWrapper);
        // 获取超级管理员用户列表
        Map<String, UserRole> adminMap = userRoleService.findByRoleId(NUM_ONE).stream().collect(Collectors.toMap(UserRole::getUserName, ur -> ur));
        // 封装数据
        List<UserDto> userDtoList = beanUserList.stream().map(beanUser -> {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(beanUser, userDto, "password");
            if (adminMap.containsKey(beanUser.getUserName())){
                userDto.setIsAdmin(true);
            }
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
        if (userDto.getManager() != null) {
            bindManager(userDto);
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
        // 分配或删除管理员角色
        bindManager(userDto);
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
                List<UserRole> userRoleList = userRoleService.get(beanUser.getUserName(), null, null);
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
    public List<UserDto> getUserRole(List<UserDto> userDtoList) {
        // 获取用户项目下角色
        List<UserRole> userRoleList = userRoleService.list();
        // 获取用户组织下角色
        userRoleList.addAll(organizationUserService.listUserRole(null));
        Map<String, List<UserRole>> userRoleMap =
            userRoleList.stream().collect(Collectors.groupingBy(UserRole::getUserName));
        return userDtoList.stream()
            .peek(
                userDto -> userDto.setUserRoleList(userRoleMap.getOrDefault(userDto.getUserName(), new ArrayList<>())))
            .collect(Collectors.toList());
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
    public void bindManager(UserDto userDto) {
        // 查询该用户是否存在管理类型角色
        List<UserRole> userRoleList = userRoleService.get(userDto.getUserName(), null, null).stream()
            .filter(
                userRole -> StringUtils.isEmpty(userRole.getOrganId()) && StringUtils.isEmpty(userRole.getProjectId()))
            .collect(Collectors.toList());
        Integer currentManagerRoleId = -1;
        if (!CollectionUtils.isEmpty(userRoleList)) {
            currentManagerRoleId = userRoleList.get(0).getRoleId();
        }
        // 期望分配管理类型角色
        if (userDto.getManager() != null) {
            if (userDto.getManager().equals(NUM_ONE)) {
                String username = JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue()
                    .getString(USERNAME);
                if (ADMIN.equals(username)) {
                    userRoleService.update(new UserRole().setUserName(userDto.getUserName()).setRoleId(NUM_ROLE_ADMIN));
                }
            } else {
                userRoleService.update(new UserRole().setUserName(userDto.getUserName()).setRoleId(userDto.getManager()));
            }
            // 移除或者不期望分配管理类型角色
        } else if (currentManagerRoleId != -1) {
            if (currentManagerRoleId == NUM_ROLE_ADMIN) {
                String username = JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue()
                    .getString(USERNAME);
                if (ADMIN.equals(username)) {
                    userRoleService.delete(userDto.getUserName(), null, null, NUM_ROLE_ADMIN);
                }
            } else {
                userRoleService.delete(userDto.getUserName(), null, null, currentManagerRoleId);
            }
        }
    }

    /**
     * 设置用户的角色列表
     *
     * @param userName 用户名
     * @param userDto
     */
    public void setUserRoleList(String userName, UserDto userDto) {
        Map<String, String> organizationMap =
            beanOrganizationMapper.selectList(new QueryWrapper<>()).stream()
                .collect(Collectors.toMap(BeanOrganization::getOrganId, BeanOrganization::getName));
        Map<String, String> projectMap = beanProjectMapper.selectList(new QueryWrapper<>()).stream()
                .collect(Collectors.toMap(BeanProject::getProjectId, BeanProject::getName));
        // 获取项目下角色
        List<UserRole> userRoleList = userRoleService.get(userName, null, null);
        // 获取用户组织下角色
        userRoleList.addAll(organizationUserService.listByUsername(userName).stream()
            .filter(organizationUser -> organizationUser.getRoleId() != null).map(organizationUser -> {
                UserRole userRole = new UserRole();
                userRole.setUserName(organizationUser.getUsername());
                userRole.setRoleName("组织管理员");
                userRole.setOrganId(organizationUser.getOrganId());
                userRole.setRoleId(organizationUser.getRoleId());
                userRole.setWeight(2);
                if (organizationUser.getOrganId() != null) {
                    userRole.setOrganName(organizationMap.get(organizationUser.getOrganId()));
                }
                return userRole;
            }).collect(Collectors.toList()));
        convertManagerInfo(userDto, userRoleList);
        if (!CollectionUtils.isEmpty(userDto.getUserRoleList())) {
            userDto.getUserRoleList().forEach(ur -> {
                if (ur.getOrganId() != null) {
                    ur.setOrganName(organizationMap.get(ur.getOrganId()));
                }
                if (ur.getProjectId() != null) {
                    ur.setProjectName(projectMap.get(ur.getProjectId()));
                }
            });
        }
    }

    /**
     * 设置用户的管理型角色信息
     *
     * @param userDto 鱼护对象
     * @param userRoleList 用户角色信息列表
     */
    private void convertManagerInfo(UserDto userDto, List<UserRole> userRoleList){
        if (!CollectionUtils.isEmpty(userRoleList)) {
            userDto.setUserRoleList(userRoleList);
            // 获取管理型角色
            userDto.setIsAdmin(false);
            List<UserRole> managerList = userRoleList.stream().filter(
                    userRole -> StringUtils.isEmpty(userRole.getOrganId()) && StringUtils.isEmpty(userRole.getProjectId()))
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(managerList)) {
                Integer roleId = managerList.get(0).getRoleId();
                userDto.setManager(roleId);
                userDto.setIsAdmin(roleId == 1);
            }
        }
    }


}
