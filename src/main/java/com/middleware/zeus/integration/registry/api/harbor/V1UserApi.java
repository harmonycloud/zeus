package com.middleware.zeus.integration.registry.api.harbor;

import com.middleware.zeus.integration.registry.bean.harbor.V1CurrentUser;
import com.middleware.zeus.util.api.AbstractApi;
import com.middleware.zeus.util.api.client.BaseClient;
import com.middleware.zeus.util.api.common.ApiException;
import okhttp3.Call;

import static com.middleware.zeus.common.constants.NameConstant.ADMIN;
import static com.middleware.zeus.util.api.util.HttpMethod.GET;

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
