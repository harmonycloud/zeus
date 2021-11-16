package com.harmonycloud.zeus.integration.cluster.api;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.tool.api.AbstractApi;
import com.harmonycloud.tool.api.client.BaseClient;
import okhttp3.Call;

import java.util.Map;

import static com.harmonycloud.tool.api.util.HttpMethod.POST;

/**
 * @author xutianhong
 * @Date 2021/11/16 7:16 下午
 */
public class AlertManagerApi extends AbstractApi {

    public AlertManagerApi(BaseClient baseClient){
        super(baseClient);
    }

    public void setSilence(Map<String, Object> body) throws Exception{
        Call call = this.localVarHarborClient.buildCall("", POST, body, new String[] {});
        this.localVarHarborClient.execute(call, JSONObject.class);
    }

}
