package com.harmonycloud.zeus.service.middleware.impl;

import com.harmonycloud.caas.common.model.middleware.MiddlewareBackup;
import com.harmonycloud.zeus.service.middleware.AbstractMiddlewareService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/24
 */
@Slf4j
@Service
public class MiddlewareBackupServiceImpl extends AbstractMiddlewareService implements MiddlewareBackupService {


    @Override
    public List<MiddlewareBackup> list(String clusterId, String namespace, String type, String middlewareName) {
        return null;
    }

    @Override
    public void create(String clusterId, String namespace, String type, String middlewareName) {
    }
}
