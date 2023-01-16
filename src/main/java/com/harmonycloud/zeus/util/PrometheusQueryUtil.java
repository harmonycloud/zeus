package com.harmonycloud.zeus.util;

/**
 * @author xutianhong
 * @Date 2023/1/16 1:53 下午
 */
public class PrometheusQueryUtil {

    public static String queryHitachiFree(String storageClass, String serialId, String poolId){
        return String.format("sum(spc_sc_pool_free_capacity{storageclass=\"%s\",serial_id=\"%s\",pool_id=\"%s\"})", storageClass, serialId, poolId);
    }

}
