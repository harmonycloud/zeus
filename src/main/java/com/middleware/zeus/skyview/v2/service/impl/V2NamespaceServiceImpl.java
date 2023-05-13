package com.middleware.zeus.skyview.v2.service.impl;

import com.middleware.zeus.common.base.CaasResult;
import com.middleware.zeus.common.model.middleware.Namespace;
import com.middleware.zeus.skyview.v2.service.V2NamespaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/26 6:35 下午
 */
@Slf4j
@Service
public class V2NamespaceServiceImpl implements V2NamespaceService {


    @Override
    public List<Namespace> list(String clusterId, Boolean detail) {
        return null;
    }
}
