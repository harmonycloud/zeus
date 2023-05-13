package com.middleware.zeus.common.model.registry;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/07
 */
@Accessors(chain = true)
@Data
public class SystemInfo {

    private String authMode;

    private String externalUrl;

    private String version;

    private String majorVersion;

    private String minorVersion;

    private String registryUrl;

}
