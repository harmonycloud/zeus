package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.zeus.integration.cluster.MysqlReplicateWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlReplicateCR;
import com.harmonycloud.zeus.service.k8s.MysqlReplicateCRDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 *
 * @author liyinlong
 * @date 2021/8/11 3:02 下午
 */
@Service
@Slf4j
public class MysqlReplicateCRDServiceImpl implements MysqlReplicateCRDService {

    @Autowired
    private MysqlReplicateWrapper mysqlReplicateWrapper;

    @Override
    public void createMysqlReplicate(String clusterId, MysqlReplicateCR mysqlReplicateCR) throws IOException {
        mysqlReplicateWrapper.create(clusterId, mysqlReplicateCR);
    }

    @Override
    public void replaceMysqlReplicate(String clusterId, MysqlReplicateCR mysqlReplicateCR) throws IOException {
        mysqlReplicateWrapper.replace(clusterId, mysqlReplicateCR);
    }

    @Override
    public void deleteMysqlReplicate(String clusterId, String namespace, String name) throws IOException {
        mysqlReplicateWrapper.delete(clusterId, namespace, name);
    }

    @Override
    public MysqlReplicateCR getMysqlReplicate(String clusterId, String namespace, String name) {
        return mysqlReplicateWrapper.getMysqlReplicate(clusterId, namespace, name);
    }
}
