package com.middleware.zeus.service.k8s;

import com.middleware.caas.common.model.StorageClassDTO;
import com.middleware.caas.common.model.middleware.StorageClass;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;

import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
public interface StorageClassService {

    /**
     * 查询存储服务列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param onlyMiddleware 是否只返回支持中间件的存储
     * @return
     */
    List<StorageClass> list(String clusterId, String namespace, boolean onlyMiddleware);

    /**
     * 根据存储类型名称判断判断存储类型是否是LVM
     * @param clusterId 集群id
     * @param namespace 分区名称
     * @param storageClassName 存储类型sc名称
     * @return
     */
    boolean checkLVMStorage(String clusterId, String namespace, String storageClassName);

    /**
     * 将从middleware中取出的pvc信息，转换为以pod的pvc名称为key,StorageClass sc为value的map,
     * 其中sc.storage表示存储大小，sc.storageClassName表示存储类型名称
     * @param pvcInfos middleware信息中的pvc数组
     * @param clusterId 集群id
     * @param namespace 分区
     * @return
     */
    Map<String, StorageClassDTO> convertStorageClass(List<MiddlewareInfo> pvcInfos, String clusterId,String namespace);

    /**
     * 根据sc的部分名称查找该sc
     * @param scMap 方法convertStorageClass的返回值
     * @param keyword sc名称的关键词
     * @return
     */
    StorageClassDTO fuzzySearchStorageClass(Map<String, StorageClassDTO> scMap, String keyword);
}
