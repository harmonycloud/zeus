package com.harmonycloud.zeus.service.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.MysqlDbDTO;
import com.harmonycloud.caas.common.model.MysqlDbDetail;
import com.harmonycloud.caas.common.model.MysqlDbPrivilege;
import com.harmonycloud.zeus.bean.BeanMysqlDb;
import com.harmonycloud.zeus.bean.BeanMysqlDbPriv;
import com.harmonycloud.zeus.dao.BeanMysqlDbMapper;
import com.harmonycloud.zeus.service.mysql.MysqlDbPrivService;
import com.harmonycloud.zeus.service.mysql.MysqlDbService;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
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

    private String initialDb = "*,information_schema,mysql,performance_schema,sys";

    @Override
    public BaseResult create(MysqlDbDTO db) {
        if (nativeCreate(getDBConnection(db), db.getDb(), db.getCharset())) {
            BeanMysqlDb mysqlDb = new BeanMysqlDb();
            mysqlDb.setDb(db.getDb());
            mysqlDb.setMysqlQualifiedName(getMysqlQualifiedName(db));
            mysqlDb.setCharset(db.getCharset());
            mysqlDb.setCreatetime(LocalDateTime.now());
            mysqlDb.setDescription(db.getDescription());
            beanMysqlDbMapper.insert(mysqlDb);
        }
        return BaseResult.error();
    }

    @Override
    public BaseResult update(MysqlDbDTO db) {
        BeanMysqlDb beanMysqlDb = beanMysqlDbMapper.selectById(db.getId());
        beanMysqlDb.setDescription(db.getDescription());
        beanMysqlDbMapper.updateById(beanMysqlDb);
        return BaseResult.ok();
    }

    @Override
    public BaseResult delete(MysqlDbDTO db) {
        if (nativeDelete(getDBConnection(db), db.getDb())) {
            beanMysqlDbMapper.deleteById(db.getId());
            //TODO 删除数据库用户关联关系
            return BaseResult.ok();
        }
        return BaseResult.error();
    }

    @Override
    public List<MysqlDbDetail> list(MysqlDbDTO db) {
        return nativeList(db);
    }


    @Override
    public List<MysqlDbDTO> listCharset(MysqlDbDTO db) {
        return nativeListCharset(getDBConnection(db));
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
    public List<MysqlDbDetail> nativeList(MysqlDbDTO db) {
        QueryRunner qr = new QueryRunner();
        String selectSchemaSql = "select SCHEMA_NAME,DEFAULT_CHARACTER_SET_NAME from information_schema.SCHEMATA ";
        Connection con = getDBConnection(db);
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
                String mysqlQualifiedName = getMysqlQualifiedName(db);
                List<MysqlDbPrivilege> privileges = nativeListDbUser(con, item.getDb(), mysqlQualifiedName);
                item.setUsers(privileges);
                BeanMysqlDb beanMysqlDb = select(mysqlQualifiedName, item.getDb());
                if (beanMysqlDb != null) {
                    item.setDescription(beanMysqlDb.getDescription());
                    item.setId(beanMysqlDb.getId());
                    item.setCreateTime(Date.from(beanMysqlDb.getCreatetime().atZone(ZoneId.systemDefault()).toInstant()));
                }
            });
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
