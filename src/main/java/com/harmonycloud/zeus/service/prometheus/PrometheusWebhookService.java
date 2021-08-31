package com.harmonycloud.zeus.service.prometheus;

/**
 * @author xutianhong
 * @Date 2021/5/7 5:46 下午
 */
public interface PrometheusWebhookService {

    void alert(String json) throws Exception;

}
