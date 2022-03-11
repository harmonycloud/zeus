package com.harmonycloud.zeus.service.middleware;

import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.model.middleware.MirrorImageDTO;

import java.util.List;

/**
 * @author yushuaikang
 * @date 2022/3/10 下午2:41
 */
public interface MirrorImageService {

    /**
     * 新增镜像仓库
     * @param clusterId      集群ID
     * @param namespace      命名空间
     * @param mirrorImageDTO 镜像仓库信息
     */
    void insert(String clusterId, String namespace, MirrorImageDTO mirrorImageDTO);

    /**
     * 获取镜像仓库列表
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param keyword   关键字
     * @return
     */
    PageInfo<MirrorImageDTO> listMirrorImages(String clusterId, String namespace, String keyword);

    /**
     * 修改镜像仓库信息
     * @param clusterId      集群ID
     * @param namespace      命名空间
     * @param mirrorImageDTO 镜像仓库信息
     */
    void update(String clusterId, String namespace, MirrorImageDTO mirrorImageDTO);

    /**
     * 删除镜像仓库
     * @param clusterId 集群ID
     * @param namespace 命名空间
     * @param id        ID
     */
    void delete(String clusterId, String namespace, String id);

}
