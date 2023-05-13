package com.middleware.zeus.util.api.common.auth;

import com.middleware.zeus.util.api.common.Pair;
import okhttp3.Credentials;

import java.util.List;
import java.util.Map;

public class HttpBasicAuth implements Authentication {
    private String username;
    private String password;

    public HttpBasicAuth() {
    }

    public HttpBasicAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void applyToParams(List<Pair> query, Map<String, String> header, Map<String, String> cookie) {
        if (username == null && password == null) {
            return;
        }
        header.put("Authorization", Credentials.basic(username == null ? "" : username, password == null ? "" : password));
    }
}
