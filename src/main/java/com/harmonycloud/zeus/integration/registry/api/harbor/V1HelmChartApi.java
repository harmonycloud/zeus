package com.harmonycloud.zeus.integration.registry.api.harbor;

import static com.middleware.caas.common.constants.NameConstant.ADMIN;
import static com.middleware.caas.common.constants.registry.HelmChartConstant.CHART_REPO_DIR_NAME;
import static com.middleware.tool.api.util.HttpMethod.GET;
import static com.middleware.tool.api.util.HttpMethod.POST;

import java.io.File;
import java.util.List;

import com.harmonycloud.zeus.integration.registry.bean.harbor.V1HelmChartVersion;
import org.springframework.http.HttpStatus;

import com.alibaba.fastjson.TypeReference;
import com.middleware.caas.common.base.BaseResult;
import com.middleware.tool.api.AbstractApi;
import com.middleware.tool.api.client.BaseClient;
import com.middleware.tool.api.common.ApiException;
import com.middleware.tool.api.common.ApiResponse;
import com.middleware.tool.api.common.RequestParams;

import okhttp3.Call;

/**
 * @author dengyulong
 * @date 2021/03/29
 */
public class V1HelmChartApi extends AbstractApi {
    
    public V1HelmChartApi(BaseClient baseClient) {
        super(baseClient);
    }

    /**
     * 下载chart文件
     * 
     * @param repositoryName 仓库名称
     * @param chartName      chart名称
     * @param authName       授权用户
     * @return
     * @throws ApiException
     */
    public List<V1HelmChartVersion> listHelmChartVersions(String repositoryName, String chartName, String authName)
        throws ApiException {
        Call call = this.localVarHarborClient.buildCall(
            "/" + CHART_REPO_DIR_NAME + "/" + repositoryName + "/charts/" + chartName, GET, null,
            new String[] {authName == null ? ADMIN : authName});
        ApiResponse<List<V1HelmChartVersion>> response =
            this.localVarHarborClient.execute(call, new TypeReference<List<V1HelmChartVersion>>() {}.getType());
        return response.getData();
    }

    /**
     * 下载chart文件
     * 
     * @param repositoryName 仓库名称
     * @param chartName      chart名称
     * @param chartVersion   chart版本
     * @param authName       授权用户
     * @return
     * @throws ApiException
     */
    public File downloadChart(String repositoryName, String chartName, String chartVersion, String authName)
        throws ApiException {
        RequestParams requestParams = new RequestParams();
        requestParams.setUseBasePath(false);
        String chartFileName = getTgzFileName(chartName, chartVersion);
        Call call = this.localVarHarborClient.buildCall(
            "/" + CHART_REPO_DIR_NAME + "/" + repositoryName + "/charts/" + chartFileName, GET, requestParams, null,
            new String[] {authName == null ? ADMIN : authName});
        ApiResponse<File> response = this.localVarHarborClient.execute(call, File.class);
        return response.getData();
    }

    private String getTgzFileName(String chartName, String chartVersion) {
        return chartName + "-" + chartVersion + ".tgz";
    }

    /**
     * 上传chart文件
     *
     * @param repositoryName 仓库名称
     * @param file           文件
     * @param authName       授权用户
     * @return
     * @throws ApiException
     */
    public BaseResult uploadChart(String repositoryName, File file, String authName) throws ApiException {
        RequestParams requestParams = new RequestParams();
        requestParams.getHeader().put("Content-Type", "multipart/form-data");
        requestParams.getForm().put("chart", file);
        Call call = this.localVarHarborClient.buildCall("/" + CHART_REPO_DIR_NAME + "/" + repositoryName + "/charts",
            POST, requestParams, null, new String[] {authName == null ? ADMIN : authName});
        ApiResponse<String> response = this.localVarHarborClient.execute(call, String.class);
        if (HttpStatus.CREATED.value() == response.getStatusCode()) {
            return BaseResult.ok();
        } else {
            return BaseResult.error().setErrorMsg(response.getData());
        }
    }

}
