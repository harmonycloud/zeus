package com.harmonycloud.zeus.service.middleware;

/**
 * @author xutianhong
 * @Date 2022/6/9 2:12 下午
 */
public interface MiddlewareCrTypeService {

    /**
     * 添加中间件cr类型映射
     *
     * @param type 中间件类型
     * @param crType 中间件cr类型
     */
    void put(String type, String crType);

    /**
     * 获取中间件cr类型
     *
     * @param type 中间件类型
     * @return String
     */
    String findByType(String type);

    /**
     * 根据cr类型获取中间件类型
     *
     * @param crType 中间件cr类型
     * @return String
     */
    String findTypeByCrType(String crType);

    /**
     * 是否属于中间件类型
     *
     * @param type 中间件类型
     * @return Boolean
     */
    Boolean isType(String type);

    /**
     * 是否属于中间件cr类型
     *
     * @param crType 中间件cr类型
     * @return Boolean
     */
    Boolean isCrType(String crType);

    /**
     * 通过chart包解析出crType
     *
     * @param chartName 中间件类型
     * @param chartVersion 中间件版本
     * @return String
     */
    String getCrTypeByChart(String chartName, String chartVersion);

}
