package com.harmonycloud.zeus.integration.registry.api.harbor;

import static com.harmonycloud.tool.api.util.HttpMethod.GET;

import com.harmonycloud.zeus.integration.registry.bean.harbor.V1SystemInfo;
import com.harmonycloud.tool.api.AbstractApi;
import com.harmonycloud.tool.api.client.BaseClient;
import com.harmonycloud.tool.api.common.ApiException;

import okhttp3.Call;

/**
 * @author dengyulong
 * @date 2021/05/24
 */
public class V1SystemApi extends AbstractApi {

    public V1SystemApi(BaseClient baseClient) {
        super(baseClient);
    }

    public V1SystemInfo getSystemInfo() throws ApiException {
        Call call = this.localVarHarborClient.buildCall("/systeminfo", GET, null, new String[] {});
        Object data = this.localVarHarborClient.execute(call, V1SystemInfo.class).getData();
        return (V1SystemInfo)data;
    }
    
}
