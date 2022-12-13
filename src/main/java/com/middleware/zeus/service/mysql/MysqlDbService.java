package com.middleware.zeus.service.mysql;

import com.middleware.caas.common.model.MysqlDbDTO;
import com.middleware.caas.common.model.MysqlDbDetail;
import com.middleware.caas.common.model.MysqlDbPrivilege;
import com.middleware.zeus.bean.BeanMysqlDb;

import java.sql.Connection;
import java.util.List;

/**
 * @author liyinlong
 * @since 2022/3/25 10:49 上午
 */
public interface MysqlDbService {

    /**
     * @param dbDTO
     * @description 平台层保存创建的数据库
     * @author liyinlong
     * @since 2022/3/25 2:59 下午
     */
    void create(MysqlDbDTO dbDTO);

    /**
     * @param dbDTO
     * @return
     * @description 平台层更新数据库备注
     * @author liyinlong
     * @since 2022/3/25 3:46 下午
     */
    void update(MysqlDbDTO dbDTO);

    boolean delete(String clusterId, String namespace, String middlewareName, String db);

    void delete(String clusterId, String namespace, String middlewareName);

    List<MysqlDbDTO> listCharset(String clusterId,String namespace,String middlewareName);

    BeanMysqlDb select(String mysqlQualifiedName, String db);

    /**
     * 创建数据库
     *
     * @param dbName
     * @param charset
     * @return
     */
    boolean nativeCreate(Connection con, String dbName, String charset);

    /**
     * @param dbName
     * @return
     * @description 删除数据库
     * @author liyinlong
     * @since 2022/3/25 3:00 下午
     */
    boolean nativeDelete(Connection con, String dbName);

    /**
     * @return
     * @description 查询全部数据库
     * @author liyinlong
     * @since 2022/3/25 3:00 下午
     */
    List<MysqlDbDetail> list(String clusterId, String namespace, String middlewareName,String keyword);

    /**
     * @param con
     * @return
     * @description 查询mysql所支持的字符集
     * @author liyinlong
     * @since 2022/3/25 4:02 下午
     */
    List<MysqlDbDTO> nativeListCharset(Connection con);

    boolean nativeCheckDbExists(Connection con, String db);

    /**
     * @param con
     * @return
     * @description 查询数据库用户
     * @author liyinlong
     * @since 2022/3/25 4:58 下午
     */
    List<MysqlDbPrivilege> nativeListDbUser(Connection con, String db, String mysqlQualifiedName);
}
