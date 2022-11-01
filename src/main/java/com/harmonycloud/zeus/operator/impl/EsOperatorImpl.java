package com.harmonycloud.zeus.operator.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.*;

import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.middleware.ElasticSearchRoleEnum;
import com.harmonycloud.caas.common.enums.middleware.StorageClassProvisionerEnum;
import com.harmonycloud.caas.common.model.StorageDto;
import com.harmonycloud.caas.common.model.middleware.CustomConfig;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareQuota;
import com.harmonycloud.tool.encrypt.PasswordUtils;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.operator.api.EsOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractEsOperator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;

/**
 * @author dengyulong
 * @date 2021/03/23 处理es逻辑
 */
@Operator(paramTypes4One = Middleware.class)
public class EsOperatorImpl extends AbstractEsOperator implements EsOperator {

    @Override
    public void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用的值
        replaceCommonValues(middleware, cluster, values);

        JSONObject clusterInfo = values.getJSONObject(CLUSTER);
        clusterInfo.put(MODE, convertMode(middleware.getMode()));
        clusterInfo.put(MODE_DESC, middleware.getMode());
        if (!values.containsKey(STORAGE)) {
            values.put(STORAGE, new JSONObject());
        }
        JSONObject storage = values.getJSONObject(STORAGE);
        JSONObject resources = values.getJSONObject(RESOURCES);
        ElasticSearchRoleEnum.getValues().forEach((k, v) -> {
            MiddlewareQuota quota = middleware.getQuota().get(k);
            if (quota == null) {
                return;
            }
            replaceCommonResources(quota, resources.getJSONObject(k));
            switch (v) {
                case MASTER:
                    // jvm参数
                    // 计算jvm堆内存
                    String mem = calculateMem(quota.getLimitMemory(), "0.5", "m");
                    JSONObject javaOpts = values.getJSONObject("esJavaOpts");
                    if (javaOpts == null) {
                        javaOpts = new JSONObject();
                        values.put("esJavaOpts", javaOpts);
                    }
                    javaOpts.put("xms", mem);
                    javaOpts.put("xmx", mem);
                    clusterInfo.put("masterReplacesCount", quota.getNum());
                    // 存储
                    storage.put("masterClass", quota.getStorageClassName());
                    storage.put("masterSize", quota.getStorageClassQuota() + "Gi");
                    break;
                case DATA:
                    clusterInfo.put("dataReplacesCount", quota.getNum());
                    // 存储
                    storage.put("dataClass", quota.getStorageClassName());
                    storage.put("dataSize", quota.getStorageClassQuota() + "Gi");
                    break;
                case CLIENT:
                    clusterInfo.put("clientReplacesCount", quota.getNum());
                    // 存储
                    storage.put("clientClass", quota.getStorageClassName());
                    storage.put("clientSize", quota.getStorageClassQuota() + "Gi");
                    break;
                case COLD:
                    clusterInfo.put("coldReplacesCount", quota.getNum());
                    // 存储
                    storage.put("coldClass", quota.getStorageClassName());
                    storage.put("coldSize", quota.getStorageClassQuota() + "Gi");
                    break;
                default:
                    // kibana无需额外处理，实例数不用修改且不需要使用存储
            }
        });

        // 密码
        if (StringUtils.isBlank(middleware.getPassword())) {
            middleware.setPassword(PasswordUtils.generateCommonPassword(10));
        }
        values.put("elasticPassword", middleware.getPassword());
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertRegistry(middleware, cluster);

