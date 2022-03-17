package com.harmonycloud.zeus.service.middleware;

import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.model.middleware.MirrorImageDTO;

/**
 * @author yushuaikang
 * @date 2022/3/10 下午2:41
 */
public interface MirrorImageService {

    /**
     * 新增镜像仓库
     * @param clusterId      集群ID
     * @param mirrorImageDTO 镜像仓库信息
     */
    void insert(String clusterId, MirrorImageDTO mirrorImageDTO);

    /**
     * 获取镜像仓库列表
     * @param clusterId 集群ID
     * @param keyword   关键字
     * @return
     */
    PageInfo<MirrorImageDTO> listMirrorImages(String clusterId, String keyword);

    /**
     * 修改镜像仓库信息
     * @param clusterId      集群ID
     * @param mirrorImageDTO 镜像仓库信息
     */
    void update(String clusterId, MirrorImageDTO mirrorImageDTO);

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
    MirrorImageDTO detailByClusterId(String clusterId);

    /**
     * 通过主键ID查看详情
     * @param id
     * @return
     */
    MirrorImageDTO detailById(Integer id);

    /**
     * 通过集群ID移除镜像仓库
     * @param clusterId
     */
    void removeMirrorImage(String clusterId);
}
