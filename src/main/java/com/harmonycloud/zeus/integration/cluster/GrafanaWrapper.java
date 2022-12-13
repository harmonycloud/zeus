package com.harmonycloud.zeus.integration.cluster;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.harmonycloud.zeus.integration.cluster.bean.prometheus.GrafanaApiKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.harmonycloud.zeus.integration.cluster.api.GrafanaApi;
import com.harmonycloud.zeus.integration.cluster.client.GrafanaClient;
import com.harmonycloud.zeus.util.ExceptionUtils;
import com.middleware.tool.api.common.ApiException;

/**
 * @author dengyulong
 * @date 2021/05/20
 */
@Component
public class GrafanaWrapper {

    private static final Map<String, String> sessionMap = new ConcurrentHashMap<>();

    public List<GrafanaApiKey> listApiKeys(MiddlewareClusterMonitorInfo grafana) {
        GrafanaApi grafanaApi = new GrafanaApi(new GrafanaClient(grafana));
        try {
            return grafanaApi.listApiKeys(getSession(grafana));
        } catch (ApiException e) {
            // 403 session失效
            if (e.getCode() == 403) {
                // 登录刷新session
                login(grafana);
                try {
                    // 再查询一次
                    return grafanaApi.listApiKeys(getSession(grafana));
                } catch (ApiException apiException) {
                    throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
                }
            }
            throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
        }
    }

    public String createApiKey(String name, MiddlewareClusterMonitorInfo grafana) {
        GrafanaApi grafanaApi = new GrafanaApi(new GrafanaClient(grafana));
        try {
            return grafanaApi.createApiKey(name, null, null, getSession(grafana));
        } catch (ApiException e) {
            // 403 session失效
            if (e.getCode() == 403) {
                // 登录刷新session
                login(grafana);
                try {
                    // 再查询一次
                    return grafanaApi.createApiKey(name, null, null, getSession(grafana));
                } catch (ApiException apiException) {
                    throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
                }
            }
            throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
        }
    }

    public void deleteApiKey(Integer id, MiddlewareClusterMonitorInfo grafana) {
        GrafanaApi grafanaApi = new GrafanaApi(new GrafanaClient(grafana));
        try {
            grafanaApi.deleteApiKeys(id, getSession(grafana));
        } catch (ApiException e) {
            // 403 session失效
            if (e.getCode() == 403) {
                // 登录刷新session
                login(grafana);
                try {
                    // 再查询一次
                    grafanaApi.deleteApiKeys(id, getSession(grafana));
                } catch (ApiException apiException) {
                    throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
                }
            }
            throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
        }
    }

    private String getSession(MiddlewareClusterMonitorInfo grafana) {
        if (sessionMap.get(grafana.getHost()) == null) {
            login(grafana);
        }
        return sessionMap.get(grafana.getHost());
    }

    private void login(MiddlewareClusterMonitorInfo grafana) {
        GrafanaApi grafanaApi = new GrafanaApi(new GrafanaClient(grafana));
        try {
            String session;
            if (StringUtils.isNotEmpty(grafana.getPassword()) && StringUtils.isNotEmpty(grafana.getPassword())) {
                session = grafanaApi.login(grafana.getUsername(), grafana.getPassword());
            } else {
                session = grafanaApi.login(null, null);
            }
            if (session == null) {
                throw new BusinessException(ErrorMessage.GRAFANA_LOGIN_FAIL, grafana.getAddress());
            }
            sessionMap.put(grafana.getHost(), session);
        } catch (ApiException e) {
            throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
        }
    }

}
