package com.harmonycloud.zeus.service.user.impl;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.model.MailUserDTO;
import com.harmonycloud.zeus.bean.MailToUser;
import com.harmonycloud.zeus.bean.PersonalizedConfiguration;
import com.harmonycloud.zeus.bean.user.BeanRole;
import com.harmonycloud.zeus.dao.MailToUserMapper;
import com.harmonycloud.zeus.dao.user.BeanRoleMapper;
import com.harmonycloud.zeus.dao.user.PersonalMapper;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

    @Autowired
    private BeanRoleMapper beanRoleMapper;


    @Value("${system.images.path:/usr/local/zeus-pv/images/middleware}")
    private String imagePath;

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
    public List<ResourceMenuDto> menu(String clusterId) {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String roleId = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString("roleId");
        List<ResourceMenuDto> resourceMenuDtoList = roleService.listMenuByRoleId(roleId);

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
            QueryWrapper<PersonalizedConfiguration> query = new QueryWrapper<PersonalizedConfiguration>().eq("status","0");
            personalMapper.delete(query);
            return;
        }
        if (IMAGE_MAP.containsKey("backgroundImag")) {
            configuration.setBackgroundImage((byte[]) IMAGE_MAP.get("backgroundImag"));
            configuration.setBackgroundPath(String.valueOf(IMAGE_MAP.get("backgroundPath")));
            handleImage((byte[]) IMAGE_MAP.get("backgroundImag"),String.valueOf(IMAGE_MAP.get("backgroundPath")));
        }
        if (IMAGE_MAP.containsKey("loginLogo")) {
            configuration.setLoginLogo((byte[]) IMAGE_MAP.get("loginLogo"));
            configuration.setLoginLogoPath(String.valueOf(IMAGE_MAP.get("loginLogoPath")));
            handleImage((byte[]) IMAGE_MAP.get("loginLogo"),String.valueOf(IMAGE_MAP.get("loginLogoPath")));
        }
        if (IMAGE_MAP.containsKey("homeLogo")) {
            configuration.setHomeLogo((byte[]) IMAGE_MAP.get("homeLogo"));
            configuration.setHomeLogoPath(String.valueOf(IMAGE_MAP.get("homeLogoPath")));
            handleImage((byte[]) IMAGE_MAP.get("homeLogo"),String.valueOf(IMAGE_MAP.get("homeLogoPath")));
        }
        IMAGE_MAP.clear();
        checkout(configuration);
    }

    /**
     * 个性化配置相关图片上传
     * @param file
     * @param type
     * @throws IOException
     */
    @Override
    public void uploadFile(MultipartFile file, String type) throws IOException {
        byte[] background = null;
        byte[] homeLogo = null;
        byte[] loginLogo = null;
        if ("background".equals(type)) {
            background = loadFile(file);
            IMAGE_MAP.put("backgroundImag",background);
            IMAGE_MAP.put("backgroundPath",file.getOriginalFilename());
        }
        if ("login".equals(type)) {
            loginLogo = loadFile(file);
            IMAGE_MAP.put("loginLogo",loginLogo);
            IMAGE_MAP.put("loginLogoPath",file.getOriginalFilename());
        }
        if ("home".equals(type)) {
            homeLogo = loadFile(file);
            IMAGE_MAP.put("homeLogo",homeLogo);
            IMAGE_MAP.put("homeLogoPath",file.getOriginalFilename());
        }
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
                UserRole userRole = new UserRole();
                try {
                    userRole = userRoleService.get(beanUser.getUserName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                UserDto userDto = new UserDto();
                BeanUtils.copyProperties(beanUser,userDto);
                if (!ObjectUtils.isEmpty(userRole)) {
                    BeanRole beanRole = beanRoleMapper.selectOne(new QueryWrapper<BeanRole>().eq("id",userRole.getRoleId()));
                    userDto.setRoleId(beanRole.getId());
                    userDto.setUserName(userRole.getUserName());
                }
                return userDto;
            }).collect(Collectors.toList());
        }
        MailUserDTO mailUserDTO = new MailUserDTO();
        return mailUserDTO.setUsers(userDtos).setUserBy(userDtoList);
    }

    /**
     * 获取个性化配置信息
     * @return
     * @throws IOException
     */
    @Override
    public PersonalizedConfiguration getPersonalConfig() {
        QueryWrapper<PersonalizedConfiguration> queryWrapper = new QueryWrapper<PersonalizedConfiguration>();
        return personalMapper.selectOne(queryWrapper);
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
        return bus;
    }

    /**
     * 校验数据库数据
     * @param configuration
     */
    private void checkout(PersonalizedConfiguration configuration) {
        QueryWrapper<PersonalizedConfiguration> queryWrapper = new QueryWrapper<PersonalizedConfiguration>();
        List<PersonalizedConfiguration> personals = personalMapper.selectList(queryWrapper);
        configuration.setStatus("0");
        Date date = new Date();
        if (personals.size() == 0) {
            configuration.setCreateTime(date);
            personalMapper.insert(configuration);
        }else {
            configuration.setUpdateTime(date);
            queryWrapper.eq("status","0");
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


    private void handleImage(byte[] bytes, String path) throws IOException {
        File file = new File(imagePath + File.separator + path);
        if (!file.exists()) {
            InputStream in = new ByteArrayInputStream(bytes);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(file);
                byte[] buf = new byte[bytes.length];
                int len;
                while ((len = in.read(buf)) != -1) {
                    fileOutputStream.write(buf, 0, len);
                }
                fileOutputStream.flush();
            } catch (Exception e) {
                log.error("图片初始化加载失败");
            } finally {
                in.close();
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            }
        }
    }

}
