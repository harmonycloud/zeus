package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.zeus.service.k8s.AbstractClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Slf4j
@Service
@ConditionalOnProperty(value="system.usercenter",havingValue = "zeus")
public class ClusterServiceImpl extends AbstractClusterService {

}
