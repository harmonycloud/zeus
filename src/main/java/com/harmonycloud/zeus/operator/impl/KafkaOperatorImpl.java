package com.harmonycloud.zeus.operator.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.constants.MysqlConstant;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlReplicateCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlReplicateStatus;
import com.harmonycloud.zeus.operator.api.KafkaOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractKafkaOperator;
import com.harmonycloud.zeus.util.DateUtil;
import com.harmonycloud.zeus.util.K8sConvert;
import com.harmonycloud.zeus.util.ServiceNameConvertUtil;
import io.fabric8.kubernetes.api.model.ConfigMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;

import static com.harmonycloud.caas.common.constants.NameConstant.RESOURCES;
import static com.harmonycloud.caas.common.constants.NameConstant.ZOOKEEPER;

/**
 * 处理Kafka逻辑
 * @author  liyinlong
 * @since 2021/9/9 2:21 下午
 */
@Operator(paramTypes4One = Middleware.class)
public class KafkaOperatorImpl extends AbstractKafkaOperator implements KafkaOperator {

    @Override
    public void create(Middleware middleware, MiddlewareClusterDTO cluster) {
        super.create(middleware, cluster);
    }

    @Override
    protected void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        // 替换通用的值
        replaceCommonValues(middleware, cluster, values);

        // 资源配额
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        replaceCommonResources(quota, values.getJSONObject(RESOURCES));
        replaceCommonStorages(quota, values);
        values.put("replicas", quota.getNum());
        // 设置zookeeper信息
        JSONObject zookeeper = new JSONObject();
        KafkaDTO kafkaDTO = middleware.getKafkaDTO();
        if (ObjectUtils.isEmpty(kafkaDTO)){
            throw new BusinessException(ErrorMessage.PARAMETER_VALUE_NOT_PROVIDE);
        }
        zookeeper.put("address", kafkaDTO.getZkAddress());
        zookeeper.put("port", kafkaDTO.getZkPort());
        zookeeper.put("path", kafkaDTO.getPath() + UUIDUtils.get8UUID());
        values.put("zookeeper", zookeeper);
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        convertRegistry(middleware, cluster);

        // 处理kafka的特有参数
        if (values != null && values.getJSONObject("zookeeper") != null) {
            JSONObject args = values.getJSONObject("zookeeper");
            KafkaDTO kafkaDTO = new KafkaDTO();
            kafkaDTO.setZkAddress(args.getString("address"));
            kafkaDTO.setPath(args.getString("path"));
            String[] ports = args.getString("port").split("/");
            kafkaDTO.setZkPort(ports[0]);
            middleware.setKafkaDTO(kafkaDTO);
        }
        middleware.setManagePlatform(true);
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
                sb.append("resources.requests.memory=").append(quota.getMemory()).append("resources.limits.memory=")
                    .append(quota.getLimitMemory()).append(",");
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
}
