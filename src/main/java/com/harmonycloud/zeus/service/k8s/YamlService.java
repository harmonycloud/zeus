package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.YamlCheck;

/**
 * @author xutianhong
 * @Date 2021/12/23 3:24 下午
 */
public interface YamlService {

    YamlCheck check(String yamlContent);

}
