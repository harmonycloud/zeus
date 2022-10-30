package com.harmonycloud.zeus.service.system.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.LicenseInfoDto;
import com.harmonycloud.caas.common.model.MonitorResourceQuotaBase;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import com.harmonycloud.tool.encrypt.RSAUtils;
import com.harmonycloud.zeus.bean.BeanSystemConfig;
import com.harmonycloud.zeus.dao.BeanSystemConfigMapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.k8s.SecretService;
import com.harmonycloud.zeus.service.middleware.MiddlewareCrTypeService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.system.LicenseService;
import com.harmonycloud.zeus.util.K8sClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2022/10/27 11:43 上午
 */
@Service
@Slf4j
public class LicenseServiceImpl implements LicenseService {

    public static final Map<String, Double> CPU_COUNT = new HashMap<>();

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
    @Autowired
    private SecretService secretService;
    @Autowired
    private BeanSystemConfigMapper beanSystemConfigMapper;

    private static final String PUBLIC_KEY =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDqVEhXdhVabafquPgbeYmz8Ab+2qCh0ayKrFSD7FIQG1+qetvwKo0hmFxeTmgvLBr3IeoDO6nxcx/7MusQdESCApS9vIzU8hdKgzzWmQE84HZ/FNRhcrxwbOgx8FmU1RlPVf/rjoKnhNhQ6xgFXtnd7RBzWnc8lZNxAppdVps0ZwIDAQAB";

    private static final String PRIVATE_KEY =
        "MIICeQIBADANBgkqhkiG9w0BAQEFAASCAmMwggJfAgEAAoGBAOpUSFd2FVptp+q4+Bt5ibPwBv7aoKHRrIqsVIPsUhAbX6p62/AqjSGYXF5OaC8sGvch6gM7qfFzH/sy6xB0RIIClL28jNTyF0qDPNaZATzgdn8U1GFyvHBs6DHwWZTVGU9V/+uOgqeE2FDrGAVe2d3tEHNadzyVk3ECml1WmzRnAgMBAAECgYEA4kQz/lAFWmYcCChHWrBG6TrSZnBRPy+pPdYdXa1pqCfmfkVX7lYIJPJr7pwjObmK6CsVPb304TJbJUILfL3oDxBw1dLVlkurW3nTVbgVNfzmUiyOd/IgdqEhHapMXLxjc//XCxLzi6fEHklamC9uegjuABeh22ufmMv14Iu7elECQQD+84HvSmfLAsGmzta8VCL8vmiJlVxSVjjhEDB9nvJCWX4egdoOajWUxjID9tjC3Z/cNSpfhUBroNwiUsVa+1e7AkEA60sOyT2hz3Vml4Rv33rbb7qE38rHJhwWF7Oqs4k2Y3dQdpBZjHa5bjsmFu8zDrvoJPAO24+11Tmb0+4wSw89RQJBAIse8b5UCcNb47RUlhT6jIUCmiTJnjFH343gubUy8NuH3ixji0vmZQqkBFLpdmsPaNZPJKovGnIguz73j74P/VUCQQDkfzOQwsWMzpoesoJiKNFJI30+R5I2tDfQNK6lQ68J0SjWuz/7ZKCXJ+HJi+mteVXr6STEnD8dHqDxovJLMjVxAkEAyXz3qOWggQiJoHTwaIMblt6tbhRmdHSmIj7Igh4f65svY5Tg4wi0a2c96aRRxPMXKbMqpbrOT8aTnqSUkW6JZg==";

    @Value("${request.cpu.limit:1}")
    private String limit;

    @Override
    public void license(String licenseStr) throws Exception {
        // 解析license
        JSONObject license = JSONObject.parseObject(RSAUtils.decryptByPrivateKey(licenseStr, PRIVATE_KEY));
        // 查询数据库 是否已存在license
        JSONObject exist = getSecret();
        if (exist == null) {
            saveLicense(license);
        } else {
            updateLicense(license, exist);
        }
    }

    public void saveLicense(JSONObject license) throws Exception{
        // todo 理论上该有的业务处理
        // 添加license
        updateSysConfig("lecense", RSAUtils.encryptByPublicKey(license.toJSONString(), PUBLIC_KEY));
        String md5 = PasswordUtils.md5(license.toString());
        updateSysConfig("sys_code", md5);
    }

