package com.harmonycloud.zeus.operator.impl;

import com.harmonycloud.caas.common.model.middleware.CustomConfig;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.operator.api.ZookeeperOperator;
import com.harmonycloud.zeus.operator.miiddleware.AbstractZookeeperOperator;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/10/22 3:20 下午
 */
@Operator(paramTypes4One = Middleware.class)
public class ZookeeperOperatorImpl extends AbstractZookeeperOperator implements ZookeeperOperator {


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
