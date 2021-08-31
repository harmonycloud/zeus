package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Accessors(chain = true)
@Data
public class MiddlewareClusterSpec {

    private MiddlewareClusterInfo info;

}
