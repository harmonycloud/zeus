package com.middleware.zeus.socket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * @author dengyulong
 * @date 2021/05/19
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(terminalSocketHandler(), "/terminal").addInterceptors(webSocketInterceptor())
            .setAllowedOrigins("*");
        registry.addHandler(terminalSocketHandler(), "/terminal").addInterceptors(webSocketInterceptor())
            .setAllowedOrigins("*").withSockJS();
    }

    @Bean
    public HandshakeInterceptor webSocketInterceptor() {
        return new WebSocketInterceptor();
    }

    @Bean
    public WebSocketHandler terminalSocketHandler() {
        return new PerConnectionWebSocketHandler(TerminalSocketHandler.class);
    }

}
