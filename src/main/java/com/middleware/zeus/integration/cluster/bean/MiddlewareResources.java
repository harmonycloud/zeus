package com.middleware.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author tangtx
 * @date 2021/03/26 11:00 AM
 */
@Accessors(chain = true)
@Data
public class MiddlewareResources {

    private Map<String, String> limits;

    private Map<String, String> requests;
}
