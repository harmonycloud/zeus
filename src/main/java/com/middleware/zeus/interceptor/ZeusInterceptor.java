package com.middleware.zeus.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.middleware.zeus.service.k8s.impl.ClusterServiceImpl;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.middleware.caas.common.base.CurrentLanguage;
import com.middleware.caas.filters.enumm.LanguageEnum;

/**
 * @author dengyulong
 * @date 2021/01/21
 * 拦截器
 */
public class ZeusInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 进入controller层之前的处理
        CurrentLanguage.setLanguage(LanguageEnum.getCurrentLanguage());
        ClusterServiceImpl.refreshCache();
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
        @Nullable ModelAndView modelAndView) throws Exception {
        // 请求处理完成之后的处理（视图渲染之前）
        CurrentLanguage.clear();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
        @Nullable Exception ex) throws Exception {
        // 视图渲染之后的处理
    }

}
