package com.harmonycloud.zeus.service.ingress.api;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.model.TraefikPort;
import com.harmonycloud.zeus.service.ingress.BaseIngressService;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/8/23 4:37 下午
 */
public interface TraefikIngressService extends BaseIngressService {

    /**
     * 获取traefik端口范围
     * @param values values.yaml
     * @return List<TraefikPort>
     */
    List<TraefikPort> getTraefikPort(JSONObject values);

}
