package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author anson
 * @since 2021/1/4 16:15
 */
@Accessors(chain = true)
@Data
public class NodeResource {
    String total;
    String allocated;
    String used;
}
