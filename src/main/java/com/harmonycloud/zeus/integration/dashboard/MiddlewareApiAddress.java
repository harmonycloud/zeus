package com.harmonycloud.zeus.integration.dashboard;

import com.dtflys.forest.callback.AddressSource;
import com.dtflys.forest.http.ForestAddress;
import com.dtflys.forest.http.ForestRequest;
import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xutianhong
 * @Date 2022/10/20 7:28 下午
 */
@Slf4j
@Component
public class MiddlewareApiAddress implements AddressSource {

    @Value("${system.middleware-api.protocol:http}")
    private String protocol;
    @Value("${system.middleware-api.host:127.0.0.1}")
    private String host;
    @Value("${system.middleware-api.port:8088}")
    private Integer port;

    @Autowired
    private ClusterComponentService clusterComponentService;

    @Override
    public ForestAddress getAddress(ForestRequest forestRequest) {
        HttpServletRequest servletRequest =
            ((ServletRequestAttributes)Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getRequest();
        String url = servletRequest.getRequestURI();
        Matcher matcher = Pattern.compile("clusters/[a-z|\\-|0-9]+").matcher(url);
        String clusterId = "";
        if (matcher.find()) {
            clusterId = matcher.group().split("/")[1];
        }
        // 根据集群id获取middleware-api组件地址
        ClusterComponentsDto clusterComponentsDto =
            clusterComponentService.get(clusterId, ComponentsEnum.MIDDLEWARE_CONTROLLER.getName());
        if (StringUtils.isNotEmpty(clusterComponentsDto.getProtocol())
            && StringUtils.isNotEmpty(clusterComponentsDto.getHost())
            && StringUtils.isNotEmpty(clusterComponentsDto.getPort())) {
            protocol = clusterComponentsDto.getProtocol();
            host = clusterComponentsDto.getHost();
            port = Integer.parseInt(clusterComponentsDto.getPort());
        }
        // 返回 Forest 地址对象
        return new ForestAddress(protocol, host, port);
    }
}
