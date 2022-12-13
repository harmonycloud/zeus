package com.middleware.zeus.service.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.constants.MysqlConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.MysqlDbDTO;
import com.middleware.caas.common.model.MysqlDbDetail;
import com.middleware.caas.common.model.MysqlDbPrivilege;
import com.middleware.zeus.bean.BeanMysqlDb;
import com.middleware.zeus.bean.BeanMysqlDbPriv;
import com.middleware.zeus.dao.BeanMysqlDbMapper;
import com.middleware.zeus.service.middleware.impl.MysqlServiceImpl;
import com.middleware.zeus.service.mysql.MysqlDbPrivService;
import com.middleware.zeus.service.mysql.MysqlDbService;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.middleware.zeus.util.MysqlConnectionUtil.getDBConnection;
import static com.middleware.zeus.util.MysqlConnectionUtil.getMysqlQualifiedName;

/**
 * @author liyinlong
 * @since 2022/3/25 10:51 上午
 */
@Slf4j
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
    public void create(MysqlDbDTO dbDTO) {
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
    }

    @Override
    public void update(MysqlDbDTO dbDTO) {
        if (StringUtils.isEmpty(dbDTO.getId())) {
            throw new BusinessException(ErrorMessage.MYSQL_INCOMPLETE_PARAMETERS);
        }
        BeanMysqlDb beanMysqlDb = beanMysqlDbMapper.selectById(dbDTO.getId());
        beanMysqlDb.setDescription(dbDTO.getDescription());
        beanMysqlDbMapper.updateById(beanMysqlDb);
    }

    @Override
    public boolean delete(String clusterId, String namespace, String middlewareName, String db) {
        if (nativeDelete(getDBConnection(mysqlService.getAccessInfo(clusterId, namespace, middlewareName)), db)) {
            QueryWrapper<BeanMysqlDb> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("db", db);
            queryWrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, getMysqlQualifiedName(clusterId, namespace, middlewareName));
            beanMysqlDbMapper.delete(queryWrapper);
            dbPrivService.deleteByDb(getMysqlQualifiedName(clusterId, namespace, middlewareName), db);
            return true;
        }
        return false;
    }

    @Override
    public void delete(String clusterId, String namespace, String middlewareName) {
        QueryWrapper<BeanMysqlDb> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, getMysqlQualifiedName(clusterId, namespace, middlewareName));
        beanMysqlDbMapper.delete(queryWrapper);
    }

    @Override
    public List<MysqlDbDTO> listCharset(String clusterId, String namespace, String middlewareName) {
        return nativeListCharset(getDBConnection(mysqlService.getAccessInfo(clusterId, namespace, middlewareName)));
    }

    @Override
    public BeanMysqlDb select(String mysqlQualifiedName, String db) {
        QueryWrapper<BeanMysqlDb> wrapper = new QueryWrapper<>();
        wrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, mysqlQualifiedName);
        wrapper.eq("db", db);
        return beanMysqlDbMapper.selectOne(wrapper);
    }

    @Override
    public boolean nativeCreate(Connection con, String dbName, String charset) {
        if (nativeCheckDbExists(con, dbName)) {
            throw new BusinessException(ErrorMessage.MYSQL_DB_EXISTS);
        }
        QueryRunner qr = new QueryRunner();
        String sql = String.format("create database `%s` DEFAULT CHARACTER SET `%s`", dbName, charset);
        try {
            log.info(sql);
            qr.execute(con, sql);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean nativeDelete(Connection con, String dbName) {
        QueryRunner qr = new QueryRunner();
        String sql = String.format("drop database `%s` ", dbName);
        try {
            qr.execute(con, sql);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<MysqlDbDetail> list(String clusterId, String namespace, String middlewareName, String keyword) {
        QueryRunner qr = new QueryRunner();
        String selectSchemaSql = "select SCHEMA_NAME,DEFAULT_CHARACTER_SET_NAME from information_schema.SCHEMATA ";
        if (StringUtils.isNotBlank(keyword)) {
            selectSchemaSql = "select SCHEMA_NAME,DEFAULT_CHARACTER_SET_NAME from information_schema.SCHEMATA  where SCHEMA_NAME like '%" + keyword + "%'";
        }
        Connection con = getDBConnection(mysqlService.getAccessInfo(clusterId, namespace, middlewareName));
        try {
            List<MysqlDbDetail> dbList = qr.query(con, selectSchemaSql, new BeanListHandler<>(MysqlDbDetail.class));
            // 过滤掉初始化的数据库
            dbList = dbList.stream().filter(item -> !initialDb.contains(item.getSCHEMA_NAME())).collect(Collectors.toList());
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
            Collections.sort(dbList, (o1, o2) -> {
                if (o1.getCreateTime() == null) {
                    return 1;
                }
                if (o2.getCreateTime() == null) {
                    return 1;
                }
                if (o1.getCreateTime().before(o2.getCreateTime())) {
                    return 1;
                } else {
                    return -1;
                }
            });
            return dbList;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
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
        }
        return Collections.emptyList();
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
                BeanMysqlDbPriv dbPriv = dbPrivService.select(mysqlQualifiedName, item.getUser(), db);
                if (dbPriv != null) {
                    item.setAuthority(dbPriv.getAuthority());
                }
            });
            return privileges;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

}
