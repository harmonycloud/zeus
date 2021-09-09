package com.harmonycloud.zeus.operator.impl;

import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.middleware.CustomConfig;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.operator.api.KafkaOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractKafkaOperator;
import com.harmonycloud.zeus.util.ServiceNameConvertUtil;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.List;
import java.util.Map;

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
        tryCreateOpenService(middleware,ServiceNameConvertUtil.convertKafka(middleware.getName()));
    }

    @Override
    public Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster) {
        Middleware mw = super.convertByHelmChart(middleware, cluster);
        mw.setManagePlatform(true);
        return mw;
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
