package com.middleware.zeus.util.api;

import com.middleware.zeus.util.api.client.BaseClient;

/**
 * @author chwetion
 * @since 2020/11/29 11:15 下午
 */
public abstract class AbstractApi {
    protected final BaseClient localVarHarborClient;

    public AbstractApi(BaseClient baseClient) {
        this.localVarHarborClient = baseClient;
    }
}
