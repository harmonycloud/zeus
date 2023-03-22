package com.middleware.zeus.service.system;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.model.ServicePort;
import com.middleware.caas.common.model.URLInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @auther wangpenglei
 * @date 2023/3/22 10:33
 */
public interface PlatformService {

    JSONObject queryAccessInfo();

    void saveRelationAddr(URLInfo urlInfo, String spareName);

    void saveChiefAddr(URLInfo urlInfo, String chiefName);

    void switchPlatform(HttpServletRequest request) throws IOException;
}
