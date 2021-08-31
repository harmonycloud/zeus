package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.zeus.integration.cluster.StatefulSetWrapper;
import com.harmonycloud.zeus.service.k8s.StatefulSetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @description
 * @author: liyinlong
 * @date 2021/7/7 3:48 下午
 */
@Service
@Slf4j
public class StatefulSetServiceImpl implements StatefulSetService {

    @Autowired
    private StatefulSetWrapper statefulSetWrapper;

    @Override
    public List<LinkedHashMap> get(String clusterId, String namespace, String name) {
        return statefulSetWrapper.get(clusterId, namespace, name);
    }

}
