package com.harmonycloud.zeus.service.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.constants.MysqlConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.MysqlPrivilegeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MysqlAccessInfo;
import com.harmonycloud.caas.common.model.MysqlDbPrivilege;
import com.harmonycloud.caas.common.model.MysqlUserDTO;
import com.harmonycloud.caas.common.model.MysqlUserDetail;
import com.harmonycloud.zeus.bean.BeanMysqlDbPriv;
import com.harmonycloud.zeus.bean.BeanMysqlUser;
import com.harmonycloud.zeus.dao.BeanMysqlUserMapper;
import com.harmonycloud.zeus.service.middleware.impl.MysqlServiceImpl;
import com.harmonycloud.zeus.service.mysql.MysqlDbPrivService;
import com.harmonycloud.zeus.service.mysql.MysqlUserService;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.harmonycloud.zeus.util.MysqlConnectionUtil.getDBConnection;
import static com.harmonycloud.zeus.util.MysqlConnectionUtil.getMysqlQualifiedName;
import static com.harmonycloud.zeus.util.MysqlConnectionUtil.passwordCheck;

/**
 * @author liyinlong
 * @since 2022/3/28 10:09 上午
 */
@Service
public class MysqlUserServiceImpl implements MysqlUserService {

    @Autowired
    private BeanMysqlUserMapper beanMysqlUserMapper;
    @Autowired
    private MysqlDbPrivService dbPrivService;
    @Autowired
    private MysqlServiceImpl mysqlService;

    private static final String INITIAL_USER = "zabbixjk,operator,replic";

    @Override
    public BaseResult create(MysqlUserDTO user) {
        if (StringUtils.isAnyBlank(user.getClusterId(), user.getNamespace(), user.getMiddlewareName(), user.getUser(), user.getPassword())) {
            throw new BusinessException(ErrorMessage.MYSQL_INCOMPLETE_PARAMETERS);
        }
        Connection con = getDBConnection(mysqlService.getAccessInfo(user));
        if (nativeCreate(con, user.getUser(), user.getPassword())) {
            BeanMysqlUser mysqlUser = new BeanMysqlUser();
            mysqlUser.setCreatetime(LocalDateTime.now());
            mysqlUser.setUser(user.getUser());
            mysqlUser.setPassword(user.getPassword());
            mysqlUser.setDescription(user.getDescription());
            mysqlUser.setMysqlQualifiedName(getMysqlQualifiedName(user));
            beanMysqlUserMapper.insert(mysqlUser);
            // 授权数据库
            List<MysqlDbPrivilege> privilegeList = user.getPrivilegeList();
            grantUserDbPrivilege(con, user.getUser(), getMysqlQualifiedName(user), privilegeList);
            return BaseResult.ok();
        }
        return BaseResult.error();
    }

    @Override
    public void create(BeanMysqlUser beanMysqlUser) {
        beanMysqlUserMapper.insert(beanMysqlUser);
    }

    @Override
    public BaseResult update(MysqlUserDTO mysqlUserDTO) {
        BeanMysqlUser mysqlUser = beanMysqlUserMapper.selectById(mysqlUserDTO.getId());
        mysqlUser.setDescription(mysqlUserDTO.getDescription());
        beanMysqlUserMapper.updateById(mysqlUser);
        return BaseResult.ok();
    }

