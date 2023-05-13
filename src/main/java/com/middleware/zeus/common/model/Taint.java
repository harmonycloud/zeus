package com.middleware.zeus.common.model;

import lombok.Data;

/**
 * @author anson
 * @since 2020/12/30 17:35
 */
@Data
public class Taint {
    private String effect;
    private String key;
    private String timeAdded;
    private String value;
}
