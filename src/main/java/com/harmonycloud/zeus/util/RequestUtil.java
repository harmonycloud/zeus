package com.harmonycloud.zeus.util;

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
            log.debug("查询projectId 失败");
        }
        return null;
    }
    
}
