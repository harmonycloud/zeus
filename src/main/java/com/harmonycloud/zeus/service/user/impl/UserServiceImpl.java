package com.harmonycloud.zeus.service.user.impl;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.constants.LdapConfigConstant;
import com.harmonycloud.caas.common.model.MailUserDTO;
import com.harmonycloud.caas.common.model.UploadImageFileDto;
import com.harmonycloud.zeus.bean.MailToUser;
import com.harmonycloud.zeus.bean.PersonalizedConfiguration;
import com.harmonycloud.zeus.bean.user.BeanRole;
import com.harmonycloud.zeus.dao.MailToUserMapper;
import com.harmonycloud.zeus.dao.user.BeanRoleMapper;
import com.harmonycloud.zeus.dao.user.PersonalMapper;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.user.*;
import com.harmonycloud.zeus.util.ApplicationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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

import static com.harmonycloud.caas.common.constants.user.UserConstant.USERNAME;
import static com.harmonycloud.caas.filters.base.GlobalKey.USER_TOKEN;


/**
 * @author xutianhong
 * @Date 2021/7/22 1:52 下午
 */
@Service
@Component
@Slf4j
public class UserServiceImpl implements UserService {

    private static final Map<String, Object> IMAGE_MAP = new HashMap<>();
    @Autowired
    private BeanUserMapper beanUserMapper;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private PersonalMapper personalMapper;
    @Autowired
    private MailToUserMapper mailToUserMapper;

