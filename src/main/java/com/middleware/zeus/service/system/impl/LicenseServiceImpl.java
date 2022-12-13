package com.middleware.zeus.service.system.impl;

import static com.middleware.caas.common.constants.NameConstant.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.middleware.zeus.service.k8s.MiddlewareCRService;
import com.middleware.zeus.service.k8s.MiddlewareClusterService;
import com.middleware.zeus.service.k8s.NamespaceService;
import com.middleware.zeus.service.k8s.SecretService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.LicenseInfoDto;
import com.middleware.caas.common.model.MonitorResourceQuotaBase;
import com.middleware.caas.common.model.Secret;
import com.middleware.caas.common.model.middleware.Middleware;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.caas.common.util.ThreadPoolExecutorFactory;
import com.middleware.tool.encrypt.Base64Utils;
import com.middleware.tool.encrypt.RSAUtils;
import com.middleware.tool.numeric.ResourceCalculationUtil;
import com.middleware.zeus.bean.BeanSystemConfig;
import com.middleware.zeus.integration.cluster.NamespaceWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.service.middleware.MiddlewareCrTypeService;
import com.middleware.zeus.service.middleware.MiddlewareService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.LicenseService;
import com.middleware.zeus.service.system.SystemConfigService;
import com.middleware.zeus.util.K8sClient;

