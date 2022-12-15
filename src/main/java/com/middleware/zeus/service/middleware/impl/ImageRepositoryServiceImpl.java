package com.middleware.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.middleware.caas.common.constants.CommonConstant;
import com.middleware.caas.common.constants.middleware.MiddlewareConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.middleware.ImageRepositoryDTO;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.Registry;
import com.middleware.tool.uuid.UUIDUtils;
import com.middleware.zeus.bean.BeanImageRepository;
import com.middleware.zeus.dao.BeanImageRepositoryMapper;
import com.middleware.zeus.integration.cluster.SecretWrapper;
import com.middleware.zeus.service.k8s.ClusterService;
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
    private ClusterService clusterService;

    @Value("${system.checkRegistry:false}")
    private boolean checkRegistry;

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
        // 更新集群默认镜像仓库
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        Registry registry = new Registry();
        BeanUtils.copyProperties(imageRepositoryDTO, registry);
        registry.setUser(imageRepositoryDTO.getUsername()).setAddress(imageRepositoryDTO.getHostAddress())
                .setType("harbor").setChartRepo(imageRepositoryDTO.getProject()).setId(beanImageRepository.getId());
        cluster.setRegistry(registry);
        clusterService.update(cluster);
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
        String address = imageRepositoryDTO.getHostAddress() + ":" + imageRepositoryDTO.getPort() + "/"
                + imageRepositoryDTO.getProject();
        beanImageRepository.setClusterId(clusterId);
        beanImageRepository.setAddress(address);
        beanImageRepository.setUpdateTime(new Date());
        beanImageRepositoryMapper.updateById(beanImageRepository);
        // 更新集群默认镜像仓库
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        if (cluster.getRegistry() != null && cluster.getRegistry().getId() != null
                && cluster.getRegistry().getId().equals(imageRepositoryDTO.getId())) {
            Registry registry = cluster.getRegistry();
            BeanUtils.copyProperties(imageRepositoryDTO, registry);
            registry.setUser(imageRepositoryDTO.getUsername()).setAddress(imageRepositoryDTO.getHostAddress())
                    .setChartRepo(imageRepositoryDTO.getProject()).setId(beanImageRepository.getId());
            cluster.setRegistry(registry);
            clusterService.update(cluster);
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
        if (CollectionUtils.isEmpty(defaultRegistries)) {
            return defaultRegistries.get(0);
        } else {
            return repositories.get(0);
        }
    }

    @Override
    public void createImagePullSecret(String clusterId, String namespace, List<ImageRepositoryDTO> imageRepositoryDTOS) {
        for (ImageRepositoryDTO repositoryDTO : imageRepositoryDTOS) {
            String sa = "middleware-registry-" + UUIDUtils.get8UUID();

        }
    }

    @Override
    public void createImagePullSecret(String clusterId, String namespace, Integer registryId) {
        ImageRepositoryDTO imageRepositoryDTO = detailById(registryId);
        if (imageRepositoryDTO == null) {
            throw new BusinessException(ErrorMessage.REGISTRY_NOT_FOUND);
        }
        List<ImageRepositoryDTO> imageRepositoryDTOS = new ArrayList<>();
        imageRepositoryDTOS.add(imageRepositoryDTO);
        createImagePullSecret(clusterId, namespace, imageRepositoryDTOS);
    }

    @Override
    public void createImagePullSecret(String clusterId, String namespace, Integer registryId, String secretName) {
        Secret secret = secretWrapper.get(clusterId, namespace, registryLabelKey);
        if (secret == null) {
            Map<String, String> data = new HashMap<>();
            data.put(".dockerconfigjson", encryptRegistry(registryId));
            secret = new Secret();
            secret.setKind(MiddlewareConstant.SECRET);
            secret.setApiVersion(MiddlewareConstant.V1);
            secret.setType("kubernetes.io/dockerconfigjson");
            secret.setData(data);
            ObjectMeta objectMeta = new ObjectMeta();
            objectMeta.setNamespace(namespace);
            objectMeta.setName(secretName);
            Map<String, String> labels = new HashMap<>();
            labels.put(registryLabelKey, registryId.toString());
            objectMeta.setLabels(labels);
            secret.setMetadata(objectMeta);
            secretWrapper.create(clusterId, namespace, secret);
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
