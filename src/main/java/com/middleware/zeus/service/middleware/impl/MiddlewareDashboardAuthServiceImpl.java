package com.middleware.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.tool.encrypt.RSAUtils;
import com.middleware.zeus.service.AbstractBaseService;
import com.middleware.zeus.service.dashboard.BaseMiddlewareApiService;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.middleware.MiddlewareDashboardAuthService;
import com.middleware.zeus.service.registry.HelmChartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.tomcat.util.http.MimeHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;

/**
 * @author xutianhong
 * @Date 2022/10/11 10:53 上午
 */
@Service
@Slf4j
public class MiddlewareDashboardAuthServiceImpl extends AbstractBaseService implements MiddlewareDashboardAuthService {

    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;

    @Override
    public JSONObject login(String clusterId, String namespace, String middlewareName, String username, String password,
        String type) {
        return login(clusterId, namespace, middlewareName, username, password, type, true);
    }

    @Override
    public JSONObject login(String clusterId, String namespace, String middlewareName, String username, String password, String type, Boolean encryptedPassword) {
        String realPassword;
        if (encryptedPassword) {
            try {
                // 解密密码
                realPassword = RSAUtils.decryptByPrivateKey(password);
            } catch (Exception e) {
                throw new BusinessException(ErrorMessage.RSA_DECRYPT_FAILED);
            }
        } else {
            realPassword = password;
        }
        // 根据类型前往不同的中间件尝试登录
        BaseMiddlewareApiService service =
                getOperator(BaseMiddlewareApiService.class, BaseMiddlewareApiService.class, type);
        String token = service.login(clusterId, namespace, middlewareName, username, realPassword);

        JSONObject res = new JSONObject();
        res.put("username", username);
        res.put("mwToken", token);
        return res;
    }

    @Override
    public void logout(String clusterId, String namespace, String middlewareName, String type) {
        // 根据类型前往不同的中间件尝试登录
        BaseMiddlewareApiService service =
                getOperator(BaseMiddlewareApiService.class, BaseMiddlewareApiService.class, type);
        service.logout(clusterId, namespace, middlewareName);
    }

    @Override
    public void addMWToken(String clusterId, String namespace, String middlewareName, String type) {
        JSONObject values = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        String username = "";
        String password = "";
        if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
            username = "root";
            JSONObject args = values.getJSONObject("args");
            if (args != null && args.containsKey("root_password")) {
                password = args.getString("root_password");
            } else {
                //TODO 获取mysql用户列表失败
            }
        } else if (MiddlewareTypeEnum.POSTGRESQL.getType().equals(type)) {
            username = "postgres";
            JSONObject userPasswords = values.getJSONObject("userPasswords");
            if (userPasswords != null && userPasswords.containsKey("postgres")) {
                password = userPasswords.getString("postgres");
            } else {
                //TODO 获取mysql用户列表失败
            }
        }

        JSONObject res = login(clusterId, namespace, middlewareName, username, password, type, false);
        String mwToken = res.getString("mwToken");
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        addMWToken(request, mwToken);
    }

    protected void addMWToken(HttpServletRequest req, String mwToken) {
        try {
            // 从 RequestFacade 中获取 org.apache.catalina.connector.Request
            Field connectorField = ReflectionUtils.findField(RequestFacade.class, "request", Request.class);
            connectorField.setAccessible(true);
            Request connectorRequest = (Request) connectorField.get(req);

            // 从 org.apache.catalina.connector.Request 中获取 org.apache.coyote.Request
            Field coyoteField = ReflectionUtils.findField(Request.class, "coyoteRequest", org.apache.coyote.Request.class);
            coyoteField.setAccessible(true);
            org.apache.coyote.Request coyoteRequest = (org.apache.coyote.Request) coyoteField.get(connectorRequest);

            // 从 org.apache.coyote.Request 中获取 MimeHeaders
            Field mimeHeadersField = ReflectionUtils.findField(org.apache.coyote.Request.class, "headers", MimeHeaders.class);
            mimeHeadersField.setAccessible(true);
            MimeHeaders mimeHeaders = (MimeHeaders) mimeHeadersField.get(coyoteRequest);
            mimeHeaders.addValue("mwToken").setString(mwToken);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
