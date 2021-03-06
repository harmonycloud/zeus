package com.harmonycloud.zeus.integration.cluster.api;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.tool.api.AbstractApi;
import com.harmonycloud.tool.api.client.BaseClient;
import okhttp3.Call;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static com.harmonycloud.caas.common.constants.NameConstant.ADMIN;
import static com.harmonycloud.tool.api.util.HttpMethod.POST;

/**
 * @author xutianhong
 * @Date 2021/11/16 7:16 下午
 */
public class AlertManagerApi extends AbstractApi {

    public AlertManagerApi(BaseClient baseClient){
        super(baseClient);
    }

    public void setSilence(Map<String, Object> body, String authName) throws Exception {
        Call call = this.localVarHarborClient.buildCall("", POST, body,
            StringUtils.isEmpty(authName) ? new String[] {} : new String[] {authName});
        this.localVarHarborClient.execute(call, JSONObject.class);
    }

}
