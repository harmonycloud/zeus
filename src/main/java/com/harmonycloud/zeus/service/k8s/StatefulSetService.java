package com.harmonycloud.zeus.service.k8s;

import io.fabric8.kubernetes.api.model.Container;

import java.util.LinkedHashMap;
import java.util.List;

public interface StatefulSetService {

    List<LinkedHashMap> get(String clusterId, String namespace, String name);
}
