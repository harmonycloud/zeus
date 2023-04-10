package com.middleware.zeus.filter;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/4/7 1:49 下午
 */
@Slf4j
public class CurrentStateFilter implements Filter {
    private static final String ROLE_ID_LEY = "roleId";
    private static final String PROJECT_ID_KEY = "projectId";
    private static final String ORGAN_ID_KEY = "organId";

    public CurrentStateFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        log.debug("current state filter is in calling");
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        CurrentUser currentUser = CurrentUserRepository.getUserExistNull();
        if (currentUser != null) {
            currentUser.setTenantId(request.getHeader(ORGAN_ID_KEY))
                    .setRoleId(request.getHeader(ROLE_ID_LEY))
                    .setProjectId(request.getHeader(PROJECT_ID_KEY));
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
