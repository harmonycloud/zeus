package com.harmonycloud.zeus.service.system.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.*;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.LicenseInfoDto;
import com.harmonycloud.caas.common.model.MonitorResourceQuotaBase;
import com.harmonycloud.caas.common.model.Secret;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.tool.encrypt.RSAUtils;
import com.harmonycloud.zeus.bean.BeanSystemConfig;
import com.harmonycloud.zeus.integration.cluster.NamespaceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.k8s.SecretService;
import com.harmonycloud.zeus.service.middleware.MiddlewareCrTypeService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.system.LicenseService;
import com.harmonycloud.zeus.service.system.SystemConfigService;
import com.harmonycloud.zeus.util.K8sClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

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
    private NamespaceWrapper namespaceWrapper;
    @Autowired
    private MiddlewareCRService middlewareCrService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private SecretService secretService;

    private static final String PUBLIC_KEY =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDqVEhXdhVabafquPgbeYmz8Ab+2qCh0ayKrFSD7FIQG1+qetvwKo0hmFxeTmgvLBr3IeoDO6nxcx/7MusQdESCApS9vIzU8hdKgzzWmQE84HZ/FNRhcrxwbOgx8FmU1RlPVf/rjoKnhNhQ6xgFXtnd7RBzWnc8lZNxAppdVps0ZwIDAQAB";

    private static final String PRIVATE_KEY =
        "MIICeQIBADANBgkqhkiG9w0BAQEFAASCAmMwggJfAgEAAoGBAOpUSFd2FVptp+q4+Bt5ibPwBv7aoKHRrIqsVIPsUhAbX6p62/AqjSGYXF5OaC8sGvch6gM7qfFzH/sy6xB0RIIClL28jNTyF0qDPNaZATzgdn8U1GFyvHBs6DHwWZTVGU9V/+uOgqeE2FDrGAVe2d3tEHNadzyVk3ECml1WmzRnAgMBAAECgYEA4kQz/lAFWmYcCChHWrBG6TrSZnBRPy+pPdYdXa1pqCfmfkVX7lYIJPJr7pwjObmK6CsVPb304TJbJUILfL3oDxBw1dLVlkurW3nTVbgVNfzmUiyOd/IgdqEhHapMXLxjc//XCxLzi6fEHklamC9uegjuABeh22ufmMv14Iu7elECQQD+84HvSmfLAsGmzta8VCL8vmiJlVxSVjjhEDB9nvJCWX4egdoOajWUxjID9tjC3Z/cNSpfhUBroNwiUsVa+1e7AkEA60sOyT2hz3Vml4Rv33rbb7qE38rHJhwWF7Oqs4k2Y3dQdpBZjHa5bjsmFu8zDrvoJPAO24+11Tmb0+4wSw89RQJBAIse8b5UCcNb47RUlhT6jIUCmiTJnjFH343gubUy8NuH3ixji0vmZQqkBFLpdmsPaNZPJKovGnIguz73j74P/VUCQQDkfzOQwsWMzpoesoJiKNFJI30+R5I2tDfQNK6lQ68J0SjWuz/7ZKCXJ+HJi+mteVXr6STEnD8dHqDxovJLMjVxAkEAyXz3qOWggQiJoHTwaIMblt6tbhRmdHSmIj7Igh4f65svY5Tg4wi0a2c96aRRxPMXKbMqpbrOT8aTnqSUkW6JZg==";

    @Value("${request.cpu.limit:1.0}")
    private double limit;

    @Override
    public void license(String licenseStr) throws Exception {
        // 解析license
        JSONObject license = JSONObject.parseObject(RSAUtils.decryptByPrivateKey(licenseStr, PRIVATE_KEY));
        // check
        checkUid(license);
        // 查询数据库 是否已存在license
        JSONObject exist = getLicense();
        if (exist.containsKey(TYPE) && "试用版".equals(exist.getString(TYPE))) {
            saveLicense(license);
        } else {
            if (licenseStr.equals(exist.getString(LICENSE))){
                throw new BusinessException(ErrorMessage.LICENSE_USED_IN_PLATFORM);
            }
            updateLicense(license, exist);
        }
    }

    public void saveLicense(JSONObject license) throws Exception {
        Secret secret = new Secret().setName(ZEUS_LICENSE).setNamespace(ZEUS);
        Map<String, String> data = new HashMap<>();
        data.put(LICENSE, RSAUtils.encryptByPublicKey(license.toJSONString(), PUBLIC_KEY));
        secret.setData(data);
        secretService.create(K8sClient.DEFAULT_CLIENT, ZEUS, secret);
        // 初始化使用量
        insertSysConfig(PRODUCE, "0.0");
        insertSysConfig(TEST, "0.0");
        insertSysConfig(CPU_UPDATE, FALSE);
    }

    public void updateLicense(JSONObject license, JSONObject exist) throws Exception {
        // 添加cpu核数
        double produce = license.getDoubleValue(PRODUCE) + exist.getDoubleValue(PRODUCE);
        double test = license.getDoubleValue(TEST) + exist.getDoubleValue(TEST);

        exist.put(PRODUCE, produce);
        exist.put(TEST, test);

        // 更新license
        String licenseStr = RSAUtils.encryptByPublicKey(exist.toJSONString(), PUBLIC_KEY);
        Secret secret = new Secret();
        Map<String, String> data = new HashMap<>();
        data.put(LICENSE, licenseStr);
        secret.setData(data);
        secretService.createOrReplace(K8sClient.DEFAULT_CLIENT, ZEUS, secret);
    }

    @Override
    public LicenseInfoDto info() throws Exception {
        JSONObject license = getLicense();
        if (license == null) {
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }
        LicenseInfoDto info = new LicenseInfoDto();
        MonitorResourceQuotaBase produce = new MonitorResourceQuotaBase();
        produce.setTotal(license.getDoubleValue(PRODUCE));
        produce.setUsed(getCpu(PRODUCE));

        MonitorResourceQuotaBase test = new MonitorResourceQuotaBase();
        test.setTotal(license.getDoubleValue(TEST));
        test.setUsed(getCpu(TEST));

        info.setType(license.getString(TYPE));
        info.setTest(test);
        info.setProduce(produce);
        info.setUser(license.getString(USER));
        info.setCode(license.getString(UID));
        return info;
    }

    @Override
    public Boolean check(String clusterId) throws Exception {
        JSONObject license = getLicense();
        List<MiddlewareClusterDTO> clusterList = clusterService.listClusterDtos();
        clusterList =
            clusterList.stream().filter(cluster -> cluster.getId().equals(clusterId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(clusterList)) {
            return false;
        }
        // 获取当前总使用量
        MiddlewareClusterDTO cluster = clusterList.get(0);
        double cpu = getCpu(StringUtils.isEmpty(cluster.getType()) ? TEST : cluster.getType());
        return license.getDoubleValue(cluster.getType()) - cpu > limit;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void middlewareResource() {
        BeanSystemConfig produceConfig = systemConfigService.getConfigForUpdate(PRODUCE);
        BeanSystemConfig testConfig = systemConfigService.getConfigForUpdate(TEST);
        if (produceConfig == null || testConfig == null){
            return;
        }
        List<MiddlewareClusterDTO> clusterList = clusterService.listClusterDtos();
        double produce = 0.0;
        double test = 0.0;
        for (MiddlewareClusterDTO cluster : clusterList) {
            List<MiddlewareCR> middlewareCrList = middlewareCrService.listCR(cluster.getId(), null, null);
            List<Namespace> namespaceList = namespaceService.list(cluster.getId());
            middlewareCrList = middlewareCrList.stream()
                .filter(middlewareCr -> namespaceList.stream()
                    .anyMatch(namespace -> namespace.getName().equals(middlewareCr.getMetadata().getNamespace())))
                .collect(Collectors.toList());
            for (MiddlewareCR middlewareCr : middlewareCrList) {
                String name = middlewareCr.getSpec().getName();
                String namespace = middlewareCr.getMetadata().getNamespace();
                String type = middlewareCrTypeService.findTypeByCrType(middlewareCr.getSpec().getType());

                JSONObject values = helmChartService.getInstalledValues(name, namespace, cluster);
                if (values == null) {
                    continue;
                }
                // 根据类型去获取对应的cpu
                Middleware middleware =
                    new Middleware().setClusterId(cluster.getId()).setNamespace(namespace).setName(name).setType(type);
                if (PRODUCE.equals(cluster.getType())) {
                    produce += middlewareService.calculateCpuRequest(middleware, values);
                } else {
                    test += middlewareService.calculateCpuRequest(middleware, values);
                }
            }
        }
        log.info("produce cpu count: {}", produce);
        log.info("test cpu count: {}", test);
        updateSysConfig(PRODUCE, String.valueOf(produce));
        updateSysConfig(TEST, String.valueOf(test));
    }

    /**
     * 获取license
     */
    public JSONObject getLicense() throws Exception {
        Secret secret = secretService.get(K8sClient.DEFAULT_CLIENT, ZEUS, ZEUS_LICENSE);
        JSONObject license = new JSONObject();
        if (secret == null){
            license.put(TYPE, "试用版");
            license.put(PRODUCE, 5);
            license.put(TEST, 5);
            return license;
        }
        if (!secret.getData().containsKey(LICENSE)){
            log.error("secret中获取license或code失败");
            throw new BusinessException(ErrorMessage.LICENSE_CHECK_FAILED);
        }
        String licenseStr = secret.getData().get(LICENSE);
        try {
            license = JSONObject.parseObject(RSAUtils.decryptByPrivateKey(licenseStr, PRIVATE_KEY));
        }catch (Exception e){
            log.error("license解析失败");
            throw e;
        }
        return license;
    }

    /**
     * 获取使用cpu缓存
     */
    public Double getCpu(String name) {
        BeanSystemConfig config = systemConfigService.getConfig(name);
        if (config == null) {
            return null;
        }
        return Double.parseDouble(config.getConfigValue());
    }

    /**
     * 校验kube-system分区uid
     */
    public void checkUid(JSONObject license) {
        String uid = getKubeSystemUid();
        if (!license.containsKey(UID) || !license.getString(UID).equals(uid)) {
            log.error("license uid 校验失败");
            throw new BusinessException(ErrorMessage.LICENSE_CHECK_FAILED);
        }
    }

    /**
     * 获取kube-system分区uid
     */
    public String getKubeSystemUid() {
        io.fabric8.kubernetes.api.model.Namespace namespace =
            namespaceWrapper.get(K8sClient.DEFAULT_CLIENT, KUBE_SYSTEM);
        log.info(namespace.getMetadata().getUid());
        return namespace.getMetadata().getUid();
    }

    public void insertSysConfig(String name, String value) {
        systemConfigService.addConfig(name, value);
    }

    public void updateSysConfig(String name, String value) {
        systemConfigService.updateConfig(name, value);
    }

}
