package com.middleware.zeus.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @auther wangpenglei
 * @date 2023/3/6 11:28
 */
@Data
@Accessors(chain = true)
public class LicenseInfo {

    /**
     *  是否开启同城双活
     */
    private Boolean activeActiveEnable;

    /**
     * 是否开启灾备
     */
    private Boolean disasterRecoveryEnable;

}
