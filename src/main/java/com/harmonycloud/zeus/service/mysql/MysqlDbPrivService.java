package com.harmonycloud.zeus.service.mysql;

import com.harmonycloud.zeus.bean.BeanMysqlDbPriv;

/**
 * @author liyinlong
 * @since 2022/3/25 10:49 上午
 */
public interface MysqlDbPrivService {

    BeanMysqlDbPriv selectByUser(String mysqlQualifiedName, String user,String db);

    BeanMysqlDbPriv selectByDb(String mysqlQualifiedName, String user,String db);

    void createOrUpdate(BeanMysqlDbPriv beanMysqlDbPriv);

    void deleteByUser(String mysqlQualifiedName, String user);

    void delete(String clusterId,String namespace,String middlewareName);

    void deleteByDb(String mysqlQualifiedName, String db);

}
