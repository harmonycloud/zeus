package com.middleware.zeus.integration.cluster.api;

import static com.middleware.tool.api.util.HttpMethod.DELETE;
import static com.middleware.tool.api.util.HttpMethod.GET;
import static com.middleware.tool.api.util.HttpMethod.POST;

import java.util.List;

import com.middleware.zeus.integration.cluster.bean.prometheus.GrafanaApiKey;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.middleware.tool.api.AbstractApi;
import com.middleware.tool.api.client.BaseClient;
import com.middleware.tool.api.common.ApiException;
import com.middleware.tool.api.common.ApiResponse;
import com.middleware.tool.api.common.RequestParams;

import okhttp3.Call;

/**
 * @author dengyulong
 * @date 2021/05/20
 */
public class GrafanaApi extends AbstractApi {

    public GrafanaApi(BaseClient baseClient) {
        super(baseClient);
    }

    /**
     * 登录并返回cookie
     * 
     * @param username 用户名
     * @param password 密码
     * @return
     * @throws ApiException
     */
    public String login(String username, String password) throws ApiException {
        JSONObject param = new JSONObject();
        param.put("user", StringUtils.isBlank(username) ? "admin" : username);
        param.put("password", StringUtils.isBlank(password) ? "admin" : password);

        Call call = this.localVarHarborClient.buildCall("/login", POST, param, new String[] {});
        ApiResponse<JSONObject> response = this.localVarHarborClient.execute(call, JSONObject.class);
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        for (String cookie : cookies) {
            if (cookie.startsWith("grafana_session")) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * 查看 API Key列表
     *
     * @param grafanaSession 登录之后的session
     * @return
     * @throws ApiException
     */
    public List<GrafanaApiKey> listApiKeys(String grafanaSession) throws ApiException {
        // 请求头参数
        RequestParams requestParams = new RequestParams();
        requestParams.getHeader().put("Cookie", grafanaSession);

        Call call = this.localVarHarborClient.buildCall("/api/auth/keys?includeExpired=false", GET, requestParams, null, new String[] {});
        ApiResponse<List<GrafanaApiKey>> response = this.localVarHarborClient.execute(call, new TypeReference<List<GrafanaApiKey>>() {}.getType());
        return response.getData();
    }

    /**
     * 创建 API Key
     *
     * @param name           key名称
     * @param role           操作角色
     * @param secondsToLive  有效期
     * @param grafanaSession 登录之后的session
     * @return
     * @throws ApiException
     */
    public String createApiKey(String name, String role, Long secondsToLive, String grafanaSession)
            throws ApiException {
        // 请求头参数
        RequestParams requestParams = new RequestParams();
        requestParams.getHeader().put("Cookie", grafanaSession);

        // 请求体参数
        JSONObject body = new JSONObject();
        body.put("name", StringUtils.isBlank(name) ? "caas-middleware" : name);
        // 总共三种权限：Viewer/Editor/Admin，默认Viewer
        body.put("role", StringUtils.isBlank(role) ? "Viewer" : role);
        // 默认100年过期
        body.put("secondsToLive", secondsToLive == null ? 3153600000L : secondsToLive);

        Call call = this.localVarHarborClient.buildCall("/api/auth/keys", POST, requestParams, body, new String[] {});
        ApiResponse<JSONObject> response = this.localVarHarborClient.execute(call, JSONObject.class);
        return response.getData().getString("key");
    }

    /**
     * 删除 API Key
     * @param id             id
     * @param grafanaSession 登录之后的session
     * @throws ApiException
     */
    public void deleteApiKeys(Integer id, String grafanaSession) throws ApiException {
        // 请求头参数
        RequestParams requestParams = new RequestParams();
        requestParams.getHeader().put("Cookie", grafanaSession);

        Call call =
            this.localVarHarborClient.buildCall("/api/auth/keys/" + id, DELETE, requestParams, null, new String[] {});
        this.localVarHarborClient.execute(call, JSONObject.class);
    }

}
