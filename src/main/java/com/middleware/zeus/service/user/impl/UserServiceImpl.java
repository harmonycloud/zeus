package com.middleware.zeus.service.user.impl;

import static com.middleware.caas.common.constants.CommonConstant.NUM_TWO;
import static com.middleware.caas.common.constants.user.UserConstant.ADMIN;
import static com.middleware.caas.common.constants.user.UserConstant.USERNAME;
import static com.harmonycloud.caas.filters.base.GlobalKey.NUM_ROLE_ADMIN;
import static com.harmonycloud.caas.filters.base.GlobalKey.USER_TOKEN;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import com.middleware.zeus.service.user.UserRoleService;
import com.middleware.zeus.service.user.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.MailUserDTO;
import com.middleware.caas.common.model.UploadImageFileDto;
import com.middleware.caas.common.model.user.ResourceMenuDto;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.common.model.user.UserRole;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.middleware.tool.encrypt.PasswordUtils;
import com.middleware.tool.encrypt.RSAUtils;
import com.middleware.zeus.bean.BeanClusterMiddlewareInfo;
import com.middleware.zeus.bean.BeanMailToUser;
import com.middleware.zeus.bean.PersonalizedConfiguration;
import com.middleware.zeus.bean.user.BeanUser;
import com.middleware.zeus.dao.BeanMailToUserMapper;
import com.middleware.zeus.dao.user.BeanUserMapper;
import com.middleware.zeus.dao.user.PersonalMapper;
import com.middleware.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.service.user.RoleService;
import com.middleware.zeus.util.ApplicationUtil;
import com.middleware.zeus.util.RequestUtil;

import lombok.extern.slf4j.Slf4j;


/**
 * @author xutianhong
 * @Date 2021/7/22 1:52 下午
 */
@Service
@Component
@Slf4j
@ConditionalOnProperty(value="system.usercenter",havingValue = "zeus")
public class UserServiceImpl implements UserService {

    @Autowired
    private BeanUserMapper beanUserMapper;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PersonalMapper personalMapper;
    @Autowired
    private BeanMailToUserMapper beanMailToUserMapper;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ClusterMiddlewareInfoService clusterMiddlewareInfoService;

    public String getUsername() {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        return currentUser.getUsername();
    }

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
        // 获取角色
        List<UserRole> userRoleList = userRoleService.list();
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
        userRoleService.delete(userName, null, null);
        // 从项目中移除
        projectService.unbindUser(null, userName);
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

    /**
     * 查询菜单信息
     *
     * @param projectId
     * @return
     */
    @Override
    public List<ResourceMenuDto> menu(String projectId) {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String username = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString(USERNAME);
        UserDto userDto = getUserDto(username);
        List<ResourceMenuDto> resourceMenuDtoList = roleService.listMenuByRoleId(userDto, projectId);

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
        Collections.sort(firstMenuList);
        return firstMenuList;
    }

    @Override
    public List<ResourceMenuDto> listMiddlewareMenu(String clusterId, String projectId) {
        // 获取集群下所有中间件
        List<BeanClusterMiddlewareInfo> middlewareInfos = clusterMiddlewareInfoService.list(clusterId, false);
        // 过滤状态为未安装的中间件
        middlewareInfos = middlewareInfos.stream().filter(mwInfo -> !mwInfo.getStatus().equals(NUM_TWO)).collect(Collectors.toList());

        // 查询用户角色项目权限
        String username =
                JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue().getString(USERNAME);
        UserDto userDto = getUserDto(username);
        Map<String, String> power = new HashMap<>();
        if (!userDto.getIsAdmin() && userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getProjectId().equals(projectId))){
            power.putAll(userDto.getUserRoleList().stream().filter(userRole -> userRole.getProjectId().equals(projectId))
                    .collect(Collectors.toList()).get(0).getPower());
        }
        
        // 过滤获取拥有权限的中间件
        if (!CollectionUtils.isEmpty(power)) {
            middlewareInfos = middlewareInfos.stream()
                .filter(mwInfo -> power.keySet().stream()
                    .anyMatch(key -> !"0000".equals(power.get(key)) && mwInfo.getChartName().equals(key)))
                .collect(Collectors.toList());
        }

