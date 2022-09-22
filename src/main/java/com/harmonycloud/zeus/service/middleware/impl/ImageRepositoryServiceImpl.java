package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.ImageRepositoryDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.Registry;
import com.harmonycloud.zeus.bean.BeanImageRepository;
import com.harmonycloud.zeus.dao.BeanImageRepositoryMapper;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.middleware.ImageRepositoryService;
import com.harmonycloud.zeus.service.registry.RegistryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
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
        if (cluster.getRegistry() == null || StringUtils.isEmpty(cluster.getRegistry().getAddress())) {
            Registry registry = new Registry();
            BeanUtils.copyProperties(imageRepositoryDTO, registry);
            registry.setUser(imageRepositoryDTO.getUsername()).setAddress(imageRepositoryDTO.getHostAddress())
                .setType("harbor").setChartRepo(imageRepositoryDTO.getProject()).setId(beanImageRepository.getId());
            cluster.setRegistry(registry);
            clusterService.updateCluster(cluster);
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
        String address = imageRepositoryDTO.getHostAddress() + ":" + imageRepositoryDTO.getPort() + "/"
            + imageRepositoryDTO.getProject();
        beanImageRepository.setClusterId(clusterId);
        beanImageRepository.setAddress(address);
        beanImageRepository.setUpdateTime(new Date());
        beanImageRepositoryMapper.updateById(beanImageRepository);
        // 更新集群默认镜像仓库
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        if (cluster.getRegistry() != null && cluster.getRegistry().getId().equals(imageRepositoryDTO.getId())) {
            Registry registry = cluster.getRegistry();
            BeanUtils.copyProperties(imageRepositoryDTO, registry);
            registry.setUser(imageRepositoryDTO.getUsername()).setAddress(imageRepositoryDTO.getHostAddress())
                .setChartRepo(imageRepositoryDTO.getProject()).setId(beanImageRepository.getId());
            cluster.setRegistry(registry);
            clusterService.updateCluster(cluster);
        }
    }

    @Override
    public void delete(String clusterId, String id) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId).eq("id", id);
        beanImageRepositoryMapper.delete(wrapper);
        // 更新集群默认镜像仓库
        /*MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        if (cluster.getRegistry() != null && cluster.getRegistry().getId().equals(Integer.parseInt(id))) {
            QueryWrapper<BeanImageRepository> existWrapper =
                new QueryWrapper<BeanImageRepository>().eq("cluster_id", clusterId);
            List<BeanImageRepository> beanImageRepositoryList = beanImageRepositoryMapper.selectList(existWrapper);
            if (beanImageRepositoryList.size() == 0) {
                cluster.setRegistry(new Registry());
            }else {
                BeanImageRepository beanImageRepository = beanImageRepositoryList.get(0);
                Registry registry = cluster.getRegistry();
                BeanUtils.copyProperties(beanImageRepository, registry);
                registry.setUser(beanImageRepository.getUsername()).setAddress(beanImageRepository.getHostAddress())
                        .setChartRepo(beanImageRepository.getProject()).setId(beanImageRepository.getId());
                cluster.setRegistry(registry);
            }
            clusterService.updateCluster(cluster);
        }*/
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
