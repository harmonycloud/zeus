package com.middleware.zeus.config;

import com.middleware.zeus.interceptor.ZeusInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author dengyulong
 * @date 2021/01/21
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 增加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 制品服务的拦截器，只拦截middleware的请求
        registry.addInterceptor(new ZeusInterceptor()).addPathPatterns(
            "/clusters/**", "/middlewares/**");
    }

}
