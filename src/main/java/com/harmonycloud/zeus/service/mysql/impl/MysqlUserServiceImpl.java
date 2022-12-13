package com.harmonycloud.zeus.service.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.constants.MysqlConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.MysqlPrivilegeEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.MysqlAccessInfo;
import com.middleware.caas.common.model.MysqlDbPrivilege;
import com.middleware.caas.common.model.MysqlUserDTO;
import com.middleware.caas.common.model.MysqlUserDetail;
import com.harmonycloud.zeus.bean.BeanMysqlDbPriv;
import com.harmonycloud.zeus.bean.BeanMysqlUser;
import com.harmonycloud.zeus.dao.BeanMysqlUserMapper;
import com.harmonycloud.zeus.operator.api.MysqlOperator;
import com.harmonycloud.zeus.service.middleware.impl.MysqlServiceImpl;
import com.harmonycloud.zeus.service.mysql.MysqlDbPrivService;
import com.harmonycloud.zeus.service.mysql.MysqlUserService;
import com.harmonycloud.zeus.util.MyAESUtil;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.ROOT;
import static com.harmonycloud.zeus.util.MysqlConnectionUtil.*;

/**
 * @author liyinlong
 * @since 2022/3/28 10:09 上午
 */
@Slf4j
@Service
public class MysqlUserServiceImpl implements MysqlUserService {

    @Autowired
    private BeanMysqlUserMapper beanMysqlUserMapper;
    @Autowired
    private MysqlDbPrivService dbPrivService;
    @Autowired
    private MysqlServiceImpl mysqlService;
    @Autowired
    private MysqlOperator mysqlOperator;

    private static final String INITIAL_USER = "zabbixjk,operator,replic";

