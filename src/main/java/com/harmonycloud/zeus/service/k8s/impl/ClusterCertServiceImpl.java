package com.harmonycloud.zeus.service.k8s.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.ClusterCert;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.bean.BeanKubeConfig;
import com.harmonycloud.zeus.dao.BeanKubeConfigMapper;
import com.harmonycloud.zeus.integration.cluster.ClusterWrapper;
import com.harmonycloud.zeus.integration.cluster.ConfigMapWrapper;
import com.harmonycloud.zeus.integration.cluster.RbacWrapper;
import com.harmonycloud.zeus.integration.cluster.SecretWrapper;
import com.harmonycloud.zeus.integration.cluster.ServiceAccountWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;
import com.harmonycloud.zeus.service.k8s.ClusterCertService;
import com.harmonycloud.zeus.util.K8sClient;
import com.harmonycloud.tool.file.FileUtil;
import io.fabric8.kubernetes.api.model.AuthInfo;
import io.fabric8.kubernetes.api.model.Cluster;
import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.Context;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleRef;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.harmonycloud.caas.common.constants.NameConstant.KUBE_SYSTEM;

/**
 * @author dengyulong
 * @date 2021/03/30
 */
@Slf4j
@Service
public class ClusterCertServiceImpl implements ClusterCertService {

    @Autowired
    private ConfigMapWrapper configMapWrapper;
    @Autowired
    private RbacWrapper rbacWrapper;
    @Autowired
    private ServiceAccountWrapper serviceAccountWrapper;
    @Autowired
    private SecretWrapper secretWrapper;
    @Autowired
    private BeanKubeConfigMapper beanKubeConfigMapper;

    @Value("${k8s.kubeconfig.path:/usr/local/kubeconfig}")
    private String kubeConfigPath;

    @Override
    public void saveCert(MiddlewareClusterDTO cluster) {
        // 生成admin.conf内容
        String adminConfYaml = generateAdminConf(cluster);
        cluster.getCert().setCertificate(adminConfYaml);

        // 如果token为空，需要根据证书生成token
        if (StringUtils.isBlank(cluster.getAccessToken())) {
            generateTokenByCert(cluster);
        }

        // 创建cm
        Map<String, String> data = new HashMap<>(1);
        data.put("admin.conf", adminConfYaml);
        String certCmName = ClusterCertService.getCertCmName(cluster.getId());
        configMapWrapper.create(K8sClient.DEFAULT_CLIENT, cluster.getDcId(), certCmName, data);

        // 记录文件到数据库
        QueryWrapper<BeanKubeConfig> wrapper = new QueryWrapper<BeanKubeConfig>().eq("cluster_id", cluster.getId());
        BeanKubeConfig exist = beanKubeConfigMapper.selectOne(wrapper);
        if (ObjectUtils.isEmpty(exist)){
            BeanKubeConfig kubeConfig = new BeanKubeConfig();
            kubeConfig.setClusterId(cluster.getId());
            kubeConfig.setConf(adminConfYaml);
            beanKubeConfigMapper.insert(kubeConfig);
        }
    }


