package com.harmonycloud.zeus.service.middleware;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @Author: zack chen
 * @Date: 2021/5/14 11:02 上午
 */
public interface MiddlewareUploadService {

    /**
     * 中间件上架
     * @param clusterId
     * @param file
     */
    void upload(String clusterId, MultipartFile file);
}
