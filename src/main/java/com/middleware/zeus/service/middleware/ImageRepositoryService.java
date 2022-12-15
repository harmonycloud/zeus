package com.middleware.zeus.service.middleware;

import com.github.pagehelper.PageInfo;
import com.middleware.caas.common.model.middleware.ImageRepositoryDTO;
import com.middleware.caas.common.model.middleware.Registry;
import com.middleware.zeus.bean.BeanImageRepository;
import io.fabric8.kubernetes.api.model.Secret;

import java.util.List;

/**
 * @author yushuaikang
 * @date 2022/3/10 下午2:41
 */
public interface ImageRepositoryService {

    /**
     * 新增镜像仓库
     * @param clusterId      集群ID
     * @param imageRepositoryDTO 镜像仓库信息
     */
    void insert(String clusterId, ImageRepositoryDTO imageRepositoryDTO);

    /**
     * 获取镜像仓库列表
     * @param clusterId 集群ID
     * @return
     */
    List<ImageRepositoryDTO> list(String clusterId);

    /**
     * 获取镜像仓库列表
     * @param clusterId 集群ID
     * @param keyword   关键字
     * @return
     */
    PageInfo<ImageRepositoryDTO> listImageRepository(String clusterId, String keyword);

    /**
     * 修改镜像仓库信息
     * @param clusterId      集群ID
     * @param imageRepositoryDTO 镜像仓库信息
     */
    void update(String clusterId, ImageRepositoryDTO imageRepositoryDTO);

    /**
     * 删除镜像仓库
     * @param clusterId 集群ID
     * @param id        ID
     */
    void delete(String clusterId, String id);

    /**
     * 通过集群ID查看详情
     * @param clusterId
     * @return
     */
    ImageRepositoryDTO detailByClusterId(String clusterId);

    /**
     * 通过主键ID查看详情
     * @param id
     * @return
     */
    ImageRepositoryDTO detailById(Integer id);

    /**
     * 通过集群ID移除镜像仓库
     * @param clusterId
     */
    void removeImageRepository(String clusterId);

    /**
     * 通过主键ID查看详情
     * @param registry
     * @return
     */
    ImageRepositoryDTO convertRegistry(Registry registry);


    /**
     * 生成register对象
     * @param mirrorImageId 镜像仓库id
     * @return Registry
     */
    Registry generateRegistry(String mirrorImageId);

    /**
     * 获取集群模式registry
     * @param clusterId
     * @return
     */
    BeanImageRepository getClusterDefaultRegistry(String clusterId);

    /**
     * 创建imagePullSecret
     */
    void createImagePullSecret(String clusterId, String namespace, List<ImageRepositoryDTO> imageRepositoryDTOS);

    /**
     * 创建imagePullSecret
     */
    void createImagePullSecret(String clusterId, String namespace, Integer registryId);

    /**
     * 查询分区下所有中间件平台的imagePullSecret
     */
    List<Secret> listImagePullSecret(String clusterId, String namespace);

    /**
     * 根据镜像仓库id查询imagePullSecret
     * @return
     */
    Secret getImagePullSecret(String clusterId, String namespace, String registryId);

    /**
     * 根据镜像仓库地址查找镜像仓库bean
     * @param address
     * @return
     */
    BeanImageRepository findByAddress(String address);

}