    /**
     * 构建admin.conf证书文件
     */
    private String generateAdminConf(MiddlewareClusterDTO cluster) {
        // check cert info
        if (cluster.getCert() == null || StringUtils.isAnyEmpty(cluster.getCert().getCertificateAuthorityData(),
            cluster.getCert().getClientCertificateData(), cluster.getCert().getClientKeyData())) {
            throw new IllegalArgumentException("cert is null, please check MiddlewareCluster resource");
        }

        // clusters
        ArrayList<Object> clusters = new ArrayList<>(1);
        Map<String, Object> clusterMap = new HashMap<>(2);
        Map<String, Object> map1 = new HashMap<>(2);
        map1.put("certificate-authority-data", cluster.getCert().getCertificateAuthorityData());
        map1.put("server", cluster.getAddress());
        clusterMap.put("cluster", map1);
        clusterMap.put("name", "kubernetes");
        clusters.add(clusterMap);

        // contexts
        ArrayList<Object> contexts = new ArrayList<>(1);
        Map<String, Object> contextMap = new HashMap<>(2);
        Map<String, Object> map2 = new HashMap<>(2);
        map2.put("cluster", "kubernetes");
        map2.put("user", "kubernetes-admin");
        contextMap.put("context", map2);
        contextMap.put("name", "kubernetes-admin@kubernetes");
        contexts.add(contextMap);

        // users
        ArrayList<Object> users = new ArrayList<>(1);
        Map<String, Object> userMap = new HashMap<>(2);
        Map<String, String> map3 = new HashMap<>(2);
        map3.put("client-certificate-data", cluster.getCert().getClientCertificateData());
        map3.put("client-key-data", cluster.getCert().getClientKeyData());
        userMap.put("user", map3);
        userMap.put("name", "kubernetes-admin");
        users.add(userMap);

        Map<String, Object> kubeConfig = new LinkedHashMap<>();
        kubeConfig.put("apiVersion", "v1");
        kubeConfig.put("clusters", clusters);
        kubeConfig.put("contexts", contexts);
        kubeConfig.put("current-context", "kubernetes-admin@kubernetes");
        kubeConfig.put("kind", "Config");
        kubeConfig.put("preferences", new HashMap<>());
        kubeConfig.put("users", users);
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(kubeConfig);
    }

    @Override
    public void generateTokenByCert(MiddlewareClusterDTO cluster) {
        try {
            boolean isNewCreate = false;
            // 创建caas-admin用户，并生成token
            String saName = "caas-admin";
            // 查询sa，没有进行创建
            ServiceAccount serviceAccount = serviceAccountWrapper.get(cluster.getId(), KUBE_SYSTEM, saName);
            if (serviceAccount == null) {
                isNewCreate = true;
                serviceAccount = new ServiceAccount();
                ObjectMeta metadata = new ObjectMeta();
                metadata.setName(saName);
                metadata.setNamespace(KUBE_SYSTEM);
                serviceAccount.setMetadata(metadata);
                serviceAccountWrapper.create(cluster.getId(), serviceAccount);
            }

            // 查询ClusterRoleBinding，没有进行创建
            ClusterRoleBinding clusterRoleBinding = rbacWrapper.get(cluster.getId(), saName);
            if (clusterRoleBinding == null) {
                // 创建ClusterRoleBinding将SA和ClusterRole绑定
                clusterRoleBinding = new ClusterRoleBinding();
                ObjectMeta metadata = new ObjectMeta();
                metadata.setName(saName);
                clusterRoleBinding.setMetadata(metadata);

                RoleRef roleRef = new RoleRef();
                roleRef.setName("cluster-admin");
                roleRef.setApiGroup("rbac.authorization.k8s.io");
                roleRef.setKind("ClusterRole");
                clusterRoleBinding.setRoleRef(roleRef);

                List<Subject> subjects = new ArrayList<>(1);
                Subject v1Subject = new Subject();
                v1Subject.setKind("ServiceAccount");
                v1Subject.setName(saName);
                v1Subject.setNamespace(KUBE_SYSTEM);
                subjects.add(v1Subject);
                clusterRoleBinding.setSubjects(subjects);

                rbacWrapper.create(cluster.getId(), clusterRoleBinding);
            } else {
                List<Subject> list = clusterRoleBinding.getSubjects();
                if (!CollectionUtils.isEmpty(list)) {
                    boolean exist = list.stream().anyMatch(v1Subject -> "ServiceAccount".equals(v1Subject.getKind())
                        && v1Subject.getNamespace().equals(KUBE_SYSTEM) && v1Subject.getName().equals(saName));
                    if (!exist) {
                        Subject v1Subject = new Subject();
                        v1Subject.setKind("ServiceAccount");
                        v1Subject.setNamespace(KUBE_SYSTEM);
                        v1Subject.setName(saName);
                        list.add(v1Subject);
                        clusterRoleBinding.setSubjects(list);
                        rbacWrapper.update(cluster.getId(), clusterRoleBinding);
                    }
                }
            }

            // 获取token并存入对象
            List<ObjectReference> secrets;
            if (isNewCreate) {
                ServiceAccount newSa = serviceAccountWrapper.get(cluster.getId(), KUBE_SYSTEM, saName);
                secrets = newSa.getSecrets();
            } else {
                secrets = serviceAccount.getSecrets();
            }
            if (!CollectionUtils.isEmpty(secrets)) {
                ObjectReference reference = secrets.get(0);
                String secretName = reference.getName();
                Secret secret = secretWrapper.get(cluster.getId(), KUBE_SYSTEM, secretName);
                // secret里的数据都是base64编码过的，需要解码才是真正的token
                cluster.setAccessToken(new String(Base64.getDecoder().decode(secret.getData().get("token"))));
            } else {
                log.error("集群id:{}，分区:{}，sa:{}，获取secret失败", cluster.getId(), KUBE_SYSTEM, saName);
            }
        } catch (Exception e) {
            log.error("集群id:{}，根据证书生成token异常", cluster.getId(), e);
        }
    }

