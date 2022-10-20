package com.harmonycloud.zeus.integration.dashboard;

import com.dtflys.forest.callback.AddressSource;
import com.dtflys.forest.http.ForestAddress;
import com.dtflys.forest.http.ForestRequest;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
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
                ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                        .getRequest();
        String url = servletRequest.getRequestURI();
        Matcher matcher = Pattern.compile("clusters/[a-z|\\-|0-9]+").matcher(url);
        String clusterId = "";
        if (matcher.find()){
            clusterId = matcher.group().split("/")[1];
        }
        // todo 根据集群id获取middleware-api组件地址  有待改造   暂时写死

        // 返回 Forest 地址对象
        return new ForestAddress(protocol, host, port);
    }
}
