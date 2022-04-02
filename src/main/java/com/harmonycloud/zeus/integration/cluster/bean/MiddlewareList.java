package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author tangtx
 * @date 2021/03/26 11:00 AM
 */
@Accessors(chain = true)
@Data
public class MiddlewareList {

    private String apiVersion = "harmonycloud.cn/v1";

    private String kind = "Middleware";

    private ObjectMeta metadata;

    private List<MiddlewareCR> items;
}