import lombok.extern.slf4j.Slf4j;

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
            // 记录license
            recordLicense(license, licenseStr);
            // 发布license
            saveLicense(license);
        } else {
            // 校验是否已绑定
            checkUsed(exist, licenseStr);
            // 记录license
            recordLicense(exist, licenseStr);
            // 更新license
            updateLicense(license, exist);
        }
    }

    public void saveLicense(JSONObject license) throws Exception {
        Secret secret = new Secret().setName(ZEUS_LICENSE).setNamespace(ZEUS);
        Map<String, String> data = new HashMap<>();
        data.put(LICENSE, RSAUtils.encryptByPublicKey(license.toJSONString(), PUBLIC_KEY));
        secret.setData(data);
        secretService.create(K8sClient.DEFAULT_CLIENT, ZEUS, secret);

    }

    public void updateLicense(JSONObject license, JSONObject exist) throws Exception {
        // 添加cpu核数
        double produce = license.getDoubleValue(PRODUCE) + exist.getDoubleValue(PRODUCE);
        double test = license.getDoubleValue(TEST) + exist.getDoubleValue(TEST);

        exist.put(PRODUCE, produce);
        exist.put(TEST, test);

        // 更新license
        String licenseStr = RSAUtils.encryptByPublicKey(exist.toJSONString(), PUBLIC_KEY);
        Secret secret = new Secret().setName(ZEUS_LICENSE).setNamespace(ZEUS);
        Map<String, String> data = new HashMap<>();
        data.put(LICENSE, licenseStr);
        secret.setData(data);
        secretService.createOrReplace(K8sClient.DEFAULT_CLIENT, ZEUS, secret);
    }

    @Override
    public LicenseInfoDto info() {
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
        String uid = license.getString(UID);
        if (StringUtils.isNotEmpty(uid)){
            info.setCode(Arrays.toString(Base64Utils.decode(uid)));
        }
        return info;
    }

    @Override
    public Boolean check(String clusterId) {
        JSONObject license = getLicense();
        List<MiddlewareClusterDTO> clusterList = clusterService.listClusterDtos();
        clusterList =
            clusterList.stream().filter(cluster -> cluster.getId().equals(clusterId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(clusterList)) {
            return false;
        }
        // 获取当前总使用量
        MiddlewareClusterDTO cluster = clusterList.get(0);
        String type = StringUtils.isEmpty(cluster.getType()) ? TEST : cluster.getType();
        double cpu = getCpu(type);
        return license.getDoubleValue(type) - cpu > limit;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshMiddlewareResource() throws Exception {
        BeanSystemConfig produceConfig = systemConfigService.getConfigForUpdate(PRODUCE);
        BeanSystemConfig testConfig = systemConfigService.getConfigForUpdate(TEST);
        if (produceConfig == null || testConfig == null) {
            return;
        }
        List<MiddlewareClusterDTO> clusterList = clusterService.listClusterDtos();
        List<Double> produceList = new ArrayList<>();
        List<Double> testList = new ArrayList<>();
        for (MiddlewareClusterDTO cluster : clusterList) {
            List<MiddlewareCR> middlewareCrList = middlewareCrService.listCR(cluster.getId(), null, null);
            List<Namespace> namespaceList = namespaceService.list(cluster.getId());
            middlewareCrList = middlewareCrList.stream()
                .filter(middlewareCr -> namespaceList.stream()
                    .anyMatch(namespace -> namespace.getName().equals(middlewareCr.getMetadata().getNamespace())))
                .collect(Collectors.toList());
            final CountDownLatch clusterCountDownLatch = new CountDownLatch(middlewareCrList.size());
            for (MiddlewareCR middlewareCr : middlewareCrList) {
                try {
                    ThreadPoolExecutorFactory.executor.execute(() -> {
                        try {
                            String name = middlewareCr.getSpec().getName();
                            String namespace = middlewareCr.getMetadata().getNamespace();
                            String type = middlewareCrTypeService.findTypeByCrType(middlewareCr.getSpec().getType());

                            JSONObject values = helmChartService.getInstalledValues(name, namespace, cluster);
                            if (values == null) {
                                return;
                            }
                            // 根据类型去获取对应的cpu
                            Middleware middleware = new Middleware().setClusterId(cluster.getId()).setNamespace(namespace)
                                    .setName(name).setType(type);
                            if (PRODUCE.equals(cluster.getType())) {
                                produceList.add(middlewareService.calculateCpuRequest(middleware, values));
                            } else {
                                testList.add(middlewareService.calculateCpuRequest(middleware, values));
                            }
                        } finally {
                            clusterCountDownLatch.countDown();
                        }
                    });
                } catch (Exception e){
                    log.error("中间件cpu资源查询失败", e);
                }
            }
            clusterCountDownLatch.await();
        }
        Double produce = calculateCpu(produceList);
        Double test = calculateCpu(testList);
        log.info("produce cpu count: {}", produce);
        log.info("test cpu count: {}", test);
        updateSysConfig(PRODUCE, String.valueOf(produce));
        updateSysConfig(TEST, String.valueOf(test));
    }

    @Override
    public void addMiddlewareResource(String type, Double cpu) {
        if (StringUtils.isEmpty(type)) {
            type = TEST;
        }
        BeanSystemConfig config = systemConfigService.getConfig(type);
        double now = 0.0;
        if (config != null) {
            now = Double.parseDouble(config.getConfigValue());
        }
        updateSysConfig(type, String.valueOf(now + cpu));
    }

    public Double calculateCpu(List<Double> cpuList) {
        double total = 0.0;
        if (!CollectionUtils.isEmpty(cpuList)) {
            for (Double cpu : cpuList) {
                if (cpu != null) {
                    total += cpu;
                }
            }
        }
        return ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(total), 2, RoundingMode.CEILING);
    }

    /**
     * 获取license
     */
    public JSONObject getLicense() {
        Secret secret = secretService.get(K8sClient.DEFAULT_CLIENT, ZEUS, ZEUS_LICENSE);
        JSONObject license = new JSONObject();
        if (secret == null) {
            license.put(TYPE, "试用版");
            license.put(PRODUCE, 20);
            license.put(TEST, 20);
            return license;
        }
        if (!secret.getData().containsKey(LICENSE)) {
            log.error("secret中获取license或code失败");
            throw new BusinessException(ErrorMessage.LICENSE_CHECK_FAILED);
        }
        String licenseStr = secret.getData().get(LICENSE);
        try {
            license = JSONObject.parseObject(RSAUtils.decryptByPrivateKey(licenseStr, PRIVATE_KEY));
        } catch (Exception e) {
            log.error("license解析失败");
            throw new BusinessException(ErrorMessage.RSA_DECRYPT_FAILED);
        }
        return license;
    }

    /**
     * 获取使用cpu缓存
     */
    public Double getCpu(String name) {
        BeanSystemConfig config = systemConfigService.getConfigForUpdate(name);
        if (config == null) {
            initCpu();
            return 0.0;
        }
        try {
            refreshMiddlewareResource();
        } catch (Exception ignored){
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
     * 校验license是否已被绑定
     */
    public void checkUsed(JSONObject license, String licenseStr) {
        JSONArray array = license.getJSONArray(LICENSE);
        if (array.contains(licenseStr)) {
            throw new BusinessException(ErrorMessage.LICENSE_USED_IN_PLATFORM);
        }
    }

    /**
     * 记录被绑定的license
     */
    public void recordLicense(JSONObject license, String licenseStr) {
        JSONArray array = license.getJSONArray(LICENSE);
        if (array == null) {
            array = new JSONArray();
        }
        array.add(licenseStr);
        license.put(LICENSE, array);
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

    public void updateSysConfig(String name, String value) {
        BeanSystemConfig config = systemConfigService.getConfig(name);
        if (config == null){
            systemConfigService.addConfig(name, value);
        }else {
            systemConfigService.updateConfig(name, value);
        }
    }

    public void initCpu(){
        // 初始化使用量
        updateSysConfig(PRODUCE, "0.0");
        updateSysConfig(TEST, "0.0");
    }

}