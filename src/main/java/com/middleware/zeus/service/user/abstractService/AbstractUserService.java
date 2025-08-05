package com.middleware.zeus.service.user.abstractService;

import static com.middleware.zeus.common.constants.CommonConstant.*;
import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.NAMESPACE;
import static com.middleware.zeus.common.constants.user.UserConstant.USERNAME;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.middleware.zeus.common.base.CurrentLanguage;
import com.middleware.zeus.common.constants.CommonConstant;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.RoleBindingEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.Namespace;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.util.FileDownloadUtil;
import com.middleware.zeus.util.OpenSSLUtil;
import com.skyview.language.annotations.TranslateAfterResult;
import io.fabric8.kubernetes.api.model.AuthInfo;
import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.NamedAuthInfo;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.middleware.zeus.common.model.UploadImageFileDto;
import com.middleware.zeus.common.model.user.ResourceMenuDto;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.common.model.user.UserRole;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.bean.BeanClusterMiddlewareInfo;
import com.middleware.zeus.bean.PersonalizedConfiguration;
import com.middleware.zeus.bean.user.BeanUser;
import com.middleware.zeus.dao.BeanMailToUserMapper;
import com.middleware.zeus.dao.user.BeanUserMapper;
import com.middleware.zeus.dao.user.PersonalMapper;
import com.middleware.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.middleware.zeus.service.system.SystemConfigService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.service.user.ResourceMenuService;
import com.middleware.zeus.service.user.RoleService;
import com.middleware.zeus.util.RequestUtil;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.HttpServletResponse;

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
    protected abstract UserDto getUserDto(String userName, boolean roleDetail);

    /**
     * 查询用户信息
     * @param keyword 关键词
     *
     * @return List<UserDto>
     */
    protected abstract List<UserDto> list(String keyword);

    @Value("${system.upload.path:/usr/local/zeus-pv/upload}")
    private String uploadPath;

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
    @Autowired
    protected ResourceMenuService resourceMenuService;
    @Autowired
    protected SecretService secretService;
    @Autowired
    protected CertificateSigningRequestService certificateSigningRequestService;
    @Autowired
    protected RoleBindingService roleBindingService;
    @Autowired
    protected ClusterService clusterService;
    @Autowired
    protected ClusterRoleBindingService clusterRoleBindingService;

    protected String getUsername() {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        return currentUser.getUsername();
    }

    public UserDto getUserDto(String userName, String projectId, boolean roleDetail) {
        if (StringUtils.isEmpty(userName)) {
            userName = getUsername();
        }
        return getUserDto(userName, roleDetail);
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
        UserDto userDto = getUserDto(username, true);
        List<ResourceMenuDto> resourceMenuDtoList = roleService.listMenuByRoleId(userDto, organId, projectId);

        return resourceMenuService.convertMenu(resourceMenuDtoList);
    }


    public List<ResourceMenuDto> listMiddlewareMenu(String clusterId, String organId, String projectId) {
        // 获取集群下所有中间件
        List<BeanClusterMiddlewareInfo> middlewareInfos = clusterMiddlewareInfoService.list(clusterId, false);
        // 过滤状态为未安装的中间件
        middlewareInfos = middlewareInfos.stream().filter(mwInfo -> !mwInfo.getStatus().equals(NUM_TWO)).collect(Collectors.toList());

        // 查询用户角色项目权限
        String username =
                JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue().getString(USERNAME);
        UserDto userDto = getUserDto(username, true);
        Map<String, String> power = new HashMap<>();
        // 判断用户是否为admin 如果不是 则根据组织id、项目id  获取该用户的角色对应的中间件权限
        List<UserRole> userRoleList = new ArrayList<>();
        if (!userDto.getIsAdmin()) {
            userRoleList =
                userDto.getUserRoleList().stream()
                    .filter(userRole -> StringUtils.isNotEmpty(userRole.getOrganId())
                        && userRole.getOrganId().equals(organId) && StringUtils.isNotEmpty(userRole.getProjectId())
                        && userRole.getProjectId().equals(projectId))
                    .collect(Collectors.toList());
        }
        if (!CollectionUtils.isEmpty(userRoleList)) {
            power.putAll(userRoleList.get(0).getPower());
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

    public String getUserK8sConf(String clusterId, String username) throws Exception {
        String conf = secretService.getUserConf(clusterId, ZEUS, ZEUS + LINE + username + LINE + "conf");
        if (conf == null){
            MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
            // 当用户为admin时，直接返回admin证书
            if (username.equals(ADMIN)){
                return cluster.getCert().getCertificate();
            }
            // 通过openssl工具生成key和csr
            KeyPair keyPair = OpenSSLUtil.generateKeyPair();
            String key = OpenSSLUtil.getPrivateKeyPem(keyPair);
            String csr = OpenSSLUtil.getCertificationRequestPem(keyPair, username);
            // 通过k8s认证生成certificate
            String certificate = certificateSigningRequestService.generateCertificate(clusterId, username, csr, null);
            if (certificate == null){
                throw new BusinessException(ErrorMessage.NOT_EXIST);
            }
            // 根据keyPair和certificate  生成conf文件
            Yaml yaml = new Yaml();
            JSONObject config = yaml.loadAs(cluster.getCert().getCertificate(), JSONObject.class);
            // 修改config中的user对象，并转回string存入secret
            JSONArray userArray = new JSONArray();
            JSONObject user = new JSONObject();
            user.put(NAME, username);

            JSONObject authInfo = new JSONObject();
            authInfo.put("client-key-data", key);
            authInfo.put("client-certificate-data", certificate);

            user.put(USER, authInfo);
            userArray.add(user);
            config.put("users", userArray);
            // 初始化用户分区绑定角色
            initK8sUserRoleBinding(clusterId, username);

            conf = yaml.dumpAsMap(config);
            // 通过secret进行保存
            secretService.genericSecretWithConf(clusterId, ZEUS, ZEUS + LINE + username + LINE + "conf", USER_CONF, conf);
        }
        return conf;
    }

    public void downloadUserK8sConf(String clusterId, String username, HttpServletResponse response) throws Exception {
        String conf = this.getUserK8sConf(clusterId, username);
        FileDownloadUtil.downloadFile(response, uploadPath, username + CommonConstant.DOT + "conf", conf);
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

            List<UserRole> userRoleList = getUserDto(userMap.getString("username"), true).getUserRoleList();
            userRoleList = userRoleList.stream()
                .filter(userRole -> userRole.getRoleId() == 1
                    || (StringUtils.isNotEmpty(userRole.getProjectId()) && userRole.getProjectId().equals(projectId)))
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

    @TranslateAfterResult
    public PersonalizedConfiguration getPersonalConfig() {
        QueryWrapper<PersonalizedConfiguration> queryWrapper = new QueryWrapper<>();
        List<PersonalizedConfiguration> personals = personalMapper.selectList(queryWrapper);
        // 判断是否存在非默认的个性化配置(status == 1)
        boolean existCustomizePersonal =
            personals.stream().anyMatch(personal -> String.valueOf(NUM_ONE).equals(personal.getStatus()));
        // 存在非默认的个性化配置
        if (existCustomizePersonal) {
            // 返回存在的第一个非默认的个性化配置
            for (PersonalizedConfiguration personalizedConfiguration : personals) {
                if (personalizedConfiguration.getStatus().equals(String.valueOf(NUM_ONE))) {
                    return personalizedConfiguration;
                }
            }
        }
        // 获取当前语言
        String language = CurrentLanguage.getLanguage();
        for (PersonalizedConfiguration personalizedConfiguration : personals) {
            if (personalizedConfiguration.getLanguage().equals(language)) {
                return personalizedConfiguration;
            }
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

    public void initK8sUserRoleBinding(String clusterId, String username){
        UserDto userDto = this.getUserDto(username, true);
        // 该用户未绑定任何角色，返回null
        if (CollectionUtils.isEmpty(userDto.getUserRoleList())) {
            return;
        }
        // 遍历用户角色
        for (UserRole userRole : userDto.getUserRoleList()) {
            if (userRole.getRoleId() == null || userRole.getWeight() == null
                    || StringUtils.isEmpty(userRole.getOrganId()) || StringUtils.isEmpty(userRole.getProjectId())) {
                continue;
            }
            // 获取需要初始化的命名空间
            List<Namespace> nsList = projectService.getNamespace(userRole.getOrganId(), userRole.getProjectId());
            // 创建roleBinding资源并绑定用户
            for (Namespace ns : nsList) {
                String clusterRole = RoleBindingEnum.findByRoleId(userRole.getRoleId()).getClusterRole();
                roleBindingService.bindUser(clusterId, ns.getName(), clusterRole, Collections.singletonList(username), clusterRole);
            }
        }
    }


}
