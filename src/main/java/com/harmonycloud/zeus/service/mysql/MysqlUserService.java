package com.harmonycloud.zeus.service.mysql;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.MysqlDbPrivilege;
import com.harmonycloud.caas.common.model.MysqlUserDTO;
import com.harmonycloud.caas.common.model.MysqlUserDetail;
import com.harmonycloud.zeus.bean.BeanMysqlUser;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

/**
 * @author liyinlong
 * @since 2022/3/25 10:48 上午
 */
@Service
public interface MysqlUserService {

    BaseResult create(MysqlUserDTO mysqlUserDTO);

    void create(BeanMysqlUser beanMysqlUser);

    BaseResult update(MysqlUserDTO mysqlUserDTO);

    BeanMysqlUser select(String mysqlQualifiedName, String user);

    BaseResult delete(String clusterId, String namespace, String middlewareName, String user);

    void delete(String clusterId, String namespace, String middlewareName);

    BaseResult grantUser(MysqlUserDTO mysqlUserDTO);

    void updatePassword(MysqlUserDTO mysqlUserDTO);

    void grantUserDbPrivilege(Connection con, String user, String mysqlQualifiedName, List<MysqlDbPrivilege> privileges);

    boolean nativeGrantUser(Connection con, String user, int authority, String db);

    boolean nativeRevokeUser(Connection con, String user);

    boolean nativeCreate(Connection con, String user, String password);

    boolean nativeDelete(Connection con, String user);

    boolean nativeUpdatePassword(Connection con, String user, String newPassword);

    boolean nativeCheckUserExists(Connection con, String user);

    List<MysqlUserDetail> list(String clusterId, String namespace, String middlewareName,String user, String keyword);

    List<MysqlDbPrivilege> nativeListUserDb(Connection con, String user, String mysqlQualifiedName, String keyword);

}
