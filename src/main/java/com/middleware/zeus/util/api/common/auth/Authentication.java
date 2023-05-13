package com.middleware.zeus.util.api.common.auth;

import com.middleware.zeus.util.api.common.Pair;

import java.util.List;
import java.util.Map;

public interface Authentication {
    void applyToParams(List<Pair> query, Map<String, String> header, Map<String, String> cookie);
}
