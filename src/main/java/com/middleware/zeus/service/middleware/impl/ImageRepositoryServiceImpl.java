package com.middleware.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.middleware.zeus.common.constants.CommonConstant;
import com.middleware.zeus.common.constants.middleware.MiddlewareConstant;
import com.middleware.zeus.common.constants.registry.RegistryConstant;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.middleware.ImageRepositoryDTO;
import com.middleware.zeus.common.model.middleware.Namespace;
import com.middleware.zeus.common.model.middleware.Registry;
import com.middleware.zeus.util.ThreadPoolExecutorFactory;
import com.middleware.zeus.util.uuid.UUIDUtils;
import com.middleware.zeus.bean.BeanImageRepository;
import com.middleware.zeus.dao.BeanImageRepositoryMapper;
import com.middleware.zeus.integration.cluster.SecretWrapper;
import com.middleware.zeus.service.k8s.NamespaceService;
import com.middleware.zeus.service.middleware.ImageRepositoryService;
import com.middleware.zeus.service.registry.RegistryService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author yushuaikang
 * @date 2022/3/10 下午3:14
 */
@Slf4j
@Service
public class ImageRepositoryServiceImpl implements ImageRepositoryService {

    @Autowired
    private BeanImageRepositoryMapper beanImageRepositoryMapper;
    @Autowired
    private RegistryService registryService;
    @Autowired
    private NamespaceService namespaceService;

    @Value("${system.checkRegistry:false}")
    private boolean checkRegistry;

    @Value("${system.privateRegistry.updateNamespaceDefaultSecret:false}")
    private boolean updateNamespaceDefaultSecret;

    @Value("${system.privateRegistry.registryLabelKey:middleware-registry-id}")
    private String registryLabelKey;

    @Autowired
    private SecretWrapper secretWrapper;

    @Override
    public void insert(String clusterId, ImageRepositoryDTO imageRepositoryDTO) {
        if (checkRegistry) {
            check(imageRepositoryDTO);
        }
        BeanImageRepository beanImageRepository = new BeanImageRepository();
        BeanUtils.copyProperties(imageRepositoryDTO, beanImageRepository);
        String address = imageRepositoryDTO.getHostAddress()
            + (imageRepositoryDTO.getPort() == null ? "" : ":" + imageRepositoryDTO.getPort()) + "/"
            + imageRepositoryDTO.getProject();
        beanImageRepository.setClusterId(clusterId);
        beanImageRepository.setAddress(address);
        beanImageRepository.setCreateTime(new Date());
        beanImageRepositoryMapper.insert(beanImageRepository);
        // 更新分区secret(imagePUllSecret)
        if (updateNamespaceDefaultSecret) {
            ThreadPoolExecutorFactory.executor.execute(() -> saveImagePullSecret(clusterId, imageRepositoryDTO.getId()));
        }
    }

    @Override
    public List<ImageRepositoryDTO> list(String clusterId) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<BeanImageRepository>().eq("cluster_id", clusterId);
        List<BeanImageRepository> beanImageRepositoryList = beanImageRepositoryMapper.selectList(wrapper);
        return beanImageRepositoryList.stream().map(beanImageRepository -> {
            ImageRepositoryDTO imageRepositoryDTO = new ImageRepositoryDTO();
            BeanUtils.copyProperties(beanImageRepository, imageRepositoryDTO);
            return imageRepositoryDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public PageInfo<ImageRepositoryDTO> listImageRepository(String clusterId, String keyword) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId);
        if (StringUtils.isNotEmpty(keyword)) {
            wrapper.and(queryWrapper -> {
                queryWrapper.like("address",keyword).or().like("project",keyword).or().like("description",keyword);
            });
        }
        wrapper.orderByDesc("create_time");
        List<BeanImageRepository> beanImageRepositories = beanImageRepositoryMapper.selectList(wrapper);
        PageInfo<ImageRepositoryDTO> ImageRepositoryDTOPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(new PageInfo<>(beanImageRepositories),ImageRepositoryDTOPageInfo);
        return ImageRepositoryDTOPageInfo;
    }