        // 处理es特有参数
        if (values != null) {
            // 模式
            JSONObject clusterInfo = values.getJSONObject(CLUSTER);
            middleware.setMode(clusterInfo.getString(MODE_DESC));
            // 资源配额
            JSONObject resources = values.getJSONObject(RESOURCES);
            JSONObject storage = values.getJSONObject(STORAGE);
            ElasticSearchRoleEnum.getValues().forEach((k, v) -> {
                JSONObject quota = resources.getJSONObject(k);
                if (quota == null) {
                    return;
                }
                convertResourcesByHelmChart(middleware, k, quota);
                switch (v) {
                    case MASTER:
                        convertStoragesByHelmChart(middleware, k, values);
                        middleware.getQuota().get(k).setNum(clusterInfo.getIntValue("masterReplacesCount"))
                            .setStorageClassName(storage.getString("masterClass"))
                            .setStorageClassQuota(storage.getString("masterSize"))
                            .setIsLvmStorage(storageClassService.checkLVMStorage(middleware.getClusterId(),
                                middleware.getNamespace(), storage.getString("masterClass")));
                        break;
                    case DATA:
                        middleware.getQuota().get(k).setNum(clusterInfo.getIntValue("dataReplacesCount"))
                            .setStorageClassName(storage.getString("dataClass"))
                            .setStorageClassQuota(storage.getString("dataSize"))
                            .setIsLvmStorage(storageClassService.checkLVMStorage(middleware.getClusterId(),
                                middleware.getNamespace(), storage.getString("dataClass")));;
                        break;
                    case CLIENT:
                        middleware.getQuota().get(k).setNum(clusterInfo.getIntValue("clientReplacesCount"))
                            .setStorageClassName(storage.getString("clientClass"))
                            .setStorageClassQuota(storage.getString("clientSize"))
                            .setIsLvmStorage(storageClassService.checkLVMStorage(middleware.getClusterId(),
                                middleware.getNamespace(), storage.getString("clientClass")));;
                        break;
                    case KIBANA:
                        middleware.getQuota().get(k).setNum(1);
                        break;
                    // kibana不用存储，无需设置
                    case COLD:
                        middleware.getQuota().get(k).setNum(clusterInfo.getIntValue("coldReplacesCount"))
                            .setStorageClassName(storage.getString("coldClass"))
                            .setStorageClassQuota(storage.getString("coldSize"))
                            .setIsLvmStorage(storageClassService.checkLVMStorage(middleware.getClusterId(),
                                middleware.getNamespace(), storage.getString("coldClass")));;
                        break;
                    default:
                }
                // 获取存储中文名
                List<StorageDto> storageDtoList = storageService.list(cluster.getId(), false);
                Map<String, String> storageNameMap =
                    storageDtoList.stream().collect(Collectors.toMap(StorageDto::getName, StorageDto::getAliasName));
                for (String key : middleware.getQuota().keySet()) {
                    middleware.getQuota().get(key).setStorageClassAliasName(
                        storageNameMap.get(middleware.getQuota().get(key).getStorageClassName()));
                }
            });
            // 密码
            middleware.setPassword(values.getString("elasticPassword"));
        }

