package com.harmonycloud.zeus.operator.impl;

import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.model.middleware.CustomConfig;
import com.middleware.caas.common.model.middleware.IngressDTO;
import com.middleware.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.operator.AbstractBaseOperator;
import com.harmonycloud.zeus.operator.BaseOperator;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/03/24
 */
@Operator(paramTypes4One = Middleware.class)
public class BaseOperatorImpl extends AbstractBaseOperator implements BaseOperator {

    @Override
    public boolean support(Middleware middleware) {
        return MiddlewareTypeEnum.BASE == MiddlewareTypeEnum.findByType(middleware.getType());
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
    public void updateConfigData(ConfigMap configMap, List<String> data) {

    }

    @Override
    public void editConfigMapData(CustomConfig customConfig, List<String> data) {

    }

    @Override
    public List<IngressDTO> listHostNetworkAddress(String clusterId, String namespace, String middlewareName, String type) {
        return null;
    }

}
