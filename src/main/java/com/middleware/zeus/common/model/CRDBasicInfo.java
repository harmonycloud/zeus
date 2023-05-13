package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * crd基础信息
 * @author liyinlong
 * @since 2023/3/22 10:26 上午
 */
@Accessors(chain = true)
@Data
public class CRDBasicInfo {

    private String group;

    private String version;

    private String scope;

    private String singular;

    private String plural;

    public CRDBasicInfo(String group, String version, String scope, String singular, String plural) {
        this.group = group;
        this.version = version;
        this.scope = scope;
        this.singular = singular;
        this.plural = plural;
    }
}
