package com.harmonycloud.zeus.service.system;

/**
 * @author xutianhong
 * @Date 2022/10/27 11:43 上午
 */
public interface LicenseService {

    /**
     * 解析license
     *
     * @param license
     */
    void license(String license);

    /**
     * 平台可用额度校验
     *
     * @return Boolean
     */
    Boolean check();

}
