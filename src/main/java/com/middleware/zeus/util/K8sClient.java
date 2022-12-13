package com.middleware.zeus.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.middleware.zeus.service.k8s.ClusterCertService;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.MiddlewareClusterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.CaasRuntimeException;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.tool.file.FileUtil;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2020/03/26
 */
@Slf4j
@Component
public class K8sClient {

    private static final Map<String, KubernetesClient> K8S_CLIENT_MAP = new ConcurrentHashMap<>();

    @Value("${k8s.master.url:https://10.96.0.1:443}")
    private String url;
    
    @Value("${k8s.master.sa:/var/run/secrets/kubernetes.io/serviceaccount/token}")
    private String SERVICE_ACCOUNT_PATH;

    public static final String DEFAULT_CLIENT = "defaultClient";

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterCertService clusterCertService;
    @Autowired
    private MiddlewareClusterService middlewareClusterService;

    public static String getClusterId(MiddlewareClusterDTO cluster) {
        return cluster.getDcId() + "--" + cluster.getName();
    }

    /**
     * 获取默认的client
     */
    public KubernetesClient getDefaultClient() {
        if (K8S_CLIENT_MAP.containsKey(DEFAULT_CLIENT)) {
            return K8S_CLIENT_MAP.get(DEFAULT_CLIENT);
        }
        return initDefaultClient();
    }

    /**
     * 获取client
     */
    public static KubernetesClient getClient(String clusterId) {
        if (!K8S_CLIENT_MAP.containsKey(clusterId)){
            try {
                Object service = SpringContextUtils.getBean("k8sClient");
                service.getClass().getMethod("initClients").invoke(service);
            }catch (Exception e){
                log.error("初始化集群失败:{}", e);
            }
        }
        KubernetesClient client = K8S_CLIENT_MAP.get(clusterId);
        if (client == null) {
            throw new CaasRuntimeException(ErrorMessage.CLUSTER_NOT_FOUND);
        }
        return client;
    }

    /**
     * 初始化
     */
    public void initClients() {
        List<MiddlewareClusterDTO> middlewareClusters = middlewareClusterService.listClusterDtos();
        if (middlewareClusters.size() > 0) {
            addK8sClients(middlewareClusters);
            clusterService.initClusterAttributes(middlewareClusters);
        }
        initDefaultClient();
    }

    /**
     * 获取默认集群信息
     */
    public KubernetesClient initDefaultClient() {
        try {
            String token = FileUtil.readFile(SERVICE_ACCOUNT_PATH);
            KubernetesClient client = new DefaultKubernetesClient(
                new ConfigBuilder().withMasterUrl(url).withTrustCerts(true).withOauthToken(token).build());
            K8S_CLIENT_MAP.put(DEFAULT_CLIENT, client);
            return client;
        } catch (Exception e) {
            log.error("初始化默认集群失败");
        }
        return null;
    }

    /**
     * 添加k8s客户端
     */
    public void addK8sClient(MiddlewareClusterDTO c) {
        addK8sClient(c, true);
    }

    /**
     * 添加k8s客户端
     */
    public void addK8sClients(List<MiddlewareClusterDTO> middlewareClusters) {
        middlewareClusters.forEach(c -> {
            if (K8S_CLIENT_MAP.containsKey(c.getId())) {
                return;
            }
            KubernetesClient client;
            if (StringUtils.isNotEmpty(c.getCert().getCertificateAuthorityData())) {
                // 使用证书构建client
                client = new DefaultKubernetesClient(new ConfigBuilder()
                    .withMasterUrl(c.getAddress())
                    .withTrustCerts(true)
                    .withCaCertData(c.getCert().getCertificateAuthorityData())
                    .withClientCertData(c.getCert().getClientCertificateData())
                    .withClientKeyData(c.getCert().getClientKeyData())
                    // 需将 Namespace 初始化为 null
                    .withNamespace(null)
                    .build());
            } else {
                // 使用token构建client，会解析成证书，所以优先使用证书
                client = new DefaultKubernetesClient(new ConfigBuilder().withMasterUrl(c.getAddress())
                    .withTrustCerts(true).withOauthToken(c.getAccessToken()).build());
            }
            K8S_CLIENT_MAP.put(c.getId(), client);

            // 保存证书
            try {
                clusterCertService.saveCert(c);
            } catch (Exception e) {
                log.error("集群{}保存证书异常", c.getId());
            }
        });
    }

