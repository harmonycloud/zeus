package com.harmonycloud.zeus.service.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.MysqlPrivilegeEnum;
import com.harmonycloud.caas.common.model.MysqlDbPrivilege;
import com.harmonycloud.caas.common.model.MysqlUserDTO;
import com.harmonycloud.caas.common.model.MysqlUserDetail;
import com.harmonycloud.zeus.bean.BeanMysqlDbPriv;
import com.harmonycloud.zeus.bean.BeanMysqlUser;
import com.harmonycloud.zeus.dao.BeanMysqlUserMapper;
import com.harmonycloud.zeus.service.mysql.MysqlDbPrivService;
import com.harmonycloud.zeus.service.mysql.MysqlUserService;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
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

    private String initialUser = "zabbixjk,operator,replic";

    @Override
    public BaseResult create(MysqlUserDTO user) {
        if (nativeCreate(getDBConnection(user), user.getUser(), user.getPassword())) {
            BeanMysqlUser mysqlUser = new BeanMysqlUser();
            mysqlUser.setCreatetime(LocalDateTime.now());
            mysqlUser.setUser(user.getUser());
            mysqlUser.setPassword(user.getPassword());
            mysqlUser.setDescription(user.getDescription());
            mysqlUser.setMysqlQualifiedName(getMysqlQualifiedName(user));
            beanMysqlUserMapper.insert(mysqlUser);
            // 授权数据库
            List<MysqlDbPrivilege> privilegeList = user.getPrivilegeList();
            grantUserDbPrivilege(getDBConnection(user), user.getUser(), getMysqlQualifiedName(user), privilegeList);
            return BaseResult.ok();
        }
        return BaseResult.error();
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
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("mysql_qualified_name", mysqlQualifiedName);
        wrapper.eq("user", user);
        return beanMysqlUserMapper.selectOne(wrapper);
    }

    @Override
    public BaseResult delete(MysqlUserDTO mysqlUserDTO) {
        if (nativeDelete(getDBConnection(mysqlUserDTO), mysqlUserDTO.getUser())) {
            beanMysqlUserMapper.deleteById(mysqlUserDTO.getId());
            //TODO 删除用户数据库关联关系
            return BaseResult.ok();
        }
        return BaseResult.error();
    }

    @Override
    public BaseResult grantUser(MysqlUserDTO mysqlUserDTO) {
        grantUserDbPrivilege(getDBConnection(mysqlUserDTO), mysqlUserDTO.getUser(), getMysqlQualifiedName(mysqlUserDTO), mysqlUserDTO.getPrivilegeList());
        return BaseResult.ok();
    }

    @Override
    public BaseResult updatePassword(MysqlUserDTO mysqlUserDTO) {
        if (nativeUpdatePassword(getDBConnection(mysqlUserDTO), mysqlUserDTO.getUser(), mysqlUserDTO.getPassword())) {
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
            qr.execute(con, grantPrivilegeSql, null);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean nativeCreate(Connection con, String user, String password) {
        QueryRunner qr = new QueryRunner();
        String sql = String.format("create user '%s'@'%s' identified by '%s'", user, "%", password);
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
    public boolean nativeDelete(Connection con, String user) {
        QueryRunner qr = new QueryRunner();
        String sql = String.format("drop user %s@'%s'", user, "%");
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
    public boolean nativeUpdatePassword(Connection con, String user, String newPassword) {
        QueryRunner qr = new QueryRunner();
        String sql = String.format("SET PASSWORD FOR '%s'@'%s' = PASSWORD ('%s'); ", user, "%", newPassword);
        try {
            qr.execute(con, sql, null);
            return true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    @Override
    public List<MysqlUserDetail> list(MysqlUserDTO userDTO) {
        QueryRunner qr = new QueryRunner();
        String selectSchemaSql = "select User from mysql.user where Host !='localhost'";
        //TODO 查询数据库连接信息 ip，端口，用户名，密码
        Connection con = getDBConnection(userDTO);
        try {
            List<MysqlUserDetail> userList = qr.query(con, selectSchemaSql, new BeanListHandler<>(MysqlUserDetail.class));
            //TODO 添加root用户信息
            userList = userList.stream().filter(item -> {
                if (initialUser.contains(item.getUser())) {
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
            // 查询每个用户所拥有的数据库及权限
            userList.forEach(item -> {
                String mysqlQualifiedName = getMysqlQualifiedName(userDTO);
                List<MysqlDbPrivilege> privileges = nativeListDbUser(con, item.getUser(), mysqlQualifiedName);
                // 查询平台存储的用户信息
                BeanMysqlUser beanMysqlUser = select(mysqlQualifiedName, item.getUser());
                if (beanMysqlUser != null) {
                    item.setId(beanMysqlUser.getId());
                    item.setDescription(beanMysqlUser.getDescription());
                    item.setPassword(beanMysqlUser.getPassword());
                    item.setCreateTime(Date.from(beanMysqlUser.getCreatetime().atZone(ZoneId.systemDefault()).toInstant()));
                    item.setPasswordCheck(passwordCheck(userDTO, item.getUser(), item.getPassword()));
                }
                item.setDbs(privileges);
            });
            return userList;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            DbUtils.closeQuietly(con);
        }
        return null;
    }

    @Override
    public List<MysqlDbPrivilege> nativeListDbUser(Connection con, String user, String mysqlQualifiedName) {
        QueryRunner qr = new QueryRunner();
        String selectUserDb = String.format("select Db from mysql.db where User = '%s'", user);
        // 查询用户拥有的数据库
        try {
            List<MysqlDbPrivilege> privileges = qr.query(con, selectUserDb, new BeanListHandler<>(MysqlDbPrivilege.class));
            privileges.forEach(item -> {
                BeanMysqlDbPriv dbPriv = dbPrivService.selectByUser(mysqlQualifiedName, user, item.getDb());
                if (dbPriv != null) {
                    item.setAuthority(dbPriv.getAuthority());
                }
            });
            return privileges;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }


}