    @Override
    public UserDto getUserDto(String userName, String projectId) {
        if (StringUtils.isEmpty(userName)) {
            CurrentUser currentUser = CurrentUserRepository.getUser();
            userName = currentUser.getUsername();
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
        List<UserRole> userRoleList = userRoleService.get(userName);
        if (!CollectionUtils.isEmpty(userRoleList)) {
            userDto.setUserRoleList(userRoleList);
        }
        return userDto;
    }

    @Override
    public List<UserDto> list(String keyword) {
        QueryWrapper<BeanUser> userWrapper = new QueryWrapper<>();
        // 非超级管理员角色用户 获取创建者为自身的用户
        List<BeanUser> beanUserList = beanUserMapper.selectList(userWrapper);
        // 获取角色
        List<UserRole> userRoleList = userRoleService.list();
        Map<String, List<UserRole>> userRoleMap =
            userRoleList.stream().collect(Collectors.groupingBy(UserRole::getUserName));
        // 封装数据
        List<UserDto> userDtoList = beanUserList.stream().map(beanUser -> {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(beanUser, userDto, "password");
            userDto.setUserRoleList(userRoleMap.get(beanUser.getUserName()));
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
        /*if (userDto.getRoleId() != null) {
            userRoleService.update(userDto, null);
        }*/
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
        userRoleService.delete(userName, null);
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
    public List<ResourceMenuDto> menu(String clusterId) {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String username = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString(USERNAME);
        List<ResourceMenuDto> resourceMenuDtoList = roleService.listMenuByRoleId(username);

        Map<Integer, List<ResourceMenuDto>> resourceMenuDtoMap =
                resourceMenuDtoList.stream().collect(Collectors.groupingBy(ResourceMenuDto::getParentId));
        List<ResourceMenuDto> firstMenuList = resourceMenuDtoMap.get(0);
        resourceMenuDtoMap.remove(0);
        firstMenuList.forEach(firstMenu -> {
            if (!resourceMenuDtoMap.containsKey(firstMenu.getId())) {
                return;
            }
            firstMenu.setSubMenu(resourceMenuDtoMap.get(firstMenu.getId()));
            Collections.sort(firstMenu.getSubMenu());
        });
        if (StringUtils.isNotBlank(clusterId)) {
            setServiceMenuSubMenu(firstMenuList, clusterId);
        }
        Collections.sort(firstMenuList);
        return firstMenuList;
    }

    /**
     * 参数校验
     */
    public void checkParams(UserDto userDto) throws Exception {
        // 校验参数是否完全
        if (StringUtils.isAnyBlank(userDto.getUserName(), userDto.getAliasName())) {
            throw new IllegalArgumentException("username/aliasName should not be null");
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

    /**
     * 添加个性化配置相关信息
     * @param configuration
     * @param status
     * @throws Exception
     */
    @Override
    public void insertPersonalConfig(PersonalizedConfiguration configuration,String status) throws Exception {
        //判断是否要初始化
        if ("init".equals(status)) {
            QueryWrapper<PersonalizedConfiguration> query = new QueryWrapper<PersonalizedConfiguration>().eq("status","1");
            personalMapper.delete(query);
            return;
        }
        configuration.setStatus("1");
        checkout(configuration);
    }

    /**
     * 个性化配置相关图片上传
     * @param file
     * @throws IOException
     */
    @Override
    public UploadImageFileDto uploadFile(MultipartFile file) throws IOException {
        byte[] bytes = null;
        UploadImageFileDto uploadImageFileDto = new UploadImageFileDto();
        bytes = loadFile(file);
        uploadImageFileDto.setBytes(bytes);
        uploadImageFileDto.setType(file.getOriginalFilename().split("\\.")[1]);
        return uploadImageFileDto;
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
            QueryWrapper<MailToUser> mailToUserQueryWrapper = new QueryWrapper<>();
            Integer alertId = Integer.parseInt(alertRuleId.replace("GJ",""));
            mailToUserQueryWrapper.eq("alert_rule_id",alertId);
            List<MailToUser> mailToUsers = mailToUserMapper.selectList(mailToUserQueryWrapper);
            userDtoList = mailToUsers.stream().map(mailToUser -> {
                BeanUser beanUser = beanUserMapper.selectOne(new QueryWrapper<BeanUser>().eq("id",mailToUser.getUserId()));
                List<UserRole> userRoleList = userRoleService.get(beanUser.getUserName());
                UserDto userDto = new UserDto();
                BeanUtils.copyProperties(beanUser,userDto);
                userDto.setUserRoleList(userRoleList);
                return userDto;
            }).collect(Collectors.toList());
        }
        MailUserDTO mailUserDTO = new MailUserDTO();
        return mailUserDTO.setUsers(userDtos).setUserBy(userDtoList);
    }

    @Override
    public void switchProject(String projectId, HttpServletResponse response) {
        JSONObject userMap = JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue();
        userMap.put("projectId", projectId);
        long currentTime = System.currentTimeMillis();
        response.setHeader(USER_TOKEN, JwtTokenComponent.generateToken("userInfo", userMap,
            new Date(currentTime + (long)(ApplicationUtil.getExpire() * 3600000L)), new Date(currentTime - 300000L)));
    }

    /**
     * 获取个性化配置信息
     * @return
     * @throws IOException
     */
    @Override
    public PersonalizedConfiguration getPersonalConfig() {
        QueryWrapper<PersonalizedConfiguration> queryWrapper = new QueryWrapper<PersonalizedConfiguration>();
        List<PersonalizedConfiguration> personals = personalMapper.selectList(queryWrapper);
        if (personals.size() > 1) {
            queryWrapper.eq("status","1");
            List<PersonalizedConfiguration> personalList = personalMapper.selectList(queryWrapper);
            for (PersonalizedConfiguration personalizedConfiguration : personalList) {
                return personalizedConfiguration;
            }
        }
        for (PersonalizedConfiguration personalizedConfiguration : personals) {
            return personalizedConfiguration;
        }
        return new PersonalizedConfiguration();
    }

    /**
     * 将文件转为二进制数组
     * @param file
     * @return
     * @throws IOException
     */
    private byte[] loadFile(MultipartFile file) throws IOException {
        InputStream inPut = null;
        byte[] bus = null;
        byte[] by = null;
        if (Objects.nonNull(file) && !file.isEmpty()) {
            try {
                inPut = file.getInputStream();
                if (inPut != null) {
                    by = new byte[inPut.available()];
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int n;
                    //inPut.read(by)从(来源)输入流中(读取内容)读取的一定数量字节数,并将它们存储到(去处)缓冲区数组by中
                    while ((n = inPut.read(by)) != -1) {
                        bos.write(by, 0, n);
                    }
                    bus = bos.toByteArray();
                }
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                inPut.close();
            }
        }
        String voiceBase64= Base64.getEncoder().encodeToString(bus);
        return Base64.getDecoder().decode(voiceBase64);
    }

    /**
     * 校验数据库数据
     * @param configuration
     */
    private void checkout(PersonalizedConfiguration configuration) {
        QueryWrapper<PersonalizedConfiguration> queryWrapper = new QueryWrapper<PersonalizedConfiguration>();
        queryWrapper.eq("status","1");
        List<PersonalizedConfiguration> personals = personalMapper.selectList(queryWrapper);
        Date date = new Date();
        if (personals.size() == 0) {
            configuration.setCreateTime(date);
            personalMapper.insert(configuration);
        }else {
            configuration.setUpdateTime(date);
            queryWrapper.eq("status","1");
            personalMapper.update(configuration,queryWrapper);
        }
    }

    /**
     * @description 将中间件设为服务列表菜单的子菜单
     * @author  liyinlong
     * @since 2021/11/2 4:09 下午
     */
    public void setServiceMenuSubMenu(List<ResourceMenuDto> menuDtos, String clusterId) {
        menuDtos.forEach(parentMenu -> {
            if ("serviceList".equals(parentMenu.getName())) {
                List<ResourceMenuDto> resourceMenuDtos = middlewareService.listAllMiddlewareAsMenu(clusterId);
                resourceMenuDtos.forEach(resourceMenuDto -> {
                    resourceMenuDto.setAliasName(MiddlewareOfficialNameEnum.findByMiddlewareName(resourceMenuDto.getAliasName()));
                    resourceMenuDto.setUrl(parentMenu.getUrl() + "/" + resourceMenuDto.getName() +"/" + resourceMenuDto.getAliasName());
                });
                parentMenu.setSubMenu(resourceMenuDtos);
                return;
            }
        });
    }
}