    /**
     * 添加k8s客户端
     */
    public void addK8sClient(MiddlewareClusterDTO c, boolean initCert) {
        if (K8S_CLIENT_MAP.containsKey(c.getId())) {
            return;
        }
        KubernetesClient client;
        if (StringUtils.isNotEmpty(c.getCert().getCertificateAuthorityData())) {
            // 使用证书构建client
            client = new DefaultKubernetesClient(new ConfigBuilder()
                .withMasterUrl(c.getAddress())
                .withTrustCerts(true)
                .withCaCertData(c.getCert().getCertificateAuthorityData())
                .withClientCertData(c.getCert().getClientCertificateData())
                .withClientKeyData(c.getCert().getClientKeyData())
                // 需将 Namespace 初始化为 null
                .withNamespace(null)
                .build());
        } else {
            // 使用token构建client，会解析成证书，所以优先使用证书
            client = new DefaultKubernetesClient(new ConfigBuilder().withMasterUrl(c.getAddress()).withTrustCerts(true)
                .withOauthToken(c.getAccessToken()).build());
        }
        //初始化添加集群，将首个集群视作默认集群
        if (CollectionUtils.isEmpty(K8S_CLIENT_MAP)) {
            K8S_CLIENT_MAP.put(DEFAULT_CLIENT, client);
        }
        K8S_CLIENT_MAP.put(c.getId(), client);

        if (initCert) {
            // 保存证书
            try {
                clusterCertService.saveCert(c);
            } catch (Exception e) {
                log.error("集群{}，保存证书异常", c.getId(), e);
            }
        }
    }

    /**
     * 移除k8s客户端
     * 
     * @param clusterId 集群id
     */
    public static void removeClient(String clusterId) {
        K8S_CLIENT_MAP.remove(clusterId);
    }

    public static String getClusterId(ObjectMeta metadata) {
        return metadata.getNamespace() + "--" + metadata.getName();
    }

    /**
     * 修改k8s客户端
     */
    public void updateK8sClient(MiddlewareClusterDTO c) {
        KubernetesClient client;
        if (StringUtils.isNotEmpty(c.getCert().getCertificateAuthorityData())) {
            // 使用证书构建client
            client = new DefaultKubernetesClient(new ConfigBuilder()
                .withMasterUrl(c.getAddress())
                .withTrustCerts(true)
                .withCaCertData(c.getCert().getCertificateAuthorityData())
                .withClientCertData(c.getCert().getClientCertificateData())
                .withClientKeyData(c.getCert().getClientKeyData())
                // 需将 Namespace 初始化为 null
                .withNamespace(null)
                .build());
        } else {
            // 使用token构建client，会解析成证书，所以优先使用证书
            client = new DefaultKubernetesClient(new ConfigBuilder().withMasterUrl(c.getAddress()).withTrustCerts(true)
                .withOauthToken(c.getAccessToken()).build());
        }
        K8S_CLIENT_MAP.put(c.getId(), client);

        // 保存证书
        try {
            clusterCertService.saveCert(c);
        } catch (Exception e) {
            log.error("集群{}保存证书异常", c.getId());
        }
    }

    /**
     * 内部类来保证单例的线程安全
     */
    /*private static class InstanceHolder {
        private static final KubernetesClient KUBERNETES_CLIENT = new DefaultKubernetesClient(new ConfigBuilder()
            .withMasterUrl(url).withTrustCerts(true).build());
        static {
            K8S_CLIENT_MAP.put(DEFAULT_CLIENT, KUBERNETES_CLIENT);
        }
    }*/
}
