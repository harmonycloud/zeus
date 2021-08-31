package com.harmonycloud.zeus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.harmonycloud.caas.filters.feign.FeignBasicAuthRequestInterceptor;

import feign.RequestInterceptor;

/**
 * @author dengyulong
 * @date 2021/01/19
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new FeignBasicAuthRequestInterceptor();
    }

}
