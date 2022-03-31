package com.harmonycloud.zeus.service.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MysqlDbDTO;
import com.harmonycloud.caas.common.model.MysqlDbDetail;
import com.harmonycloud.caas.common.model.MysqlDbPrivilege;
import com.harmonycloud.caas.common.model.MysqlUserDetail;
import com.harmonycloud.zeus.bean.BeanMysqlDb;
import com.harmonycloud.zeus.bean.BeanMysqlDbPriv;
import com.harmonycloud.zeus.dao.BeanMysqlDbMapper;
import com.harmonycloud.zeus.service.middleware.impl.MysqlServiceImpl;
import com.harmonycloud.zeus.service.mysql.MysqlDbPrivService;
import com.harmonycloud.zeus.service.mysql.MysqlDbService;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.harmonycloud.zeus.util.MysqlConnectionUtil.getDBConnection;
import static com.harmonycloud.zeus.util.MysqlConnectionUtil.getMysqlQualifiedName;

/**
 * @author liyinlong
 * @since 2022/3/25 10:51 上午
 */
@Service
public class MysqlDbServiceImpl implements MysqlDbService {

    @Autowired
    private BeanMysqlDbMapper beanMysqlDbMapper;
    @Autowired
    private MysqlDbPrivService dbPrivService;
    @Autowired
    private MysqlServiceImpl mysqlService;

    private String initialDb = "*,information_schema,mysql,performance_schema,sys";

    @Override
    public BaseResult create(MysqlDbDTO dbDTO) {
        if (StringUtils.isAnyBlank(dbDTO.getClusterId(), dbDTO.getNamespace(), dbDTO.getMiddlewareName(), dbDTO.getDb(), dbDTO.getCharset())) {
            throw new BusinessException(ErrorMessage.MYSQL_INCOMPLETE_PARAMETERS);
        }
        if (nativeCreate(getDBConnection(mysqlService.getAccessInfo(dbDTO)), dbDTO.getDb(), dbDTO.getCharset())) {
            BeanMysqlDb mysqlDb = new BeanMysqlDb();
            mysqlDb.setDb(dbDTO.getDb());
            mysqlDb.setMysqlQualifiedName(getMysqlQualifiedName(dbDTO));
            mysqlDb.setCharset(dbDTO.getCharset());
            mysqlDb.setCreatetime(LocalDateTime.now());
            mysqlDb.setDescription(dbDTO.getDescription());
            beanMysqlDbMapper.insert(mysqlDb);
        }
        return BaseResult.error();
    }

    @Override
    public BaseResult update(MysqlDbDTO dbDTO) {
        if (StringUtils.isAnyBlank(dbDTO.getId(), dbDTO.getDescription())) {
            throw new BusinessException(ErrorMessage.MYSQL_INCOMPLETE_PARAMETERS);
        }
        BeanMysqlDb beanMysqlDb = beanMysqlDbMapper.selectById(dbDTO.getId());
        beanMysqlDb.setDescription(dbDTO.getDescription());
        beanMysqlDbMapper.updateById(beanMysqlDb);
        return BaseResult.ok();
    }

    @Override
    public BaseResult delete(String clusterId, String namespace, String middlewareName, String db) {
        if (nativeDelete(getDBConnection(mysqlService.getAccessInfo(clusterId, namespace, middlewareName)), db)) {
            QueryWrapper<BeanMysqlDb> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("db", db);
            queryWrapper.eq("mysql_qualified_name", getMysqlQualifiedName(clusterId, namespace, middlewareName));
            beanMysqlDbMapper.delete(queryWrapper);
            dbPrivService.deleteByDb(getMysqlQualifiedName(clusterId, namespace, middlewareName), db);
            return BaseResult.ok();
        }
        return BaseResult.error();
    }

    @Override
    public void delete(String clusterId, String namespace, String middlewareName) {
        QueryWrapper<BeanMysqlDb> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mysql_qualified_name", getMysqlQualifiedName(clusterId, namespace, middlewareName));
        beanMysqlDbMapper.delete(queryWrapper);
    }

    @Override
    public List<MysqlDbDTO> listCharset(String clusterId,String namespace,String middlewareName) {
        return nativeListCharset(getDBConnection(mysqlService.getAccessInfo(clusterId, namespace, middlewareName)));
    }

    @Override
    public BeanMysqlDb select(String mysqlQualifiedName, String db) {
        QueryWrapper<BeanMysqlDb> wrapper = new QueryWrapper<>();
        wrapper.eq("mysql_qualified_name", mysqlQualifiedName);
        wrapper.eq("db", db);
        return beanMysqlDbMapper.selectOne(wrapper);
    }

