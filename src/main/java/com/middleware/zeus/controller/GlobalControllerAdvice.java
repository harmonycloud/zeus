package com.middleware.zeus.controller;

import org.springframework.core.annotation.Order;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * @author xutianhong
 * 解决Spring漏洞 CVE-2022-22950
 * @Date 2022/4/1 1:46 下午
 */
@ControllerAdvice
@Order
public class GlobalControllerAdvice {

    @InitBinder
    public void setAllowedFields(WebDataBinder webDataBinder){
        String[] abd = new String[]{"class.*", "Class.*", "*.class.*", "*.Class.*", "a"};
        webDataBinder.setDisallowedFields(abd);
    }

}


