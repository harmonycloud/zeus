package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.Secret;

import java.util.List;

/**
 * @author xutianhong
 * @since 2021/6/23 10:55 上午
 */
public interface SecretService {

    List<Secret> list(String clusterId, String namespace);

}