    @Override
    public boolean create(MysqlUserDTO user) {
        if (StringUtils.isAnyBlank(user.getClusterId(), user.getNamespace(), user.getMiddlewareName(), user.getUser(), user.getPassword(), user.getConfirmPassword())) {
            throw new BusinessException(ErrorMessage.MYSQL_INCOMPLETE_PARAMETERS);
        }
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            throw new BusinessException(ErrorMessage.MYSQL_PASSWORD_NOT_MATCH);
        }
        Connection con = getDBConnection(mysqlService.getAccessInfo(user));
        if (nativeCreate(con, user.getUser(), user.getPassword())) {
            BeanMysqlUser mysqlUser = new BeanMysqlUser();
            mysqlUser.setCreatetime(LocalDateTime.now());
            mysqlUser.setUser(user.getUser());
            mysqlUser.setPassword(user.getPassword());
            mysqlUser.setDescription(user.getDescription());
            mysqlUser.setMysqlQualifiedName(getMysqlQualifiedName(user));
            create(mysqlUser);
            // 授权数据库
            List<MysqlDbPrivilege> privilegeList = user.getPrivilegeList();
            grantUserDbPrivilege(con, user.getUser(), getMysqlQualifiedName(user), privilegeList);
            return true;
        }
        return false;
    }

    @Override
    public void create(BeanMysqlUser beanMysqlUser) {
        beanMysqlUser.setPassword(MyAESUtil.encodeBase64(beanMysqlUser.getPassword()));
        beanMysqlUserMapper.insert(beanMysqlUser);
    }

    @Override
    public void update(MysqlUserDTO mysqlUserDTO) {
        BeanMysqlUser mysqlUser = beanMysqlUserMapper.selectById(mysqlUserDTO.getId());
        mysqlUser.setDescription(mysqlUserDTO.getDescription());
        beanMysqlUserMapper.updateById(mysqlUser);
    }

    @Override
    public BeanMysqlUser select(String mysqlQualifiedName, String user) {
        QueryWrapper<BeanMysqlUser> wrapper = new QueryWrapper<>();
        wrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, mysqlQualifiedName);
        wrapper.eq("user", user);
        return beanMysqlUserMapper.selectOne(wrapper);
    }

    @Override
    public boolean delete(String clusterId, String namespace, String middlewareName, String user) {
        if (nativeDelete(getDBConnection(mysqlService.getAccessInfo(clusterId, namespace, middlewareName)), user)) {
            BeanMysqlUser mysqlUser = select(getMysqlQualifiedName(clusterId, namespace, middlewareName), user);
            beanMysqlUserMapper.deleteById(mysqlUser);
            dbPrivService.deleteByUser(getMysqlQualifiedName(clusterId, namespace, middlewareName), user);
            return true;
        }
        return false;
    }

    @Override
    public void delete(String clusterId, String namespace, String middlewareName) {
        QueryWrapper<BeanMysqlUser> wrapper = new QueryWrapper<>();
        wrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, getMysqlQualifiedName(clusterId, namespace, middlewareName));
        beanMysqlUserMapper.delete(wrapper);
    }

    @Override
    public void grantUser(MysqlUserDTO mysqlUserDTO) {
        if (StringUtils.isAnyBlank(mysqlUserDTO.getId())) {
            throw new BusinessException(ErrorMessage.MYSQL_INCOMPLETE_PARAMETERS);
        }
        BeanMysqlUser beanMysqlUser = beanMysqlUserMapper.selectById(mysqlUserDTO.getId());
        if (StringUtils.isNotBlank(mysqlUserDTO.getDescription())) {
            beanMysqlUser.setDescription(mysqlUserDTO.getDescription());
            beanMysqlUserMapper.updateById(beanMysqlUser);
        }
        grantUserDbPrivilege(getDBConnection(mysqlService.getAccessInfo(mysqlUserDTO)), mysqlUserDTO.getUser(), getMysqlQualifiedName(mysqlUserDTO), mysqlUserDTO.getPrivilegeList());
    }

    @Override
    public void updatePassword(MysqlUserDTO mysqlUserDTO) {
        if (StringUtils.isAnyEmpty(mysqlUserDTO.getUser(), mysqlUserDTO.getPassword(),
            mysqlUserDTO.getConfirmPassword())) {
            throw new BusinessException(ErrorMessage.MYSQL_PASSWORD_NOT_MATCH);
        }
        if (nativeUpdatePassword(getDBConnection(mysqlService.getAccessInfo(mysqlUserDTO)), mysqlUserDTO.getUser(),
            mysqlUserDTO.getPassword())) {
            // 更新数据库密码
            BeanMysqlUser mysqlUser = beanMysqlUserMapper.selectById(mysqlUserDTO.getId());
            if (mysqlUser == null) {
                String name = getMysqlQualifiedName(mysqlUserDTO.getClusterId(), mysqlUserDTO.getNamespace(),
                    mysqlUserDTO.getMiddlewareName());
                initMysqlUserInfo(name, mysqlUserDTO.getPassword(), mysqlUserDTO.getUser());
            } else {
                mysqlUser.setPassword(MyAESUtil.encodeBase64(mysqlUserDTO.getPassword()));
                beanMysqlUserMapper.updateById(mysqlUser);
            }
        } else {
            throw new BusinessException(ErrorMessage.MYSQL_UPDATE_PASSWORD_FAILED);
        }
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
            String privilege = MysqlPrivilegeEnum.findDbPrivilege(authority);
            String grantPrivilegeSql = String.format(" grant %s on `%s`.* to '%s' ", privilege, db, user);
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
            if (!nativeCheckUserExists(con, user)) {
                return true;
            }
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
        }
        return false;
    }

    @Override
    public boolean nativeDelete(Connection con, String user) {
        QueryRunner qr = new QueryRunner();
        String sql = String.format("drop user `%s`@`%s`", user, "%");
        try {
            qr.execute(con, sql);
            return true;
        } catch (SQLException e) {
            log.error("删除数据库用户出错了", e);
        }
        return false;
    }

    @Override
    public boolean nativeUpdatePassword(Connection con, String user, String newPassword) {
        QueryRunner qr = new QueryRunner();
        String sql = String.format("ALTER USER '%s'@'%s' IDENTIFIED WITH MYSQL_NATIVE_PASSWORD BY '%s';",user, "%", newPassword);
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
        MysqlAccessInfo accessInfo = mysqlService.getAccessInfo(clusterId, namespace, middlewareName);
        Connection con = getDBConnection(accessInfo);
        try {
            List<MysqlUserDetail> userList = qr.query(con, generateDbQuerySql(user, keyword), new BeanListHandler<>(MysqlUserDetail.class));
            userList = userList.stream().filter(item -> !INITIAL_USER.contains(item.getUser())).collect(Collectors.toList());
            // 查询每个用户所拥有的数据库及权限
            userList.forEach(userDetail -> {
                String mysqlQualifiedName = getMysqlQualifiedName(clusterId, namespace, middlewareName);
                List<MysqlDbPrivilege> privileges =
                    nativeListUserDb(con, userDetail.getUser(), mysqlQualifiedName, keyword);
                userDetail.setDbs(privileges);
                // 查询平台存储的用户信息
                BeanMysqlUser beanMysqlUser = select(mysqlQualifiedName, userDetail.getUser());
                if (beanMysqlUser == null && ROOT.equals(userDetail.getUser())) {
                    beanMysqlUser = initMysqlUserInfo(mysqlQualifiedName, accessInfo.getPassword(), ROOT);
                }
                if (beanMysqlUser != null) {
                    userDetail.setId(beanMysqlUser.getId());
                    userDetail.setPassword(MyAESUtil.decodeBase64(beanMysqlUser.getPassword()));
                    userDetail
                        .setPasswordCheck(passwordCheck(accessInfo, userDetail.getUser(), userDetail.getPassword()));
                    userDetail.setDescription(beanMysqlUser.getDescription());
                    userDetail.setCreateTime(
                        Date.from(beanMysqlUser.getCreatetime().atZone(ZoneId.systemDefault()).toInstant()));
                }
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

    public BeanMysqlUser initMysqlUserInfo(String mysqlQualifiedName, String password, String user) {
        BeanMysqlUser beanMysqlUser = new BeanMysqlUser();
        beanMysqlUser.setMysqlQualifiedName(mysqlQualifiedName);
        beanMysqlUser.setUser(user);
        beanMysqlUser.setPassword(MyAESUtil.encodeBase64(password));
        beanMysqlUser.setCreatetime(LocalDateTime.now());
        beanMysqlUserMapper.insert(beanMysqlUser);
        return select(mysqlQualifiedName, user);
    }

    // 校验对外服务是否存在，若不存在，则创建
    public void checkAndCreateOpenService(String clusterId,String namespace,String middlewareName, MysqlAccessInfo mysqlAccessInfo){
        if(!mysqlAccessInfo.isOpenService()){


        }
    }

}