    @Override
    public String getKubeConfigFilePath(String clusterId) {
        File file = new File(kubeConfigPath + "/" + ClusterCertService.getCertCmName(clusterId) + ".conf");
        if (!file.exists()) {
            QueryWrapper<BeanKubeConfig> wrapper = new QueryWrapper<BeanKubeConfig>().eq("cluster_id", clusterId);
            BeanKubeConfig kubeConfig = beanKubeConfigMapper.selectOne(wrapper);
            String certCmName = ClusterCertService.getCertCmName(clusterId);
            try {
                FileUtil.writeToLocal(kubeConfigPath, certCmName + ".conf", kubeConfig.getConf());
            } catch (IOException e) {
                log.error("写出admin.conf文件到路径{}/{}异常", kubeConfigPath, certCmName, e);
            }
        }
        return kubeConfigPath + "/" + ClusterCertService.getCertCmName(clusterId) + ".conf";
    }

    @Override
    public void setCertByAdminConf(ClusterCert cert) {
        try {
            Config config = KubeConfigUtils.parseConfigFromString(cert.getCertificate());

            if (config == null) {
                throw new BusinessException(DictEnum.CERTIFICATE, ErrorMessage.AUTH_FAILED);
            }
            Context currentContext = KubeConfigUtils.getCurrentContext(config);
            if (currentContext == null) {
                throw new BusinessException(DictEnum.CERTIFICATE, ErrorMessage.AUTH_FAILED);
            }
            Cluster c = KubeConfigUtils.getCluster(config, currentContext);
            if (c == null || StringUtils.isEmpty(c.getCertificateAuthorityData())
                && StringUtils.isEmpty(c.getCertificateAuthority())) {
                throw new BusinessException(DictEnum.CERTIFICATE, ErrorMessage.AUTH_FAILED);
            }
            AuthInfo user = KubeConfigUtils.getUserAuthInfo(config, currentContext);
            if (user == null
                || StringUtils.isEmpty(user.getClientCertificateData())
                    && StringUtils.isEmpty(user.getClientCertificate())
                || StringUtils.isEmpty(user.getClientKeyData()) && StringUtils.isEmpty(user.getClientKey())) {
                throw new BusinessException(DictEnum.CERTIFICATE, ErrorMessage.AUTH_FAILED);
            }

            // 设置证书信息
            cert.setCertificateAuthorityData(StringUtils.isNotEmpty(c.getCertificateAuthorityData())
                ? c.getCertificateAuthorityData() : c.getCertificateAuthority())
                .setClientCertificateData(StringUtils.isNotEmpty(user.getClientCertificateData())
                    ? user.getClientCertificateData() : user.getClientCertificate())
                .setClientKeyData(
                    StringUtils.isNotEmpty(user.getClientKeyData()) ? user.getClientKeyData() : user.getClientKey());
        } catch (IOException e) {
            log.error("读取admin.conf内容io异常，内容如下：{}", cert.getCertificate(), e);
            throw new BusinessException(ErrorMessage.IO_FAILED);
        }
    }

}
