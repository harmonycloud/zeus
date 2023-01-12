package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.zeus.bean.BeanProjectBackupServer;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/11 3:02 下午
 */
public interface ProjectBackupServerService {

    List<BeanProjectBackupServer> listByBackupServerId(Integer serverId);

}
