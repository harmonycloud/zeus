package com.middleware.zeus.service.k8s;

import com.middleware.zeus.common.model.ClusterQuotaQuery;
import com.middleware.zeus.common.model.ResourceQuotaDo;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/28 4:17 下午
 */
public interface ClusterQuotaService {

    /**
     * 查询平台资源配额
     * @param clusterQuotaQuery
     *
     * @return List<ResourceQuotaDo>
     */
    List<ResourceQuotaDo> list(ClusterQuotaQuery clusterQuotaQuery);
}