    @Override
    public void update(String clusterId, ImageRepositoryDTO imageRepositoryDTO) {
        if (checkRegistry) {
            check(imageRepositoryDTO);
        }
        BeanImageRepository beanImageRepository = new BeanImageRepository();
        BeanUtils.copyProperties(imageRepositoryDTO, beanImageRepository);
        String address;
        if (imageRepositoryDTO.getPort() != null){
            address = imageRepositoryDTO.getHostAddress() + ":" + imageRepositoryDTO.getPort() + "/";
        } else {
            address = imageRepositoryDTO.getHostAddress() + "/";
        }
        address += imageRepositoryDTO.getProject();
        beanImageRepository.setClusterId(clusterId);
        beanImageRepository.setAddress(address);
        beanImageRepository.setUpdateTime(new Date());
        beanImageRepositoryMapper.updateById(beanImageRepository);
        // 更新分区secret(imagePUllSecret)
        if (updateNamespaceDefaultSecret) {
            ThreadPoolExecutorFactory.executor.execute(() -> saveImagePullSecret(clusterId, imageRepositoryDTO.getId()));
        }
    }

    @Override
    public void delete(String clusterId, String id) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId).eq("id", id);
        beanImageRepositoryMapper.delete(wrapper);
    }

    @Override
    public ImageRepositoryDTO detailByClusterId(String clusterId) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId).eq("is_default", CommonConstant.NUM_ONE);
        List<BeanImageRepository> beanImageRepositories = beanImageRepositoryMapper.selectList(wrapper);
        ImageRepositoryDTO imageRepositoryDTO = new ImageRepositoryDTO();
        if (!beanImageRepositories.isEmpty()) {
            BeanUtils.copyProperties(beanImageRepositories.get(0), imageRepositoryDTO);
        }
        return imageRepositoryDTO;
    }

    @Override
    public ImageRepositoryDTO detailById(Integer id) {
        ImageRepositoryDTO imageRepositoryDTO = new ImageRepositoryDTO();
        BeanImageRepository beanImageRepository = beanImageRepositoryMapper.selectById(id);
        BeanUtils.copyProperties(beanImageRepository, imageRepositoryDTO);
        return imageRepositoryDTO;
    }

    @Override
    public void removeImageRepository(String clusterId) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId);
        beanImageRepositoryMapper.delete(wrapper);
    }

    @Override
    public ImageRepositoryDTO convertRegistry(Registry registry) {
        ImageRepositoryDTO imageRepositoryDTO = new ImageRepositoryDTO();
        BeanUtils.copyProperties(registry, imageRepositoryDTO);
        imageRepositoryDTO.setUsername(registry.getUser());
        imageRepositoryDTO.setProject(registry.getChartRepo());
        imageRepositoryDTO.setIsDefault(CommonConstant.NUM_ONE);
        imageRepositoryDTO.setPort(registry.getPort());
        imageRepositoryDTO.setHostAddress(registry.getAddress());
        return imageRepositoryDTO;
    }

    @Override
    public Registry generateRegistry(String mirrorImageId) {
        Registry registry = new Registry();
        ImageRepositoryDTO imageRepositoryDTO = this.detailById(Integer.valueOf(mirrorImageId));
        BeanUtils.copyProperties(imageRepositoryDTO,registry);
        registry.setUser(imageRepositoryDTO.getUsername());
        registry.setChartRepo(imageRepositoryDTO.getProject());
        registry.setPort(imageRepositoryDTO.getPort());
        registry.setAddress(imageRepositoryDTO.getHostAddress());
        return registry;
    }

    @Override
    public BeanImageRepository getClusterDefaultRegistry(String clusterId) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId);
        List<BeanImageRepository> repositories = beanImageRepositoryMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(repositories)) {
            throw new BusinessException(ErrorMessage.CLUSTER_NOT_ADD_REPOSITORY);
        }
        List<BeanImageRepository> defaultRegistries = repositories.stream().
                filter(beanImageRepository -> beanImageRepository.getIsDefault() != null && beanImageRepository.getIsDefault() == 1).
                collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(defaultRegistries)) {
            return defaultRegistries.get(0);
        } else {
            return repositories.get(0);
        }
    }

    @Override
    public void createOrReplaceImagePullSecret(String clusterId, String namespace, List<ImageRepositoryDTO> imageRepositoryDTOS) {
        for (ImageRepositoryDTO repositoryDTO : imageRepositoryDTOS) {
            String sa = "middleware-registry-" + UUIDUtils.get8UUID();
            createOrReplaceImagePullSecret(clusterId, namespace, repositoryDTO.getId(), sa);
        }
    }

    @Override
    public void createOrReplaceImagePullSecret(String clusterId, String namespace, Integer registryId) {
        ImageRepositoryDTO imageRepositoryDTO = detailById(registryId);
        if (imageRepositoryDTO == null) {
            throw new BusinessException(ErrorMessage.REGISTRY_NOT_FOUND);
        }
        List<ImageRepositoryDTO> imageRepositoryDTOS = new ArrayList<>();
        imageRepositoryDTOS.add(imageRepositoryDTO);
        createOrReplaceImagePullSecret(clusterId, namespace, imageRepositoryDTOS);
    }

    @Override
    public void createOrReplaceImagePullSecret(String clusterId, String namespace, Integer registryId, String secretName) {
        Secret secret = secretWrapper.get(clusterId, namespace, registryLabelKey);
        if (secret == null) {
            Map<String, String> data = new HashMap<>();
            data.put(RegistryConstant.KEY_IMAGE_PULL_SECRET, encryptRegistry(registryId));
            secret = new Secret();
            secret.setKind(MiddlewareConstant.SECRET);
            secret.setApiVersion(MiddlewareConstant.V1);
            secret.setType(RegistryConstant.IMAGE_PULL_SECRET_TYPE);
            secret.setData(data);
            ObjectMeta objectMeta = new ObjectMeta();
            objectMeta.setNamespace(namespace);
            objectMeta.setName(secretName);
            Map<String, String> labels = new HashMap<>();
            labels.put(registryLabelKey, registryId.toString());
            objectMeta.setLabels(labels);
            secret.setMetadata(objectMeta);
            secretWrapper.createOrReplace(clusterId, namespace, secret);
        } else {
            String secretStr = secret.getData().get(RegistryConstant.KEY_IMAGE_PULL_SECRET);
            String newSecretStr = encryptRegistry(registryId);
            if (!secretStr.equals(newSecretStr)) {
                secret.getData().put(RegistryConstant.KEY_IMAGE_PULL_SECRET, newSecretStr);
                secretWrapper.createOrReplace(clusterId, namespace, secret);
            }
        }
    }

    @Override
    public List<Secret> listImagePullSecret(String clusterId, String namespace) {
        return secretWrapper.list(clusterId,  namespace, registryLabelKey);
    }

    @Override
    public Secret getImagePullSecret(String clusterId, String namespace, String registryId) {
        return secretWrapper.get(clusterId, namespace, registryLabelKey, registryId);
    }

    @Override
    public BeanImageRepository findByAddress(String address) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("address", address);
        List<BeanImageRepository> repositories = beanImageRepositoryMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(repositories)) {
            return repositories.get(0);
        }
        return null;
    }

    private void saveImagePullSecret(String clusterId, Integer repositoryId) {
        // 先查询全部已注册的分区
        List<Namespace> nsList = namespaceService.list(clusterId, true, false, false, null, null, null);
        // 遍历nsList,根据label查询分区下的secret
        for (Namespace namespace : nsList) {
            Secret secret = secretWrapper.get(clusterId, namespace.getName(), registryLabelKey, String.valueOf(repositoryId));
            // 判断secret是否存在，不存在则创建，并绑定到默认分区
            if (secret == null) {
                namespaceService.checkAndBindImagePullSecret(clusterId, namespace.getName(), repositoryId);
            } else {
                // 更新secret
                createOrReplaceImagePullSecret(clusterId, namespace.getName(), repositoryId, secret.getMetadata().getName());
            }
        }
    }

    /**
     * 加密制品仓库信息
     * @param registryId
     * @return
     */
    private String encryptRegistry(Integer registryId) {
        ImageRepositoryDTO registry = detailById(registryId);

        String host = registry.getHostAddress();
        Integer port = registry.getPort();
        String user = registry.getUsername();
        String password = registry.getPassword();

        Base64.Encoder encoder = Base64.getEncoder();
        String auth = user + ":" + password;
        auth = encoder.encodeToString(auth.getBytes());

        JSONObject authInfo = new JSONObject();
        authInfo.put("auth", auth);
        authInfo.put("username", user);
        authInfo.put("password", password);

        JSONObject registryJson = new JSONObject();
        String address = (port == null) ? host : host + ":" + port;
        registryJson.put(address, authInfo);

        JSONObject authObj = new JSONObject();
        authObj.put("auths", registryJson);

        String res = authObj.toJSONString();
        return encoder.encodeToString(res.getBytes());
    }

    /**
     * 校验仓库是否可以连接
     */
    public void check(ImageRepositoryDTO imageRepositoryDTO){
        Registry registry = new Registry()
                .setVersion(imageRepositoryDTO.getVersion())
                .setAddress(imageRepositoryDTO.getHostAddress())
                .setProtocol(imageRepositoryDTO.getProtocol())
                .setPort(imageRepositoryDTO.getPort())
                .setChartRepo(imageRepositoryDTO.getPassword())
                .setType("harbor")
                .setUser(imageRepositoryDTO.getUsername())
                .setPassword(imageRepositoryDTO.getPassword());
        registryService.validate(registry);
    }
}
