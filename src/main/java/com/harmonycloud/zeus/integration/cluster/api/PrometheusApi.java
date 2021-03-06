package com.harmonycloud.zeus.integration.cluster.api;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.PrometheusRulesResponse;
import com.harmonycloud.caas.common.model.PrometheusResponse;
import com.harmonycloud.tool.api.AbstractApi;
import com.harmonycloud.tool.api.client.BaseClient;
import com.harmonycloud.tool.api.common.Pair;
import com.harmonycloud.tool.api.common.RequestParams;
import lombok.Data;
import okhttp3.Call;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Map;

import static com.harmonycloud.caas.common.constants.NameConstant.ADMIN;
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
