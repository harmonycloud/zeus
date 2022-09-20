package com.harmonycloud.zeus.service.middleware;

import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.model.middleware.ImageRepositoryDTO;
import com.harmonycloud.caas.common.model.middleware.Registry;

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
     * @param id
     * @return
     */
    ImageRepositoryDTO convertRegistry(Registry registry);
}
