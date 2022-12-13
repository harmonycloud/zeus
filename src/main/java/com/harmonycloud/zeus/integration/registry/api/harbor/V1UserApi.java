package com.harmonycloud.zeus.integration.registry.api.harbor;

import com.harmonycloud.zeus.integration.registry.bean.harbor.V1CurrentUser;
import com.middleware.tool.api.AbstractApi;
import com.middleware.tool.api.client.BaseClient;
import com.middleware.tool.api.common.ApiException;
import okhttp3.Call;

import static com.middleware.caas.common.constants.NameConstant.ADMIN;
import static com.middleware.tool.api.util.HttpMethod.GET;

/**
 * @author dengyulong
 * @date 2021/05/16
 */
public class V1UserApi extends AbstractApi {

    public V1UserApi(BaseClient baseClient) {
        super(baseClient);
    }

    public V1CurrentUser getCurrentUser(String authName) throws ApiException {
        Call call = this.localVarHarborClient.buildCall("/users/current", GET, null, new String[] {authName == null ? ADMIN : authName});
        Object data = this.localVarHarborClient.execute(call, V1CurrentUser.class).getData();
        return (V1CurrentUser)data;
    }

}
