package com.middleware.zeus.util.api.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestParams {
    private final List<Pair> query = new ArrayList<>();
    private final Map<String, String> header = new HashMap<>();
    private final Map<String, String> cookie = new HashMap<>();
    private final Map<String, Object> form = new HashMap<>();

    private boolean useBasePath = true;

    public List<Pair> getQuery() {
        return query;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public Map<String, String> getCookie() {
        return cookie;
    }

    public Map<String, Object> getForm() {
        return form;
    }

    public boolean isUseBasePath() {
        return useBasePath;
    }

    public void setUseBasePath(boolean useBasePath) {
        this.useBasePath = useBasePath;
    }
}
