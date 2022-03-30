package com.harmonycloud.zeus.service.mysql;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.MysqlDbDTO;
import com.harmonycloud.caas.common.model.MysqlDbDetail;
import com.harmonycloud.caas.common.model.MysqlDbPrivilege;
import com.harmonycloud.zeus.bean.BeanMysqlDb;

import java.sql.Connection;
import java.util.List;

/**
 * @author liyinlong
 * @since 2022/3/25 10:49 上午
 */
public interface MysqlDbService {

    /**
     * @param db
     * @description 平台层保存创建的数据库
     * @author liyinlong
     * @since 2022/3/25 2:59 下午
     */
    BaseResult create(MysqlDbDTO db);

    /**
     * @param db
     * @return
     * @description 平台层更新数据库备注
     * @author liyinlong
     * @since 2022/3/25 3:46 下午
     */
    BaseResult update(MysqlDbDTO db);

    BaseResult delete(MysqlDbDTO db);

    List<MysqlDbDetail> list(MysqlDbDTO db);

    List<MysqlDbDTO> listCharset(MysqlDbDTO db);

    BeanMysqlDb select(String mysqlQualifiedName,String db);

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
    List<MysqlDbDetail> nativeList(MysqlDbDTO db);

    /**
     * @param con
     * @return
     * @description 查询mysql所支持的字符集
     * @author liyinlong
     * @since 2022/3/25 4:02 下午
     */
    List<MysqlDbDTO> nativeListCharset(Connection con);

    /**
     * @description 查询数据库用户
     * @author  liyinlong
     * @since 2022/3/25 4:58 下午
     * @param con
     * @return
     */
    List<MysqlDbPrivilege> nativeListDbUser(Connection con,String db,String mysqlQualifiedName);
}
