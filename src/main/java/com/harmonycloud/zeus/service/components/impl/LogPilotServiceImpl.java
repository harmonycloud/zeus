package com.harmonycloud.zeus.service.components.impl;

import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.LogPilotService;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author xutianhong
 * @Date 2021/11/4 9:53 上午
 */
@Service
@Operator(paramTypes4One = String.class)
public class LogPilotServiceImpl extends AbstractBaseOperator implements LogPilotService {

    @Override
    public boolean support(String name) {
        return ComponentsEnum.LOGPILOT.getName().equals(name);
    }

    @Override
    public void deploy(MiddlewareClusterDTO cluster, String type) {
        //创建分区
        namespaceService.save(cluster.getId(), "logging", null);
        //发布logPilot
        super.deploy(cluster, type);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {

    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, String type) {
        return "image.logpilotRepository=" + repository + "/log-pilot" +
                ",image.logstashRepository=" + repository + "/logstash";
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        helmChartService.upgradeInstall("log", "logging", setValues,
                componentsPath + File.separator + "logging", cluster);
    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {

    }
}
