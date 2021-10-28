package com.harmonycloud.zeus.operator.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.constants.MysqlConstant;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlReplicateCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlReplicateStatus;
import com.harmonycloud.zeus.operator.api.KafkaOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractKafkaOperator;
import com.harmonycloud.zeus.util.DateUtil;
import com.harmonycloud.zeus.util.ServiceNameConvertUtil;
import io.fabric8.kubernetes.api.model.ConfigMap;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        tryCreateOpenService(middleware, ServiceNameConvertUtil.convertKafka(middleware.getName()), false);
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        JSONObject values = helmChartService.getInstalledValues(middleware, cluster);
        convertCommonByHelmChart(middleware, values);
        convertStoragesByHelmChart(middleware, middleware.getType(), values);
        // 处理kafka的特有参数
        if (values != null && values.getJSONObject("zookeeper") != null) {
            JSONObject args = values.getJSONObject("zookeeper");
            KafkaDTO kafkaDTO = new KafkaDTO();
            kafkaDTO.setZkAddress(args.getString("address"));
            String[] ports = args.getString("port").split("/");
            kafkaDTO.setZkPort(ports[0]);
            middleware.setKafkaDTO(kafkaDTO);
            JSONArray dynamicTabs = values.getJSONArray("dynamicTabs");
            List<String> capabilities = new ArrayList<>();
            if (dynamicTabs != null) {
                for (Object dynamicTab : dynamicTabs) {
                    capabilities.add(dynamicTab.toString());
                }
                middleware.setCapabilities(capabilities);
            }
        }
        middleware.setManagePlatform(true);
        return middleware;
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

    protected void replaceValues(Middleware middleware, MiddlewareClusterDTO cluster, JSONObject values) {
        super.replaceValues(middleware, cluster, values);
        if (middleware.getQuota() != null) {
            MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
            replaceCommonResources(quota, values.getJSONObject(RESOURCES));
            replaceCommonStorages(quota, values);
        }
        //设置zookeeper信息
        JSONObject zookeeper = values.getJSONObject("zookeeper");
        KafkaDTO kafkaDTO = middleware.getKafkaDTO();
        if (zookeeper != null && kafkaDTO != null) {
            zookeeper.put("address", kafkaDTO.getZkAddress());
            zookeeper.put("port", kafkaDTO.getZkPort() + "/kafka" + UUIDUtils.get8UUID());
            values.put("custom", true);
            //设置dynamicTabs
            values.put("dynamicTabs", middleware.getCapabilities());
        }
    }

}
