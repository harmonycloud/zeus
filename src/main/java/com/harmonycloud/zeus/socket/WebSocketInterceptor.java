package com.harmonycloud.zeus.socket;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;

/**
 * @author dengyulong
 * @date 2021/05/19
 */
public class WebSocketInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> map) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest)request;
            HttpSession session = servletRequest.getServletRequest().getSession();
            if (session != null) {
                // 根据用户名(和角色)区分区分socket连接以定向发送消息
                CurrentUser user = CurrentUserRepository.getUser();
                if (user.getRoleId() != null) {
                    map.put("roleId", user.getRoleId());
                }
                map.put("username", user.getUsername());
            }
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("projectId"))) {
                map.put("projectId", httpServletRequest.getParameter("projectId"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("clusterId"))) {
                map.put("clusterId", httpServletRequest.getParameter("clusterId"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("type"))) {
                map.put("type", httpServletRequest.getParameter("type"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("id"))) {
                map.put("id", httpServletRequest.getParameter("id"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("scriptType"))) {
                map.put("scriptType", httpServletRequest.getParameter("scriptType"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("terminalType"))) {
                map.put("terminalType", httpServletRequest.getParameter("terminalType"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("container"))) {
                map.put("container", httpServletRequest.getParameter("container"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("pod"))) {
                map.put("pod", httpServletRequest.getParameter("pod"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("namespace"))) {
                map.put("namespace", httpServletRequest.getParameter("namespace"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("logDir"))) {
                map.put("logDir", httpServletRequest.getParameter("logDir"));
            }
            if (StringUtils.isNotBlank(httpServletRequest.getParameter("logFile"))) {
                map.put("logFile", httpServletRequest.getParameter("logFile"));
            }

        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                               Exception e) {
        // 如果有websocket的子协议，则response里也需要把子协议带上，否则握手失败
        // todo：当前子协议的值是gateway颁发的token，如果真有子协议时需要再处理
        if (StringUtils.isNotBlank(request.getHeaders().getFirst(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL))) {
            response.getHeaders().set(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL,
                request.getHeaders().getFirst(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL));
        }
    }
}
