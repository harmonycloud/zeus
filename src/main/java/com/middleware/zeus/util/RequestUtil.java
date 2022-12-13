package com.middleware.zeus.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xutianhong
 * @Date 2022/4/6 5:11 下午
 */
public class RequestUtil {
    
    public static String getProjectId() {
        HttpServletRequest request =
            ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
        return request.getHeader("projectId");
    }
    
}
