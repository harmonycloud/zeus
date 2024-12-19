package com.middleware.zeus.operator.impl;

import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;
import static com.middleware.zeus.common.enums.DictEnum.POD;
import static com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum.MONGODB;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.Secret;
import com.middleware.zeus.common.model.StorageDto;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.integration.dashboard.MongodbClientWrapper;
import com.middleware.zeus.operator.api.MongodbOperator;
import com.middleware.zeus.operator.miiddleware.AbstractMongodbOperator;
import com.middleware.zeus.util.encrypt.Base64Utils;

import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2024/12/9 4:51 PM
 */
@Slf4j
@Operator(paramTypes4One = Middleware.class)
public class MongodbOperatorImpl extends AbstractMongodbOperator implements MongodbOperator {

    @Autowired
    private MongodbClientWrapper mongodbClientWrapper;

    @Override
    protected void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用values
        replaceCommonValues(middleware, cluster, values);
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        replaceCommonResources(quota, values.getJSONObject(RESOURCES));
        replaceCommonStorages(quota, values.getJSONObject(PERSISTENCE));
        // 设置实例数
        values.getJSONObject(MONGODB.getType()).put("members", quota.getNum());

        // 查询用户认证信息
        Secret secret = secretService.get(cluster.getId(), MIDDLEWARE_OPERATOR,
                "middleware-operator-mongodb-enterprise-operator-om-admin-key");
        String publicKey = new String(Base64Utils.decode(secret.getData().get("publicKey")));
        String privateKey = new String(Base64Utils.decode(secret.getData().get("privateKey")));
        // 设置用户认证信息
        values.getJSONObject("credentials").put("privateKey", privateKey);
        values.getJSONObject("credentials").put("publicKey", publicKey);
        // 查询orgId
        String orgId = mongodbClientWrapper.getOrgId(publicKey, privateKey);
        if (StringUtils.isEmpty(orgId)) {
            throw new BusinessException(ErrorMessage.MONGODB_GET_ORGAN_ID_FAILED);
        }
        values.getJSONObject(PROJECT).put("organId", orgId);


    }
    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertResourcesByHelmChart(middleware, middleware.getType(),
                values.getJSONObject(POD.getEnPhrase()).getJSONObject(RESOURCES));
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        // 设置副本数
        if (middleware.getQuota() != null && middleware.getQuota().get(middleware.getType()) != null) {
            middleware.getQuota().get(middleware.getType()).setNum(values.getJSONObject(MONGODB.getType()).getInteger("members"));
        }
        convertRegistry(middleware, values);
        return middleware;
    }

    @Override
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
        StringBuilder sb = new StringBuilder();

        // 实例扩容
        if (middleware.getQuota() != null && middleware.getQuota().get(middleware.getType()) != null) {
            MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
            // 设置limit的resources
            setLimitResources(quota);
            // 实例规格扩容
            // cpu
            if (StringUtils.isNotBlank(quota.getCpu())) {
                sb.append("resources.requests.cpu=").append(quota.getCpu()).append(",resources.limits.cpu=")
                        .append(quota.getLimitCpu()).append(",");
            }
            // memory
            if (StringUtils.isNotBlank(quota.getMemory())) {
                sb.append("resources.requests.memory=").append(quota.getMemory())
                        .append(",resources.limits.memory=").append(quota.getLimitMemory()).append(",");
            }
            // 实例模式扩容
            if (quota.getNum() != null) {
                sb.append("replicas=").append(quota.getNum()).append(",");
            }
        }

        // 更新通用字段
        super.updateCommonValues(sb, middleware);
        // 没有修改，直接返回
        if (sb.length() == 0) {
            return;
        }
        // 去掉末尾的逗号
        sb.deleteCharAt(sb.length() - 1);
        // 更新helm
        helmChartService.upgrade(middleware, sb.toString(), null, cluster);
    }

    @Override
    protected void replaceCommonStorages(MiddlewareQuota quota, JSONObject persistence) {
        persistence.put("storageClass", quota.getStorageClassName());
        persistence.put("size", quota.getStorageClassQuota() + "Gi");
    }

    @Override
    protected void convertStoragesByHelmChart(Middleware middleware, String quotaKey, JSONObject values) {
        if (StringUtils.isBlank(quotaKey) || values == null) {
            return;
        }
        MiddlewareQuota quota = checkMiddlewareQuota(middleware, quotaKey);
        JSONObject persistence = values.getJSONObject(PERSISTENCE);
        String storageClass = persistence.getString("storageClass");
        quota.setStorageClassName(storageClass).setStorageClassQuota(persistence.getString("size"));
        quota.setIsLvmStorage(storageClassService.checkLVMStorage(middleware.getClusterId(), middleware.getNamespace(),
                values.getString("storageClassName")));

        // 获取存储中文名
        try {
            if (storageClass.contains(",")) {
                String[] storageClasses = storageClass.split(",");
                StringBuilder sb = new StringBuilder();
                for (String aClass : storageClasses) {
                    StorageDto storageDto = storageService.get(middleware.getClusterId(), aClass);
                    sb.append(storageDto.getAliasName()).append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                quota.setStorageClassAliasName(sb.toString());
            } else {
                StorageDto storageDto = storageService.get(middleware.getClusterId(), storageClass);
                quota.setStorageClassAliasName(storageDto.getAliasName());
            }
        } catch (Exception e) {
            log.error("中间件{}, 获取存储中文名失败", middleware.getName());
        }
    }

    @Override
    public List<String> getConfigmapDataList(ConfigMap configMap) {
        return null;
    }

    @Override
    public Map<String, String> configMap2Data(ConfigMap configMap) {
        return null;
    }

    @Override
    public void editConfigMapData(CustomConfig customConfig, List<String> data) {

    }

    @Override
    public void updateConfigData(ConfigMap configMap, List<String> data) {

    }

    @Override
    public List<IngressDTO> listHostNetworkAddress(String clusterId, String namespace, String middlewareName, String type) {
        return null;
    }
}
