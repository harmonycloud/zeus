package com.harmonycloud.zeus.service.k8s.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.harmonycloud.zeus.service.k8s.NodeLabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.middleware.caas.common.model.Node;
import com.harmonycloud.zeus.service.k8s.NodeService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Slf4j
@Service
public class NodeLabelServiceImpl implements NodeLabelService {

    @Autowired
    private NodeService nodeService;

    @Override
    public List<String> list(String clusterId) {
        List<Node> list = nodeService.list(clusterId);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        Set<String> sets = new HashSet<>();
        list.forEach(no -> {
            if (!CollectionUtils.isEmpty(no.getLabels())) {
                no.getLabels().forEach((k, v) -> sets.add(k + "=" + v));
            }
        });
        return new ArrayList<>(sets);
    }
}
