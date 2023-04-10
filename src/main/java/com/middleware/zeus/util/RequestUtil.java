package com.middleware.zeus.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xutianhong
 * @Date 2022/4/6 5:11 下午
 */
@Slf4j
public class RequestUtil {
    
    public static String getProjectId() {
        try {
            HttpServletRequest request =
                    ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
            return request.getHeader("projectId");
        } catch (Exception e){
            log.debug("获取项目id失败", e);
        }
        return null;
    }

    public static String getOrganId() {
        try {
            HttpServletRequest request =
                    ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
            return request.getHeader("organId");
        } catch (Exception e){
            log.debug("获取组织id失败", e);
        }
        return null;
    }
    
}
