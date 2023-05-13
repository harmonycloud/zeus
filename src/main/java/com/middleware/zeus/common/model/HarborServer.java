package com.middleware.zeus.common.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author tangtx
 * @date 2019-09-24 15:26
 */
@Data
public class HarborServer implements Serializable {

    private static final long serialVersionUID = 2L;

    private String harborProtocol;
    private String harborHost;
    private Integer harborPort;
    private String harborAdminAccount;
    private String harborAdminPassword;
    private String registryAddress;

    public String getHarborAddress(){
        if(!"443".equals(harborPort.toString()) && !"80".equals(harborPort.toString())){
            return harborHost + ":" + harborPort;
        }
        return harborHost;
    }
}
