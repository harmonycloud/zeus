package com.middleware.zeus.util.api.common.auth;

import com.middleware.zeus.util.api.common.Pair;

import java.util.List;
import java.util.Map;

/**
 * @author chwetion
 * @since 2020/12/9 2:12 下午
 */
public class HttpBearerAuth implements Authentication {
    private final String schema;
    private String bearerToken;

    public HttpBearerAuth(String schema) {
        this.schema = schema;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public HttpBearerAuth setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
        return this;
    }

    @Override
    public void applyToParams(List<Pair> query, Map<String, String> header, Map<String, String> cookie) {
        if (this.bearerToken == null) {
            return;
        }
        header.put("Authorization", (schema != null ? upperCaseBearer(schema) + " " : "") + this.bearerToken);
    }

    private static String upperCaseBearer(String scheme) {
        return ("bearer".equalsIgnoreCase(scheme)) ? "Bearer" : scheme;
    }
}
