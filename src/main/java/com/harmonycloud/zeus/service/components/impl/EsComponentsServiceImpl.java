package com.harmonycloud.zeus.service.components.impl;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterLogging;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterLoggingInfo;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.EsComponentsService;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author xutianhong
 * @Date 2021/11/4 9:42 上午
 */
@Service
@Operator(paramTypes4One = String.class)
public class EsComponentsServiceImpl extends AbstractBaseOperator implements EsComponentsService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.ELASTICSEARCH.getName().equals(name);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster) {
        return "image.repository=" + repository +
                ",elasticsearch-operator.enabled=false" +
                ",elasticPassword=Hc@Cloud01" +
                ",storage.masterClass=local-path" +
                ",logging.collection.filelog.enable=false" +
                ",logging.collection.stdout.enable=false" +
                ",resources.master.limits.cpu=0.5" +
                ",resources.master.limits.memory=1Gi" +
                ",resources.master.requests.cpu=0.5" +
                ",resources.master.requests.memory=1Gi";
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.upgradeInstall("middleware-elasticsearch", "logging", setValues,
                componentsPath + File.separator + "elasticsearch", cluster);
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {
        MiddlewareClusterLoggingInfo es = new MiddlewareClusterLoggingInfo();
        es.setHost(cluster.getHost());
        es.setUser("elastic");
        es.setPassword("Hc@Cloud01");
        es.setProtocol("http");
        es.setPort("30092");
        MiddlewareClusterLogging logging = new MiddlewareClusterLogging();
        logging.setElasticSearch(es);
        cluster.setLogging(logging);
        clusterService.updateCluster(cluster);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {

    }
}
