package com.middleware.zeus.config;

import com.middleware.zeus.filter.AuthFilter;
import com.middleware.zeus.filter.TokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.middleware.caas.filters.filters.CurrentStateFilter;

/**
 * @author chwetion
 * @since 2020/12/22 8:34 下午
 */
@Configuration
public class FiltersConfiguration {

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilter() {
        FilterRegistrationBean<AuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new AuthFilter());
        bean.setName("authFilter");
        bean.addUrlPatterns("/*");
        bean.setOrder(-1);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<TokenFilter> tokenFilter() {
        FilterRegistrationBean<TokenFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new TokenFilter());
        bean.setName("tokenFilter");
        bean.addUrlPatterns("/*");
        bean.setOrder(0);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<CurrentStateFilter> currentStateFilter() {
        FilterRegistrationBean<CurrentStateFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new CurrentStateFilter());
        bean.setName("currentStateFilter");
        bean.addUrlPatterns("/*");
        bean.setOrder(1);
        return bean;
    }

}