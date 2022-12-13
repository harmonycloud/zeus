package com.middleware.zeus.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xutianhong
 * @Date 2021/9/9 5:26 下午
 */
@Configuration
public class HttpConfig {

    @Value("${server.http.port}")
    private Integer port;

    @Bean
    public ServletWebServerFactory servletWebServerFactory(){
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addAdditionalTomcatConnectors(createConnector());
        return tomcat;
    }

    public Connector createConnector(){
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(port);
        return connector;
    }

}
