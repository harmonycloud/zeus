package com.harmonycloud.zeus.service.system.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.LicenseInfoDto;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.middleware.MiddlewareCrTypeService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.system.LicenseService;
import com.harmonycloud.zeus.util.K8sClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2022/10/27 11:43 上午
 */
@Service
@Slf4j
public class LicenseServiceImpl implements LicenseService {

    @Autowired
    private MiddlewareClusterService clusterService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;

    /**
     * 获取公钥的key
     */
    private static final String PRIVATE_KEY =
        "MIICeQIBADANBgkqhkiG9w0BAQEFAASCAmMwggJfAgEAAoGBAOpUSFd2FVptp+q4+Bt5ibPwBv7aoKHRrIqsVIPsUhAbX6p62/AqjSGYXF5OaC8sGvch6gM7qfFzH/sy6xB0RIIClL28jNTyF0qDPNaZATzgdn8U1GFyvHBs6DHwWZTVGU9V/+uOgqeE2FDrGAVe2d3tEHNadzyVk3ECml1WmzRnAgMBAAECgYEA4kQz/lAFWmYcCChHWrBG6TrSZnBRPy+pPdYdXa1pqCfmfkVX7lYIJPJr7pwjObmK6CsVPb304TJbJUILfL3oDxBw1dLVlkurW3nTVbgVNfzmUiyOd/IgdqEhHapMXLxjc//XCxLzi6fEHklamC9uegjuABeh22ufmMv14Iu7elECQQD+84HvSmfLAsGmzta8VCL8vmiJlVxSVjjhEDB9nvJCWX4egdoOajWUxjID9tjC3Z/cNSpfhUBroNwiUsVa+1e7AkEA60sOyT2hz3Vml4Rv33rbb7qE38rHJhwWF7Oqs4k2Y3dQdpBZjHa5bjsmFu8zDrvoJPAO24+11Tmb0+4wSw89RQJBAIse8b5UCcNb47RUlhT6jIUCmiTJnjFH343gubUy8NuH3ixji0vmZQqkBFLpdmsPaNZPJKovGnIguz73j74P/VUCQQDkfzOQwsWMzpoesoJiKNFJI30+R5I2tDfQNK6lQ68J0SjWuz/7ZKCXJ+HJi+mteVXr6STEnD8dHqDxovJLMjVxAkEAyXz3qOWggQiJoHTwaIMblt6tbhRmdHSmIj7Igh4f65svY5Tg4wi0a2c96aRRxPMXKbMqpbrOT8aTnqSUkW6JZg==";

    @Value("${request.cpu.limit:1}")
    private String limit;

    @Override
    public void license(String license) {
        // todo 解析license

        // todo 生成新的平台认证码(license的MD5)并写入数据库

        // todo 将上述内容加密（rsa）

        // todo 创建secret

    }

    @Override
    public LicenseInfoDto info() {
        return null;
    }

    @Override
    public Boolean check() {
        // 获取当前总使用量

        // 查询secret获取license 并解析获取数据

        // 比较返回值
        return false;
    }

    public void middlewareResource() {

        List<MiddlewareClusterDTO> clusterList = clusterService.listClusterDtos();
        for (MiddlewareClusterDTO cluster : clusterList) {
            List<MiddlewareCR> middlewareCrList = middlewareCRService.listCR(cluster.getId(), null, null);
            List<Namespace> namespaceList = namespaceService.list(cluster.getId());
            middlewareCrList = middlewareCrList.stream()
                .filter(middlewareCr -> namespaceList.stream()
                    .anyMatch(namespace -> namespace.getName().equals(middlewareCr.getMetadata().getNamespace())))
                .collect(Collectors.toList());
            for (MiddlewareCR middlewareCr : middlewareCrList){
                String name = middlewareCr.getMetadata().getName();
                String namespace = middlewareCr.getMetadata().getNamespace();
                String type = middlewareCrTypeService.findTypeByCrType(middlewareCr.getSpec().getType());

                JSONObject values = helmChartService.getInstalledValues(name, namespace, cluster);
                // 根据类型去获取对应的cpu
                Middleware middleware = new Middleware().setClusterId(cluster.getId()).setNamespace(namespace).setName(name).setType(type);
                Double cpu = middlewareService.calculateCpuRequest(middleware, values);
            }
        }
    }

}