    public void updateLicense(JSONObject license, JSONObject exist) throws Exception {
        // 添加cpu核数
        double produce = license.getDoubleValue("produce") + exist.getDoubleValue("produce");
        double test = license.getDoubleValue("test") + exist.getDoubleValue("test");
        if (license.containsKey("expireTime")) {
            exist.put("expireTime", license.getString("expireTime"));
        }
        exist.put("produce", produce);
        exist.put("test", test);

        String md5 = PasswordUtils.md5(exist.toString());

        // 更新绑定码
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<BeanSystemConfig>().eq("config_name", "sys_code");
        BeanSystemConfig config = beanSystemConfigMapper.selectOne(wrapper);
        config.setConfigValue(md5);

        beanSystemConfigMapper.updateById(config);

        // 更新license
        String licenseStr = RSAUtils.encryptByPublicKey(exist.toJSONString(), PUBLIC_KEY);
        QueryWrapper<BeanSystemConfig> licenseWrapper = new QueryWrapper<BeanSystemConfig>().eq("config_name", "license");
        BeanSystemConfig licenseConfig = beanSystemConfigMapper.selectOne(licenseWrapper);
        config.setConfigValue(licenseStr);
        beanSystemConfigMapper.updateById(licenseConfig);
    }

    public void updateSysConfig(String name, String value) {
        String username = CurrentUserRepository.getUser().getUsername();
        BeanSystemConfig config = new BeanSystemConfig();
        config.setConfigName(name);
        config.setConfigValue(value);
        config.setCreateUser(username);
        beanSystemConfigMapper.insert(config);
    }

    @Override
    public LicenseInfoDto info() throws Exception {
        JSONObject license = getSecret();
        if (license == null){
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }
        LicenseInfoDto info = new LicenseInfoDto();
        MonitorResourceQuotaBase produce = new MonitorResourceQuotaBase();
        produce.setTotal(license.getDoubleValue("produce"));
        produce.setUsed(CPU_COUNT.get("produce"));

        MonitorResourceQuotaBase test = new MonitorResourceQuotaBase();
        test.setTotal(license.getDoubleValue("test"));
        test.setUsed(CPU_COUNT.get("test"));

        info.setType(license.getString("type"));
        info.setTest(test);
        info.setProduce(produce);
        info.setUser(license.getString("user"));
        // todo
        info.setCode("");
        return info;
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
        double produce = 0.0;
        double test = 0.0;
        for (MiddlewareClusterDTO cluster : clusterList) {
            List<MiddlewareCR> middlewareCrList = middlewareCRService.listCR(cluster.getId(), null, null);
            List<Namespace> namespaceList = namespaceService.list(cluster.getId());
            middlewareCrList = middlewareCrList.stream()
                .filter(middlewareCr -> namespaceList.stream()
                    .anyMatch(namespace -> namespace.getName().equals(middlewareCr.getMetadata().getNamespace())))
                .collect(Collectors.toList());
            for (MiddlewareCR middlewareCr : middlewareCrList) {
                String name = middlewareCr.getMetadata().getName();
                String namespace = middlewareCr.getMetadata().getNamespace();
                String type = middlewareCrTypeService.findTypeByCrType(middlewareCr.getSpec().getType());

                JSONObject values = helmChartService.getInstalledValues(name, namespace, cluster);
                // 根据类型去获取对应的cpu
                Middleware middleware =
                    new Middleware().setClusterId(cluster.getId()).setNamespace(namespace).setName(name).setType(type);
                if ("produce".equals(cluster.getType())){
                    produce += middlewareService.calculateCpuRequest(middleware, values);
                }else if ("test".equals(cluster.getType())){
                    test += middlewareService.calculateCpuRequest(middleware, values);
                }
            }
        }
        CPU_COUNT.put("produce", produce);
        CPU_COUNT.put("test", test);
    }

    public JSONObject getSecret() throws Exception {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<BeanSystemConfig>().eq("config_name", "license");
        BeanSystemConfig config = beanSystemConfigMapper.selectOne(wrapper);
        if (config == null) {
            return null;
        }
        return JSONObject.parseObject(RSAUtils.decryptByPrivateKey(config.getConfigValue(), PRIVATE_KEY));
    }

    public String getCode() {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<BeanSystemConfig>().eq("config_name", "sys_code");
        BeanSystemConfig config = beanSystemConfigMapper.selectOne(wrapper);
        if (config == null) {
            return null;
        }
        return config.getConfigValue();
    }

    public static void main(String[] args) throws Exception {
        String key =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDqVEhXdhVabafquPgbeYmz8Ab+2qCh0ayKrFSD7FIQG1+qetvwKo0hmFxeTmgvLBr3IeoDO6nxcx/7MusQdESCApS9vIzU8hdKgzzWmQE84HZ/FNRhcrxwbOgx8FmU1RlPVf/rjoKnhNhQ6xgFXtnd7RBzWnc8lZNxAppdVps0ZwIDAQAB";
        // 生成licenses
        JSONObject object = new JSONObject();
        Date date = new Date();
        object.put("time", date);
        object.put("produce", "300");
        object.put("test", "200");
        object.put("user", "xth");
        object.put("type", "正式版");
        object.put("usable", DateUtils.addInteger(date, Calendar.MINUTE, 15));
        String rsa = RSAUtils.encryptByPublicKey(object.toJSONString(), key);
        System.out.println(rsa);
        String json = RSAUtils.decryptByPrivateKey(rsa, PRIVATE_KEY);
        System.out.println(json);
    }

}