        // 封装数据
        List<ResourceMenuDto> subMenuList = new ArrayList<>();
        for (BeanClusterMiddlewareInfo middlewareInfoDTO : middlewareInfos) {
            ResourceMenuDto resourceMenuDto = new ResourceMenuDto();
            resourceMenuDto.setName(middlewareInfoDTO.getChartName());
            resourceMenuDto.setAliasName(MiddlewareOfficialNameEnum.findByChartName(middlewareInfoDTO.getChartName()));
            resourceMenuDto.setAvailable(true);
            resourceMenuDto.setUrl("serviceList/" + resourceMenuDto.getName() + "/" + resourceMenuDto.getAliasName());
            subMenuList.add(resourceMenuDto);
        }
        return subMenuList;
    }

    /**
     * 参数校验
     */
    public void checkParams(UserDto userDto) {
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
     *
     * @param configuration
     * @param status
     * @throws Exception
     */
    @Override
    public void insertPersonalConfig(PersonalizedConfiguration configuration, String status) throws Exception {
        //判断是否要初始化
        if ("init".equals(status)) {
            QueryWrapper<PersonalizedConfiguration> query = new QueryWrapper<PersonalizedConfiguration>().eq("status", "1");
            personalMapper.delete(query);
            return;
        }
        configuration.setStatus("1");
        checkout(configuration);
    }

    /**
     * 个性化配置相关图片上传
     *
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
    public void switchProject(String projectId, HttpServletResponse response) {
        JSONObject userMap = JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue();
        userMap.put("projectId", projectId);
        long currentTime = System.currentTimeMillis();
        response.setHeader(USER_TOKEN, JwtTokenComponent.generateToken("userInfo", userMap,
                new Date(currentTime + (long) (ApplicationUtil.getExpire() * 3600000L)), new Date(currentTime - 300000L)));
    }

    @Override
    public Map<String, String> getPower() {
        String projectId = RequestUtil.getProjectId();
        if (StringUtils.isNotEmpty(projectId)) {
            JSONObject userMap = JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue();
            List<UserRole> userRoleList = userRoleService.get(userMap.getString("username"));
            userRoleList = userRoleList.stream()
                    .filter(userRole -> userRole.getRoleId() == 1 || userRole.getProjectId().equals(projectId))
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(userRoleList)) {
                return userRoleList.get(0).getPower();
            }
        }
        return new HashMap<>();
    }

    /**
     * 获取个性化配置信息
     *
     * @return
     * @throws IOException
     */
    @Override
    public PersonalizedConfiguration getPersonalConfig() {
        QueryWrapper<PersonalizedConfiguration> queryWrapper = new QueryWrapper<PersonalizedConfiguration>();
        List<PersonalizedConfiguration> personals = personalMapper.selectList(queryWrapper);
        if (personals.size() > 1) {
            queryWrapper.eq("status", "1");
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
     *
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
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                inPut.close();
            }
        }
        String voiceBase64 = Base64.getEncoder().encodeToString(bus);
        return Base64.getDecoder().decode(voiceBase64);
    }

    /**
     * 校验数据库数据
     *
     * @param configuration
     */
    private void checkout(PersonalizedConfiguration configuration) {
        QueryWrapper<PersonalizedConfiguration> queryWrapper = new QueryWrapper<PersonalizedConfiguration>();
        queryWrapper.eq("status", "1");
        List<PersonalizedConfiguration> personals = personalMapper.selectList(queryWrapper);
        Date date = new Date();
        if (personals.size() == 0) {
            configuration.setCreateTime(date);
            personalMapper.insert(configuration);
        } else {
            configuration.setUpdateTime(date);
            queryWrapper.eq("status", "1");
            personalMapper.update(configuration, queryWrapper);
        }
    }

    /**
     * 绑定或解绑超级管理员
     */
    public void bindAdmin(UserDto userDto) {
        String username =
                JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue().getString(USERNAME);
        if (!ADMIN.equals(username)) {
            throw new BusinessException(ErrorMessage.NO_AUTHORITY);
        }
        if (userDto.getIsAdmin()) {
            userRoleService.insert(null, userDto.getUserName(), NUM_ROLE_ADMIN);
        } else {
            userRoleService.delete(userDto.getUserName(), null, NUM_ROLE_ADMIN);
        }
    }

    /**
     * 设置用户的角色列表
     *
     * @param userName 用户名
     * @param userDto
     */
    public void setUserRoleList(String userName, UserDto userDto) {
        List<UserRole> userRoleList = userRoleService.get(userName);
        if (!CollectionUtils.isEmpty(userRoleList)) {
            userDto.setUserRoleList(userRoleList);
            userDto.setIsAdmin(userRoleList.stream().anyMatch(userRole -> userRole.getRoleId() == 1));
        }
    }

}