    @Override
    public boolean nativeCreate(Connection con, String dbName, String charset) {
        if (nativeCheckDbExists(con, dbName)) {
            throw new BusinessException(ErrorMessage.MYSQL_DB_EXISTS);
        }
        QueryRunner qr = new QueryRunner();
        String sql = String.format("create database %s DEFAULT CHARACTER SET %s", dbName, charset);
        try {
            qr.execute(con, sql, null);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(con);
        }
        return false;
    }

    @Override
    public boolean nativeDelete(Connection con, String dbName) {
        QueryRunner qr = new QueryRunner();
        String sql = String.format("drop database %s ", dbName);
        try {
            qr.execute(con, sql, null);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(con);
        }
        return false;
    }

    @Override
    public List<MysqlDbDetail> list(String clusterId, String namespace, String middlewareName,String keyword) {
        QueryRunner qr = new QueryRunner();
        String selectSchemaSql = "select SCHEMA_NAME,DEFAULT_CHARACTER_SET_NAME from information_schema.SCHEMATA ";
        if (StringUtils.isNotBlank(keyword)) {
            selectSchemaSql = "select SCHEMA_NAME,DEFAULT_CHARACTER_SET_NAME from information_schema.SCHEMATA  where SCHEMA_NAME like '%" + keyword + "%'";
        }
        Connection con = getDBConnection(mysqlService.getAccessInfo(clusterId, namespace, middlewareName));
        try {
            List<MysqlDbDetail> dbList = qr.query(con, selectSchemaSql, new BeanListHandler<>(MysqlDbDetail.class));
            // 过滤掉初始化的数据库
            dbList = dbList.stream().filter(item -> {
                if (initialDb.contains(item.getSCHEMA_NAME())) {
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
            // 查询每个数据库的使用用户、数据库备注
            dbList.forEach(item -> {
                String mysqlQualifiedName = getMysqlQualifiedName(clusterId, namespace, middlewareName);
                List<MysqlDbPrivilege> privileges = nativeListDbUser(con, item.getDb(), mysqlQualifiedName);
                item.setUsers(privileges);
                BeanMysqlDb beanMysqlDb = select(mysqlQualifiedName, item.getDb());
                if (beanMysqlDb != null) {
                    item.setDescription(beanMysqlDb.getDescription());
                    item.setId(beanMysqlDb.getId());
                    item.setCreateTime(Date.from(beanMysqlDb.getCreatetime().atZone(ZoneId.systemDefault()).toInstant()));
                }
            });
            dbList.sort(Comparator.comparing(MysqlDbDetail::getCreateTime));
            return dbList;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(con);
        }
        return null;
    }

    @Override
    public List<MysqlDbDTO> nativeListCharset(Connection con) {
        QueryRunner qr = new QueryRunner();
        String sql = "show char set";
        try {
            List<MysqlDbDTO> charsets = qr.query(con, sql, new BeanListHandler<>(MysqlDbDTO.class));
            charsets.sort(Comparator.comparing(MysqlDbDTO::getCharset));
            return charsets;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(con);
        }
        return null;
    }

    @Override
    public boolean nativeCheckDbExists(Connection con, String db) {
        QueryRunner qr = new QueryRunner();
        String selectSchemaSql = "select SCHEMA_NAME,DEFAULT_CHARACTER_SET_NAME from information_schema.SCHEMATA  where SCHEMA_NAME = '" + db + "'";
        try {
            MysqlDbDetail mysqlDbDetail = qr.query(con, selectSchemaSql, new BeanHandler<>(MysqlDbDetail.class));
            if (mysqlDbDetail != null) {
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    @Override
    public List<MysqlDbPrivilege> nativeListDbUser(Connection con, String db, String mysqlQualifiedName) {
        QueryRunner qr = new QueryRunner();
        String selectDbUser = String.format("select User from mysql.db where Db = '%s'", db);
        try {
            // 查询数据库的用户
            List<MysqlDbPrivilege> privileges = qr.query(con, selectDbUser, new BeanListHandler<>(MysqlDbPrivilege.class));
            privileges.forEach(item -> {
                BeanMysqlDbPriv dbPriv = dbPrivService.selectByUser(mysqlQualifiedName, item.getUser(), db);
                if (dbPriv != null) {
                    item.setAuthority(dbPriv.getAuthority());
                }
            });
            return privileges;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
