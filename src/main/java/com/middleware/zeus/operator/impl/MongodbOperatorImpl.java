package com.middleware.zeus.operator.impl;

import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.ACTIVE_ACTIVE;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;
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
import com.middleware.zeus.common.model.user.ProjectDto;
import com.middleware.zeus.operator.api.MongodbOperator;
import com.middleware.zeus.operator.miiddleware.AbstractMongodbOperator;
import com.middleware.zeus.service.k8s.ServiceAccountService;
import com.middleware.zeus.service.middleware.OpsManagerService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.util.RequestUtil;
import com.middleware.zeus.util.encrypt.Base64Utils;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2024/12/9 4:51 PM
 */
@Slf4j
@Operator(paramTypes4One = Middleware.class)
public class MongodbOperatorImpl extends AbstractMongodbOperator implements MongodbOperator {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private ServiceAccountService serviceAccountService;
    @Autowired
    private OpsManagerService opsManagerService;

    @Override
    protected void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 清理ID_MAP
        opsManagerService.clearIdMap();
        // 刷新组织项目信息
        opsManagerService.refresh(cluster.getId());
        // 尝试创建sa
        tryCreateSa(cluster.getId(), middleware.getNamespace());
        // 替换通用values
        replaceCommonValues(middleware, cluster, values);
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        replaceCommonResources(quota, values.getJSONObject(RESOURCES));
        replaceCommonStorages(quota, values.getJSONObject(PERSISTENCE));
        // 设置实例数
        values.getJSONObject(MONGODB.getType()).put("members", quota.getNum());
        // 设置版本
        values.getJSONObject(MONGODB.getType()).put("version", middleware.getVersion());

        if (StringUtils.isNotEmpty(middleware.getMode())) {
            JSONObject mongodb = values.getJSONObject(MONGODB.getType());
            mongodb.put(TYPE, middleware.getMode());
        }

        // 查询用户认证信息
        Secret secret = secretService.get(cluster.getId(), MIDDLEWARE_OPERATOR,
                "middleware-operator-mongodb-enterprise-operator-om-admin-key");
        String publicKey = new String(Base64Utils.decode(secret.getData().get("publicKey")));
        String privateKey = new String(Base64Utils.decode(secret.getData().get("privateKey")));
        // 设置用户认证信息
        values.getJSONObject("credentials").put("privateKey", privateKey);
        values.getJSONObject("credentials").put("publicKey", publicKey);
        // 获取当前组织所在的映射在ops manager中的组织id
        String orgId = opsManagerService.getMappingId(RequestUtil.getOrganId()).get(0);
        if (StringUtils.isEmpty(orgId)) {
            throw new BusinessException(ErrorMessage.MONGODB_GET_ORGAN_ID_FAILED);
        }
        values.getJSONObject(PROJECT).put("organId", orgId);
        // 根据当前项目id获取项目名称
        ProjectDto projectDto = projectService.get(RequestUtil.getOrganId(), RequestUtil.getProjectId());
        values.getJSONObject(PROJECT).put("projectName", projectDto.getName() + "@" + middleware.getName());

        // 设置双活信息
        checkAndSetActiveActive(values, middleware);
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertResourcesByHelmChart(middleware, middleware.getType(), values.getJSONObject(RESOURCES));
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        // 设置副本数
        if (middleware.getQuota() != null && middleware.getQuota().get(middleware.getType()) != null) {
            middleware.getQuota().get(middleware.getType()).setNum(values.getJSONObject(MONGODB.getType()).getInteger("members"));
        }
        // 设置模式
        if (values.getJSONObject(MONGODB.getType()).containsKey(TYPE)) {
            middleware.setMode(values.getJSONObject(MONGODB.getType()).getString(TYPE));
        }
        // 设置版本
        middleware.setVersion(values.getJSONObject(MONGODB.getType()).getString("version"));

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
                sb.append("mongodb.members=").append(quota.getNum()).append(",");
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

    /**
     * 检查是否是双活分区并设置双活配置字段
     * @param values
     * @param middleware
     */
    @Override
    public void checkAndSetActiveActive(JSONObject values, Middleware middleware) {
        if (namespaceService.isOpenAvailableDomain(middleware.getClusterId(), middleware.getNamespace())) {
            super.setActiveActiveConfig(null, values);
            super.setActiveActiveToleration(middleware, values);
            values.put(ACTIVE_ACTIVE, true);
        }
    }

    @Override
    public void deleteStorage(Middleware middleware) {
        super.deleteStorage(middleware);
        ProjectDto projectDto = projectService.get(RequestUtil.getOrganId(), RequestUtil.getProjectId());
        // 删除mongodb项目
        opsManagerService.deleteProject(middleware.getClusterId(), projectDto.getName() + "@" + middleware.getName());
        // 清理ID_MAP
        opsManagerService.clearIdMap();
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

    @Override
    public void getPublicKeyAndPrivateKey(String clusterId, String operatorName) {

    }

    private void tryCreateSa(String clusterId, String namespace) {
        // 查询用户认证信息
        ServiceAccount serviceAccount = serviceAccountService.get(clusterId, namespace, "mongodb-enterprise-database-pods");
        if (serviceAccount != null){
            return;
        }
        serviceAccountService.create(clusterId, namespace, "mongodb-enterprise-database-pods", null);
    }
}
