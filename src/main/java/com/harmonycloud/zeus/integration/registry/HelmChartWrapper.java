package com.harmonycloud.zeus.integration.registry;

import java.io.File;
import java.util.List;

import org.springframework.stereotype.Component;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.Registry;
import com.harmonycloud.zeus.integration.registry.api.harbor.V1HelmChartApi;
import com.harmonycloud.zeus.integration.registry.bean.harbor.V1HelmChartVersion;
import com.harmonycloud.zeus.integration.registry.client.RegistryClientFactory;
import com.harmonycloud.zeus.util.ExceptionUtils;
import com.harmonycloud.tool.api.common.ApiException;

/**
 * @author dengyulong
 * @date 2021/03/29
 */
@Component
public class HelmChartWrapper {

    /**
     * 下载helm chart包
     */
    public List<V1HelmChartVersion> listHelmChartVersions(Registry registry, String chartName) {
        V1HelmChartApi helmChartApi = new V1HelmChartApi(RegistryClientFactory.getClient(registry));
        try {
            return helmChartApi.listHelmChartVersions(registry.getChartRepo(), chartName, null);
        } catch (ApiException e) {
            throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
        }
    }

    /**
     * 下载helm chart包
     */
    public File downloadHelmChart(Registry registry, String chartName, String chartVersion) {
        V1HelmChartApi helmChartApi = new V1HelmChartApi(RegistryClientFactory.getClient(registry));
        try {
            return helmChartApi.downloadChart("middleware", chartName, chartVersion, null);
        } catch (ApiException e) {
            throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
        }
    }

    /**
     * 上传helm chart包
     */
    public BaseResult uploadHelmChart(Registry registry, File file) {
        V1HelmChartApi helmChartApi = new V1HelmChartApi(RegistryClientFactory.getClient(registry));
        try {
            return helmChartApi.uploadChart("middleware", file, null);
        } catch (ApiException e) {
            throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
        }
    }

}
