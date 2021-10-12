package com.harmonycloud.zeus.service.user.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.ADMIN;
import static com.harmonycloud.caas.filters.base.GlobalKey.USER_TOKEN;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.service.user.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.caas.common.model.user.UserRole;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.zeus.bean.user.BeanUser;
import com.harmonycloud.zeus.dao.user.BeanUserMapper;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import com.harmonycloud.tool.encrypt.RSAUtils;

/**
 * @author xutianhong
 * @Date 2021/7/22 1:52 下午
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private BeanUserMapper beanUserMapper;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleService roleService;

    @Override
    public UserDto get(String userName) throws Exception {
        if (StringUtils.isEmpty(userName)) {
            CurrentUser currentUser = CurrentUserRepository.getUser();
            userName = currentUser.getUsername();
        }
        return get(userName, false);
    }

    @Override
    public UserDto get(String userName, Boolean withPassword) throws Exception {
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userName);
        BeanUser beanUser = beanUserMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(beanUser)) {
            throw new BusinessException(ErrorMessage.USER_NOT_EXIT);
        }
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(beanUser, userDto);
        UserRole userRole = userRoleService.get(userName);
        if (userRole != null) {
            userDto.setRoleId(userRole.getRoleId()).setRoleName(userRole.getRoleName());
        }
        if (!withPassword) {
            userDto.setPassword(null);
        }
        return userDto;
    }

    @Override
    public List<UserDto> list(String keyword) throws Exception {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        QueryWrapper<BeanUser> userWrapper = new QueryWrapper<>();
        // 获取创建者为自身的用户
        if(!"admin".equals(currentUser.getUsername())){
            userWrapper.eq("creator", currentUser.getUsername());
        }
        List<BeanUser> beanUserList = beanUserMapper.selectList(userWrapper);
        // 获取角色
        List<UserRole> userRoleList = userRoleService.list(beanUserList);
        Map<String, UserRole> userRoleMap =
            userRoleList.stream().collect(Collectors.toMap(UserRole::getUserName, ur -> ur));
        // 封装数据
        List<UserDto> userDtoList = beanUserList.stream().map(beanUser -> {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(beanUser, userDto, "password");
            userDto.setRoleId(userRoleMap.get(beanUser.getUserName()).getRoleId());
            userDto.setRoleName(userRoleMap.get(beanUser.getUserName()).getRoleName());
            return userDto;
        }).collect(Collectors.toList());
        // 过滤
        if (StringUtils.isNotEmpty(keyword)) {
            userDtoList = userDtoList.stream()
                .filter(userDto -> StringUtils.containsIgnoreCase(userDto.getUserName(), keyword)
                    || StringUtils.containsIgnoreCase(userDto.getAliasName(), keyword)
                    || StringUtils.containsIgnoreCase(userDto.getEmail(), keyword)
                    || StringUtils.containsIgnoreCase(userDto.getPhone(), keyword)
                    || StringUtils.containsIgnoreCase(userDto.getRoleName(), keyword))
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
    }

    @Override
    public void update(UserDto userDto) throws Exception {
        // 校验参数
        checkParams(userDto);

        UserDto targetUser = this.get(userDto.getUserName(), false);
        String currentUser = CurrentUserRepository.getUser().getUsername();
        if (targetUser.getRoleId() != null && targetUser.getRoleId() == 1
            && !currentUser.equals(targetUser.getUserName())) {
            throw new BusinessException(ErrorMessage.NO_AUTHORITY);
        }
        // 修改用户基本信息
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userDto.getUserName());
        BeanUser beanUser = new BeanUser();
        beanUser.setUserName(userDto.getUserName());
        beanUser.setAliasName(userDto.getAliasName());
        beanUser.setEmail(userDto.getEmail());
        beanUser.setPhone(userDto.getPhone());
        beanUserMapper.update(beanUser, wrapper);
        // 修改角色
        if (userDto.getRoleId() != null) {
            userRoleService.update(userDto);
        }
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
        userRoleService.delete(userName);
        return true;
    }

    @Override
    public Boolean reset(String userName) {
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userName);
        BeanUser beanUser = beanUserMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(beanUser)) {
            throw new BusinessException(ErrorMessage.USER_NOT_EXIT);
        }
        beanUser.setPassword(PasswordUtils.md5("Ab123456!"));
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
    public List<ResourceMenuDto> menu() {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String roleId = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString("roleId");
        List<ResourceMenuDto> resourceMenuDtoList = roleService.listMenuByRoleId(roleId);

        Map<Integer, List<ResourceMenuDto>> resourceMenuDtoMap =
            resourceMenuDtoList.stream().collect(Collectors.groupingBy(ResourceMenuDto::getParentId));
        List<ResourceMenuDto> firstMenuList = resourceMenuDtoMap.get(0);
        resourceMenuDtoMap.remove(0);
        firstMenuList.forEach(firstMenu -> {
            if (!resourceMenuDtoMap.containsKey(firstMenu.getId())){
                return;
            }
            firstMenu.setSubMenu(resourceMenuDtoMap.get(firstMenu.getId()));
            Collections.sort(firstMenu.getSubMenu());
        });
        Collections.sort(firstMenuList);
        return firstMenuList;
    }

    /**
     * 参数校验
     */
    public void checkParams(UserDto userDto) throws Exception {
        // 校验参数是否完全
        if (StringUtils.isAnyBlank(userDto.getUserName(), userDto.getAliasName(), userDto.getPhone())) {
            throw new IllegalArgumentException("username/aliasName/phone should not be null");
        }
    }

    /**
     * 校验用户是否已经存在
     * 
     * @return true 已存在; false 不存在
     */
    public boolean checkExist(String userName) {
        QueryWrapper<BeanUser> wrapper = new QueryWrapper<BeanUser>().eq("username", userName);
        BeanUser beanUser = beanUserMapper.selectOne(wrapper);
        return !ObjectUtils.isEmpty(beanUser);
    }

    /**
     * 写入用户表
     */
    public void insertUser(UserDto userDto) {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        BeanUser beanUser = new BeanUser();
        BeanUtils.copyProperties(userDto, beanUser);
        if (StringUtils.isEmpty(beanUser.getPassword())) {
            beanUser.setPassword(PasswordUtils.md5("Ab123456!"));
        }
        beanUser.setCreator(currentUser.getUsername());
        beanUser.setCreateTime(new Date());
        beanUser.setPasswordTime(new Date());
        beanUserMapper.insert(beanUser);
    }

}
