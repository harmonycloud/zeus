package com.harmonycloud.zeus.integration.cluster.api;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.PrometheusRulesResponse;
import com.harmonycloud.caas.common.model.PrometheusResponse;
import com.harmonycloud.tool.api.AbstractApi;
import com.harmonycloud.tool.api.client.BaseClient;
import com.harmonycloud.tool.api.common.Pair;
import com.harmonycloud.tool.api.common.RequestParams;
import okhttp3.Call;

import java.util.Map;

import static com.harmonycloud.tool.api.util.HttpMethod.GET;
import static com.harmonycloud.tool.api.util.HttpMethod.POST;

/**
 * @author xutianhong
 * @Date 2021/3/31 4:29 下午
 */
public class PrometheusApi extends AbstractApi {

    public PrometheusApi(BaseClient baseClient) {
        super(baseClient);
    }

    public PrometheusResponse getMonitorInfo(String url, Map<String, String> queryMap) throws Exception {
        RequestParams requestParams = new RequestParams();
        queryMap.forEach((k, v) -> {
            requestParams.getQuery().add(new Pair(k, v));
        });
        Call call = this.localVarHarborClient.buildCall(url, GET, requestParams, null, new String[] {});
        return (PrometheusResponse) this.localVarHarborClient.execute(call, PrometheusResponse.class).getData();
    }

    public PrometheusRulesResponse getRules() throws Exception{
        RequestParams requestParams = new RequestParams();
        Call call = this.localVarHarborClient.buildCall("", GET, requestParams, null, new String[] {});
        return (PrometheusRulesResponse) this.localVarHarborClient.execute(call, PrometheusRulesResponse.class).getData();
    }

    public void setSilence(Map<String, Object> body) throws Exception{
        Call call = this.localVarHarborClient.buildCall("", POST, body, new String[] {});
        this.localVarHarborClient.execute(call, JSONObject.class);
    }
}