    @Override
    public BeanMysqlUser select(String mysqlQualifiedName, String user) {
        QueryWrapper<BeanMysqlUser> wrapper = new QueryWrapper<>();
        wrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, mysqlQualifiedName);
        wrapper.eq("user", user);
        return beanMysqlUserMapper.selectOne(wrapper);
    }

    @Override
    public BaseResult delete(String clusterId, String namespace, String middlewareName, String user) {
        if (nativeDelete(getDBConnection(mysqlService.getAccessInfo(clusterId, namespace, middlewareName)), user)) {
            BeanMysqlUser mysqlUser = select(getMysqlQualifiedName(clusterId, namespace, middlewareName), user);
            beanMysqlUserMapper.deleteById(mysqlUser);
            dbPrivService.deleteByUser(getMysqlQualifiedName(clusterId, namespace, middlewareName), user);
            return BaseResult.ok();
        }
        return BaseResult.error();
    }

    @Override
    public void delete(String clusterId, String namespace, String middlewareName) {
        QueryWrapper<BeanMysqlUser> wrapper = new QueryWrapper<>();
        wrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, getMysqlQualifiedName(clusterId, namespace, middlewareName));
        beanMysqlUserMapper.delete(wrapper);
    }

    @Override
    public BaseResult grantUser(MysqlUserDTO mysqlUserDTO) {
        if (StringUtils.isAnyBlank(mysqlUserDTO.getId())) {
            throw new BusinessException(ErrorMessage.MYSQL_INCOMPLETE_PARAMETERS);
        }
        BeanMysqlUser beanMysqlUser = beanMysqlUserMapper.selectById(mysqlUserDTO.getId());
        if (StringUtils.isNotBlank(mysqlUserDTO.getDescription())) {
            beanMysqlUser.setDescription(mysqlUserDTO.getDescription());
            beanMysqlUserMapper.updateById(beanMysqlUser);
        }
        grantUserDbPrivilege(getDBConnection(mysqlService.getAccessInfo(mysqlUserDTO)), mysqlUserDTO.getUser(), getMysqlQualifiedName(mysqlUserDTO), mysqlUserDTO.getPrivilegeList());
        return BaseResult.ok();
    }

    @Override
    public BaseResult updatePassword(MysqlUserDTO mysqlUserDTO) {
        if (nativeUpdatePassword(getDBConnection(mysqlService.getAccessInfo(mysqlUserDTO)), mysqlUserDTO.getUser(), mysqlUserDTO.getPassword())) {
            // 更新数据库密码
            BeanMysqlUser mysqlUser = beanMysqlUserMapper.selectById(mysqlUserDTO.getId());
            mysqlUser.setPassword(mysqlUserDTO.getPassword());
            beanMysqlUserMapper.updateById(mysqlUser);
            return BaseResult.ok();
        }
        return BaseResult.error();
    }

    @Override
    public void grantUserDbPrivilege(Connection con, String user, String mysqlQualifiedName, List<MysqlDbPrivilege> privileges) {
        if (CollectionUtils.isEmpty(privileges)) {
            return;
        }
        // 先取消该用户所有数据库授权
        if (!nativeRevokeUser(con, user)) {
            return;
        }
        // 删除平台该用户所有授权记录
        dbPrivService.deleteByUser(mysqlQualifiedName, user);
        // 重新授权数据库权限
        privileges.forEach(item -> {
            if (nativeGrantUser(con, user, item.getAuthority(), item.getDb())) {
                BeanMysqlDbPriv dbPriv = new BeanMysqlDbPriv();
                dbPriv.setUser(user);
                dbPriv.setAuthority(item.getAuthority());
                dbPriv.setDb(item.getDb());
                dbPriv.setMysqlQualifiedName(mysqlQualifiedName);
                dbPrivService.createOrUpdate(dbPriv);
            }
        });
    }

    @Override
    public boolean nativeGrantUser(Connection con, String user, int authority, String db) {
        QueryRunner qr = new QueryRunner();
        try {
            String privilege = MysqlPrivilegeEnum.findPrivilege(authority).getPrivilege();
            String grantPrivilegeSql = String.format(" grant %s on %s.* to '%s' ", privilege, db, user);
            qr.execute(con, grantPrivilegeSql);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean nativeRevokeUser(Connection con, String user) {
        QueryRunner qr = new QueryRunner();
        try {
            String grantPrivilegeSql = String.format("REVOKE ALL PRIVILEGES, GRANT OPTION FROM '%s' ", user);
            qr.execute(con, grantPrivilegeSql);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean nativeCreate(Connection con, String user, String password) {
        QueryRunner qr = new QueryRunner();
        if (nativeCheckUserExists(con, user)) {
            throw new BusinessException(ErrorMessage.MYSQL_USER_EXISTS);
        }
        String sql = String.format("create user '%s'@'%s' identified by '%s'", user, "%", password);
        try {
            qr.execute(con, sql);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(con);
        }
        return false;
    }

    @Override
    public boolean nativeDelete(Connection con, String user) {
        QueryRunner qr = new QueryRunner();
        String sql = String.format("drop user %s@'%s'", user, "%");
        try {
            qr.execute(con, sql);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(con);
        }
        return false;
    }

    @Override
    public boolean nativeUpdatePassword(Connection con, String user, String newPassword) {
        QueryRunner qr = new QueryRunner();
        String sql = String.format("SET PASSWORD FOR '%s'@'%s' = PASSWORD ('%s'); ", user, "%", newPassword);
        try {
            qr.execute(con, sql);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean nativeCheckUserExists(Connection con, String user) {
        QueryRunner qr = new QueryRunner();
        String selectSchemaSql = "select User from mysql.user where Host !='localhost' and User = '" + user + "'";
        try {
            MysqlUserDetail mysqlUserDetail = qr.query(con, selectSchemaSql, new BeanHandler<>(MysqlUserDetail.class));
            if (mysqlUserDetail != null) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<MysqlUserDetail> list(String clusterId, String namespace, String middlewareName, String user, String keyword) {
        QueryRunner qr = new QueryRunner();
        Connection con = getDBConnection(mysqlService.getAccessInfo(clusterId, namespace, middlewareName));
        try {
            List<MysqlUserDetail> userList = qr.query(con, generateDbQuerySql(user, keyword), new BeanListHandler<>(MysqlUserDetail.class));
            userList = userList.stream().filter(item -> !INITIAL_USER.contains(item.getUser())).collect(Collectors.toList());
            // 查询每个用户所拥有的数据库及权限
            MysqlAccessInfo accessInfo = mysqlService.getAccessInfo(clusterId, namespace, middlewareName);
            userList.forEach(item -> {
                String mysqlQualifiedName = getMysqlQualifiedName(clusterId, namespace, middlewareName);
                List<MysqlDbPrivilege> privileges = nativeListUserDb(con, item.getUser(), mysqlQualifiedName, keyword);
                // 查询平台存储的用户信息
                BeanMysqlUser beanMysqlUser = select(mysqlQualifiedName, item.getUser());
                if (beanMysqlUser != null) {
                    item.setId(beanMysqlUser.getId());
                    item.setDescription(beanMysqlUser.getDescription());
                    item.setPassword(beanMysqlUser.getPassword());
                    item.setCreateTime(Date.from(beanMysqlUser.getCreatetime().atZone(ZoneId.systemDefault()).toInstant()));
                    item.setPasswordCheck(passwordCheck(accessInfo, item.getUser(), item.getPassword()));
                }
                item.setDbs(privileges);
            });
            userList.sort((o1, o2) -> {
                if (o1.getCreateTime() == null) {
                    return 0;
                }
                if (o2.getCreateTime() == null) {
                    return 0;
                }
                if (o1.getCreateTime().before(o2.getCreateTime())) {
                    return 1;
                } else {
                    return -1;
                }
            });
            return userList;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(con);
        }
        return Collections.emptyList();
    }

    @Override
    public List<MysqlDbPrivilege> nativeListUserDb(Connection con, String user, String mysqlQualifiedName, String keyword) {
        QueryRunner qr = new QueryRunner();
        String selectUserDb = String.format("select Db from mysql.db where User = '%s'", user);
        if (StringUtils.isNotBlank(keyword)) {
            selectUserDb = String.format("select Db from mysql.db where User = '%s' and Db like '%s'", user, "%" + keyword + "%");
        }
        // 查询用户拥有的数据库
        try {
            List<MysqlDbPrivilege> privileges = qr.query(con, selectUserDb, new BeanListHandler<>(MysqlDbPrivilege.class));
            privileges.forEach(item -> {
                BeanMysqlDbPriv dbPriv = dbPrivService.select(mysqlQualifiedName, user, item.getDb());
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

    public String generateDbQuerySql(String user, String keyword) {
        String selectSchemaSql = "select User from mysql.user where Host !='localhost'";
        if (StringUtils.isNotBlank(user)) {
            selectSchemaSql = "select User from mysql.user where Host !='localhost' and User = '" + user + "'";
            return selectSchemaSql;
        }
        if (StringUtils.isNotBlank(keyword)) {
            selectSchemaSql = "select User from mysql.user where Host !='localhost' and User like '%" + keyword + "%'";
        }
        return selectSchemaSql;
    }

}