        middleware.setManagePlatform(true);
        return middleware;
    }

    @Override
    public void update(Middleware middleware, MiddlewareClusterDTO cluster) {
        if (cluster == null) {
            cluster = clusterService.findById(middleware.getClusterId());
        }
        StringBuilder sb = new StringBuilder();

        // 实例扩容
        if (middleware.getQuota() != null) {
            // 实例模式修改
            if (StringUtils.isNotBlank(middleware.getMode())) {
                sb.append("cluster.mode=").append(middleware.getMode()).append(",");
            }

            // 遍历处理实例规格
            ElasticSearchRoleEnum.getValues().forEach((k, v) -> {
                MiddlewareQuota quota = middleware.getQuota().get(k);
                if (quota == null) {
                    return;
                }
                // 设置limit的resources
                setLimitResources(quota);

                // 实例规格扩容
                // cpu
                if (StringUtils.isNotBlank(quota.getCpu())) {
                    sb.append("resources.").append(k).append(".requests.cpu=").append(quota.getCpu())
                        .append(",resources.").append(k).append(".limits.cpu=").append(quota.getLimitCpu()).append(",");
                }
                // memory
                if (StringUtils.isNotBlank(quota.getMemory())) {
                    sb.append("resources.").append(k).append(".requests.memory=").append(quota.getMemory())
                        .append(",resources.").append(k).append(".limits.memory=").append(quota.getLimitMemory())
                        .append(",");
                }
                String replicasKey = null;
                switch (v) {
                    case MASTER:
                        // 计算jvm堆内存
                        if (StringUtils.isNotBlank(quota.getLimitMemory())) {
                            String mem = calculateMem(quota.getLimitMemory(), "0.5", "m");
                            sb.append("esJavaOpts.xms=").append(mem).append(",esJavaOpts.xmx=").append(mem).append(",");
                        }
                        replicasKey = "cluster.masterReplacesCount";
                        break;
                    case DATA:
                        replicasKey = "cluster.dataReplacesCount";
                        break;
                    case CLIENT:
                        replicasKey = "cluster.clientReplacesCount";
                        break;
                    default:
                }
                if (quota.getNum() != null && replicasKey != null) {
                    sb.append(replicasKey).append("=").append(quota.getNum()).append(",");
                }
            });
        }

        // 修改密码
        if (StringUtils.isNotBlank(middleware.getPassword())) {
            sb.append("elasticPassword=").append(middleware.getPassword()).append(",");
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
    public void createPreCheck(Middleware middleware, MiddlewareClusterDTO cluster) {
        // 校验节点数量
        checkNum(middleware);
        super.createPreCheck(middleware, cluster);
    }

    @Override
    public void updatePreCheck(Middleware middleware, MiddlewareClusterDTO cluster) {
        // 校验节点数量
        checkNum(middleware);
        super.updatePreCheck(middleware, cluster);
    }

    private void checkNum(Middleware middleware) {
        if (middleware.getQuota() == null) {
            return;
        }
        // 主节点
        MiddlewareQuota master = middleware.getQuota().get(ElasticSearchRoleEnum.MASTER.getRole());
        if (master == null || StringUtils.isAnyBlank(master.getCpu(), master.getMemory())) {
            throw new IllegalArgumentException("Please confirm master node quota is not null");
        }
        if (master.getNum() < 3) {
            throw new IllegalArgumentException("Please confirm master node num greater than or equal to 3");
        }

        // 数据节点
        MiddlewareQuota data = middleware.getQuota().get(ElasticSearchRoleEnum.DATA.getRole());
        if (data != null && data.getStorageClassName() != null && data.getNum() < 3) {
            throw new IllegalArgumentException("Please confirm data node num greater than or equal to 3");
        }
    }

    @Override
    public List<String> getConfigmapDataList(ConfigMap configMap) {
        return new ArrayList<>(Arrays.asList(configMap.getData().get("elasticsearch.yml").split("\n")));
    }

    /**
     * 转换data为map形式
     */
    @Override
    public Map<String, String> configMap2Data(ConfigMap configMap) {
        String dataString = configMap.getData().get("elasticsearch.yml");
        Map<String, String> dataMap = new HashMap<>();
        String[] datalist = dataString.split("\n");
        for (String data : datalist) {
            String[] keyValue = data.split(":");
            dataMap.put(keyValue[0], keyValue[1].replace(" ", ""));
        }
        return dataMap;
    }

    @Override
    public void editConfigMapData(CustomConfig customConfig, List<String> data) {
        boolean changed = false;
        for (int i = 0; i < data.size(); ++i) {
            if (data.get(i).contains(customConfig.getName())) {
                String temp = StringUtils.substring(data.get(i), data.get(i).indexOf(":") + 2, data.get(i).length());
                if (data.get(i).replace(" ", "").replace(temp, "").replace(":", "").equals(customConfig.getName())) {
                    data.set(i, data.get(i).replace(temp, customConfig.getValue()));
                    changed = true;
                }
            }
        }
        if (!changed) {
            data.add(customConfig.getName() + ": " + customConfig.getValue());
        }
    }

    /**
     * 转换data为map形式
     */
    @Override
    public void updateConfigData(ConfigMap configMap, List<String> data) {
        // 构造新configmap
        StringBuilder temp = new StringBuilder();
        for (String str : data) {
            {
                temp.append(str).append("\n");
            }
        }
        configMap.getData().put("elasticsearch.yml", temp.toString());
    }

    @Override
    public void create(Middleware middleware, MiddlewareClusterDTO cluster) {
        super.create(middleware, cluster);
    }

    @Override
    public void updatePvc(Middleware middleware, List<PersistentVolumeClaim> pvcList) {
        for (PersistentVolumeClaim pvc : pvcList) {
            if (CollectionUtils.isEmpty(pvc.getMetadata().getAnnotations())
                || !pvc.getMetadata().getAnnotations().containsKey("volume.beta.kubernetes.io/storage-provisioner")
                || !StorageClassProvisionerEnum.CSI_LVM.getProvisioner()
                    .equals(pvc.getMetadata().getAnnotations().get("volume.beta.kubernetes.io/storage-provisioner"))) {
                continue;
            }
            // 获取对应类型的storage: master data client cold
            String[] names = pvc.getMetadata().getName().split("-");
            String storage = middleware.getQuota().get(names[names.length - 2]).getStorageClassQuota();
            // update
            pvc.getSpec().getResources().getRequests().put(STORAGE, new Quantity(storage));
            pvcWrapper.update(middleware.getClusterId(), middleware.getNamespace(), pvc);
        }

    }

    /**
     * 转换es模式
     * 
     * @param mode
     * @return
     */
    public static String convertMode(String mode) {
        if ("simple".equals(mode)) {
            return "simple";
        } else {
            return "complex";
        }
    }

    @Override
    public Double calculateCpuRequest(JSONObject values) {
        double cpuCount = 0.0;
        JSONObject clusterInfo = values.getJSONObject(CLUSTER);
        JSONObject resources = values.getJSONObject(RESOURCES);
        for (ElasticSearchRoleEnum role : ElasticSearchRoleEnum.values()){
            JSONObject quota = resources.getJSONObject(role.getRole());
            if (quota == null) {
                continue;
            }
            String cpu = quota.getJSONObject("requests").getString(CPU);
            cpuCount += ResourceCalculationUtil.getResourceValue(cpu, CPU, "");
        }
        return cpuCount;
    }
}
