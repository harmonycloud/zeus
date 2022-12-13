package com.harmonycloud.zeus.integration.cluster.api;

import com.middleware.caas.common.model.PrometheusRulesResponse;
import com.middleware.caas.common.model.PrometheusResponse;
import com.middleware.tool.api.AbstractApi;
import com.middleware.tool.api.client.BaseClient;
import com.middleware.tool.api.common.Pair;
import com.middleware.tool.api.common.RequestParams;
import okhttp3.Call;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Map;

import static com.middleware.tool.api.util.HttpMethod.GET;

/**
 * @author xutianhong
 * @Date 2021/3/31 4:29 下午
 */
public class PrometheusApi extends AbstractApi {

    public PrometheusApi(BaseClient baseClient) {
        super(baseClient);
    }

    public PrometheusResponse get(String url, Map<String, String> queryMap, String authName) throws Exception {
        RequestParams requestParams = new RequestParams();
        if (!CollectionUtils.isEmpty(queryMap)) {
            queryMap.forEach((k, v) -> requestParams.getQuery().add(new Pair(k, v)));
        }
        Call call = this.localVarHarborClient.buildCall(url, GET, requestParams, null,
            StringUtils.isEmpty(authName) ? new String[] {} : new String[] {authName});
        return (PrometheusResponse)this.localVarHarborClient.execute(call, PrometheusResponse.class).getData();
    }

    public PrometheusRulesResponse getRules(String authName) throws Exception {
        RequestParams requestParams = new RequestParams();
        Call call = this.localVarHarborClient.buildCall("", GET, requestParams, null,
            StringUtils.isEmpty(authName) ? new String[] {} : new String[] {authName});
        return (PrometheusRulesResponse)this.localVarHarborClient.execute(call, PrometheusRulesResponse.class)
            .getData();
    }
}
