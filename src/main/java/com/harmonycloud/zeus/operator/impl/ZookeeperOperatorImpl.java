package com.harmonycloud.zeus.operator.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.PERSISTENCE;
import static com.harmonycloud.caas.common.constants.NameConstant.RESOURCES;
import static com.harmonycloud.caas.common.enums.DictEnum.POD;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.zeus.util.K8sConvert;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.StorageDto;
import com.harmonycloud.caas.common.model.middleware.CustomConfig;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareQuota;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.operator.api.ZookeeperOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractZookeeperOperator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author liyinlong
 * @since 2021/10/22 3:20 下午
 */
@Operator(paramTypes4One = Middleware.class)
@Slf4j
public class ZookeeperOperatorImpl extends AbstractZookeeperOperator implements ZookeeperOperator {

    @Override
    protected void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用的值
        replaceCommonValues(middleware, cluster, values);

        // 资源配额
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        JSONObject resources = values.getJSONObject(POD.getEnPhrase()).getJSONObject(RESOURCES);
        replaceCommonResources(quota, resources);
        replaceCommonStorages(quota, values.getJSONObject(PERSISTENCE));
        values.put("replicas", quota.getNum());

        // 配置annotations
        if (StringUtils.isNotEmpty(middleware.getAnnotations())) {
            JSONObject ann = new JSONObject();
            String[] annotations = middleware.getAnnotations().split(",");
            for (String annotation : annotations) {
                String[] temp = annotation.split("=");
                ann.put(temp[0], temp[1]);
            }
            values.put("annotations", ann);
        }
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertResourcesByHelmChart(middleware, middleware.getType(),
            values.getJSONObject(POD.getEnPhrase()).getJSONObject(RESOURCES));
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        convertRegistry(middleware, cluster);
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
                sb.append("pod.resources.requests.cpu=").append(quota.getCpu()).append(",pod.resources.limits.cpu=")
                    .append(quota.getLimitCpu()).append(",");
            }
            // memory
            if (StringUtils.isNotBlank(quota.getMemory())) {
                sb.append("pod.resources.requests.memory=").append(quota.getMemory())
                    .append(",pod.resources.limits.memory=").append(quota.getLimitMemory()).append(",");
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
        helmChartService.upgrade(middleware, sb.toString(), cluster);
    }

    @Override
    protected void replaceCommonStorages(MiddlewareQuota quota, JSONObject persistence) {
        persistence.put("storageClassName", quota.getStorageClassName());
        persistence.put("volumeSize", quota.getStorageClassQuota() + "Gi");
    }

    @Override
    protected void convertStoragesByHelmChart(Middleware middleware, String quotaKey, JSONObject values) {
        if (StringUtils.isBlank(quotaKey) || values == null) {
            return;
        }
        MiddlewareQuota quota = checkMiddlewareQuota(middleware, quotaKey);
        JSONObject persistence = values.getJSONObject(PERSISTENCE);
        String storageClass = persistence.getString("storageClassName");
        quota.setStorageClassName(storageClass)
                .setStorageClassQuota(persistence.getString("volumeSize"));
        quota.setIsLvmStorage(storageClassService.checkLVMStorage(middleware.getClusterId(), middleware.getNamespace(),
                values.getString("storageClassName")));

        // 获取存储中文名
        try {
            if (storageClass.contains(",")){
                String[] storageClasses = storageClass.split(",");
                StringBuilder sb = new StringBuilder();
                for (String aClass : storageClasses) {
                    StorageDto storageDto = storageService.get(middleware.getClusterId(), aClass, false);
                    sb.append(storageDto.getAliasName()).append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                quota.setStorageClassAliasName(sb.toString());
            }else {
                StorageDto storageDto = storageService.get(middleware.getClusterId(), storageClass, false);
                quota.setStorageClassAliasName(storageDto.getAliasName());
            }
        } catch (Exception e) {
            log.error("中间件{}, 获取存储中文名失败", middleware.getName());
        }
    }

    @Override
    public void replaceLabels(Middleware middleware, JSONObject values){
        if (StringUtils.isNotBlank(middleware.getLabels())) {
            String[] labelAry = middleware.getLabels().split(CommonConstant.COMMA);
            JSONObject labelJson = new JSONObject();
            for (String label : labelAry) {
                String[] pair = label.split(CommonConstant.EQUAL);
                if (pair.length == 1) {
                    labelJson.put(pair[0], "");
                } else {
                    labelJson.put(pair[0], pair[1]);
                }
            }
            values.getJSONObject("pod").put("labels", labelJson);
        }
    }

    @Override
    public void replaceNodeAffinity(Middleware middleware, JSONObject values){
        if (!CollectionUtils.isEmpty(middleware.getNodeAffinity())) {
            // convert to k8s model
            JSONObject nodeAffinity = K8sConvert.convertNodeAffinity2Json(middleware.getNodeAffinity());
            if (nodeAffinity != null) {
                JSONObject affinity  = new JSONObject();
                affinity.put("nodeAffinity", nodeAffinity);
                values.getJSONObject("pod").put("affinity", affinity);
            }
        }
    }

    @Override
    public void replaceToleration(Middleware middleware, JSONObject values){
        if (!CollectionUtils.isEmpty(middleware.getTolerations())) {
            JSONArray jsonArray = K8sConvert.convertToleration2Json(middleware.getTolerations());
            values.getJSONObject("pod").put("tolerations", jsonArray);
            StringBuffer sbf = new StringBuffer();
            for (String toleration : middleware.getTolerations()) {
                sbf.append(toleration).append(",");
            }
            values.put("tolerationAry", sbf.substring(0, sbf.length()));
        }
    }

    @Override
    public void replaceAnnotations(Middleware middleware, JSONObject values){
        if (StringUtils.isNotEmpty(middleware.getAnnotations())) {
            JSONObject ann = new JSONObject();
            String[] annotations = middleware.getAnnotations().split(",");
            for (String annotation : annotations) {
                String[] temp = annotation.split("=");
                ann.put(temp[0], temp[1]);
            }
            values.getJSONObject("pod").put("annotations", ann);
        }
    }

    @Override
    public List<String> getConfigmapDataList(ConfigMap configMap) {
        return getConfigmapDataList(configMap);
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
}
