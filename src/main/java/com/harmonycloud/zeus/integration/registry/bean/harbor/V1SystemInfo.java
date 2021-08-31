package com.harmonycloud.zeus.integration.registry.bean.harbor;

import lombok.Data;

/**
 * @author dengyulong
 * @date 2020/12/07
 */
@Data
public class V1SystemInfo {

    private String admiralEndpoint;
    private String authMode;
    private String externalUrl;
    private String harborVersion;
    private Boolean hasCaRoot;
    private Boolean notificationEnable;
    private String projectCreationRestriction;
    private Boolean readOnly;
    private String registryStorageProviderName;
    private String registryUrl;
    private Boolean selfRegistration;
    private Boolean withAdmiral;
    private Boolean withChartmuseum;
    private Boolean withNotary;

}
