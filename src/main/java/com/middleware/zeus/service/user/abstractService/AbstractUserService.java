package com.middleware.zeus.service.user.abstractService;

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
import com.middleware.zeus.service.user.OrganizationUserService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.service.user.RoleService;
import com.middleware.zeus.service.user.UserRoleService;
import com.middleware.zeus.util.ApplicationUtil;
import com.middleware.zeus.util.RequestUtil;
import org.apache.catalina.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * @Date 2023/3/19 6:31 下午
 */
public abstract class AbstractUserService {

    /**
     * 查询用户信息
     * @param userName 用户名
     *
     * @return UserDto
     */
    protected abstract UserDto getUserDto(String userName);

    /**
     * 查询用户信息
     * @param keyword 关键词
     *
     * @return List<UserDto>
     */
    protected abstract List<UserDto> list(String keyword);

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
    protected ProjectService projectService;

    protected String getUsername() {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        return currentUser.getUsername();
    }

    public UserDto getUserDto(String userName, String projectId) {
        if (StringUtils.isEmpty(userName)) {
            userName = getUsername();
        }
        return getUserDto(userName);
    }

    /**
     * 查询菜单信息
     *
     * @param projectId
     * @return
     */

    public List<ResourceMenuDto> menu(String organId, String projectId) {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String username = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString(USERNAME);
        UserDto userDto = getUserDto(username);
        List<ResourceMenuDto> resourceMenuDtoList = roleService.listMenuByRoleId(userDto, organId, projectId);

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


    public List<ResourceMenuDto> listMiddlewareMenu(String clusterId, String organId, String projectId) {
        // 获取集群下所有中间件
        List<BeanClusterMiddlewareInfo> middlewareInfos = clusterMiddlewareInfoService.list(clusterId, false);
        // 过滤状态为未安装的中间件
        middlewareInfos = middlewareInfos.stream().filter(mwInfo -> !mwInfo.getStatus().equals(NUM_TWO)).collect(Collectors.toList());

        // 查询用户角色项目权限
        String username =
                JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue().getString(USERNAME);
        UserDto userDto = getUserDto(username);
        Map<String, String> power = new HashMap<>();
        // 判断用户是否为admin 如果不是 则根据组织id、项目id  获取该用户的角色
        boolean flag = !userDto.getIsAdmin()
            && userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getOrganId().equals(organId)
                && StringUtils.isNotEmpty(projectId) && userRole.getProjectId().equals(projectId));
        if (flag) {
            power.putAll(userDto
                .getUserRoleList().stream().filter(userRole -> userRole.getOrganId().equals(organId)
                    && StringUtils.isNotEmpty(userRole.getProjectId()) && userRole.getProjectId().equals(projectId))
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
     * 添加个性化配置相关信息
     *
     * @param configuration
     * @param status
     * @throws Exception
     */

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

    public UploadImageFileDto uploadFile(MultipartFile file) throws IOException {
        byte[] bytes = null;
        UploadImageFileDto uploadImageFileDto = new UploadImageFileDto();
        bytes = loadFile(file);
        uploadImageFileDto.setBytes(bytes);
        uploadImageFileDto.setType(file.getOriginalFilename().split("\\.")[1]);
        return uploadImageFileDto;
    }

    public Map<String, String> getPower() {
        String projectId = RequestUtil.getProjectId();
        if (StringUtils.isNotEmpty(projectId)) {
            JSONObject userMap = JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue();

            List<UserRole> userRoleList = getUserDto(userMap.getString("username")).getUserRoleList();
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


}
