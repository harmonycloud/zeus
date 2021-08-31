package com.harmonycloud.zeus.integration.cluster.bean.prometheus;

import lombok.Data;

/**
 * @author dengyulong
 * @date 2021/05/20
 */
@Data
public class GrafanaApiKey {

    private Integer id;

    private String name;

    private String role;

    private String expiration;

}
