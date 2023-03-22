package com.middleware.zeus.service.k8s;

import com.middleware.caas.common.model.YamlCheck;

/**
 * @author xutianhong
 * @Date 2021/12/23 3:24 下午
 */
public interface YamlService {

    YamlCheck check(String yamlContent);

    /**
     * 查看yaml
     * @param clusterId
     * @param namespace
     * @param plural 资源类型：pods,deployments,mysqlclusters
     * @param name 资源名称
     * @return
     */
    String view(String clusterId, String namespace, String plural, String name);

}
