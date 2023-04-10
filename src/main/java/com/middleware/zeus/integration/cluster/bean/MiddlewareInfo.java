package com.middleware.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author tangtx
 * @date 2021/03/26 11:00 AM
 */
@Accessors(chain = true)
@Data
public class MiddlewareInfo {

    private String name;

    private String type;

    private int orderNum;

    private String aliasType;

}
