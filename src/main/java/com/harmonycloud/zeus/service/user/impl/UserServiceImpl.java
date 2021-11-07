package com.harmonycloud.zeus.service.user.impl;

import java.io.*;
import java.sql.Blob;
import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.zeus.bean.PersonalizedConfiguration;
import com.harmonycloud.zeus.dao.user.PersonalMapper;
import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.service.user.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

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

    @Autowired
    private PersonalMapper personalMapper;

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
        String roleId = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString("roleId");
        QueryWrapper<BeanUser> userWrapper = new QueryWrapper<>();
        // 非超级管理员角色用户 获取创建者为自身的用户
        if(!"1".equals(roleId)){
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
            beanUser.setPassword(PasswordUtils.md5("zeus123.com"));
        }
        beanUser.setCreator(currentUser.getUsername());
        beanUser.setCreateTime(new Date());
        beanUser.setPasswordTime(new Date());
        beanUserMapper.insert(beanUser);
    }

    @Override
    public void insertPersonalConfig(PersonalizedConfiguration configuration, MultipartFile backgroundFile, MultipartFile homeLogoFile, MultipartFile loginLoginFile,String status) throws Exception {
        if ("init".equals(status)) {
            QueryWrapper<PersonalizedConfiguration> query = new QueryWrapper<PersonalizedConfiguration>().eq("status","0");
            personalMapper.delete(query);
            return;
        }
        byte[] background = loadFile(backgroundFile);
        byte[] homeLogo = loadFile(homeLogoFile);
        byte[] loginLogo = loadFile(loginLoginFile);
        Date now = new Date();
        if (background != null) {
            configuration.setBackgroundImage(background);
            configuration.setBackgroundPath(backgroundFile.getOriginalFilename());
        }
        if (homeLogo != null) {
            configuration.setHomeLogo(homeLogo);
            configuration.setHomeLogoPath(homeLogoFile.getOriginalFilename());
        }
        if (loginLogo != null) {
            configuration.setLoginLogo(loginLogo);
            configuration.setLoginLogoPath(loginLoginFile.getOriginalFilename());
        }
        QueryWrapper<PersonalizedConfiguration> queryWrapper = new QueryWrapper<PersonalizedConfiguration>();
        List<PersonalizedConfiguration> personals = personalMapper.selectList(queryWrapper);
        if (personals.size() == 0) {
            configuration.setCreateTime(now);
            configuration.setStatus("1");
            personalMapper.insert(configuration);
        }else if (personals.size() == 1){
            configuration.setCreateTime(now);
            configuration.setStatus("0");
            personalMapper.insert(configuration);
        }else if (personals.size() == 2) {
            configuration.setUpdateTime(now);
            QueryWrapper<PersonalizedConfiguration> wrapper = new QueryWrapper<PersonalizedConfiguration>().eq("status","0");
            personalMapper.update(configuration,wrapper);
        }
    }

    @Override
    public PersonalizedConfiguration getPersonalConfig() throws IOException {
        QueryWrapper<PersonalizedConfiguration> queryWrapper = new QueryWrapper<PersonalizedConfiguration>();
        List<PersonalizedConfiguration> personals = personalMapper.selectList(queryWrapper);
        if (personals.size() > 1) {
            queryWrapper.eq("status","0");
        }
        PersonalizedConfiguration personal = personalMapper.selectOne(queryWrapper);
        personal.setBackgroundImage(null);
        personal.setHomeLogo(null);
        personal.setLoginLogo(null);
        return personal;
    }

    private byte[] loadFile(MultipartFile file) throws IOException {
        InputStream inPut = null;
        byte[] bus = null;
        byte[] by = null;
        if (Objects.nonNull(file) && !file.isEmpty()) {
            try {
                inPut = file.getInputStream();
                if (inPut != null) {
                    by = new byte[inPut.available()];
                    //在内存创建一个字节数组缓冲区
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int n;
                    //inPut.read(by)从(来源)输入流中(读取内容)读取的一定数量字节数,并将它们存储到(去处)缓冲区数组by中
                    while ((n = inPut.read(by)) != -1) {
                        bos.write(by, 0, n);  //参数：1、要输出的字节数组 2、从什么位置开始 3、多少个字节  写入此字节数组输出流
                    }
                    bus = bos.toByteArray();  //创建一个新分配的字节数组。数组的大小和当前输出流的大小，内容是当前输出流的拷贝。
                }
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                inPut.close();
            }
        }
        return bus;
    }

}
