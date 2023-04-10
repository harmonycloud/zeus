package com.middleware.zeus.service.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.constants.MysqlConstant;
import com.middleware.zeus.bean.BeanMysqlDbPriv;
import com.middleware.zeus.dao.BeanMysqlDbPrivMapper;
import com.middleware.zeus.service.mysql.MysqlDbPrivService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.middleware.zeus.util.MysqlConnectionUtil.getMysqlQualifiedName;

/**
 * @author liyinlong
 * @since 2022/3/25 10:50 上午
 */
@Service
public class MysqlDbPrivServiceImpl implements MysqlDbPrivService {

    @Autowired
    private BeanMysqlDbPrivMapper mysqlDbPrivMapper;

    @Override
    public BeanMysqlDbPriv select(String mysqlQualifiedName, String user, String db) {
        QueryWrapper<BeanMysqlDbPriv> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, mysqlQualifiedName);
        queryWrapper.eq("user", user);
        queryWrapper.eq("db", db);
        return mysqlDbPrivMapper.selectOne(queryWrapper);
    }

    @Override
    public void createOrUpdate(BeanMysqlDbPriv beanMysqlDbPriv) {
        QueryWrapper<BeanMysqlDbPriv> wrapper = new QueryWrapper<>();
        wrapper.eq("db", beanMysqlDbPriv.getDb());
        wrapper.eq("user", beanMysqlDbPriv.getUser());
        wrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, beanMysqlDbPriv.getMysqlQualifiedName());
        BeanMysqlDbPriv dbPriv = mysqlDbPrivMapper.selectOne(wrapper);
        if (dbPriv == null) {
            mysqlDbPrivMapper.insert(beanMysqlDbPriv);
        } else {
            dbPriv.setAuthority(beanMysqlDbPriv.getAuthority());
            mysqlDbPrivMapper.updateById(dbPriv);
        }
    }

    @Override
    public void deleteByUser(String mysqlQualifiedName, String user) {
        QueryWrapper<BeanMysqlDbPriv> wrapper = new QueryWrapper<>();
        wrapper.eq("user", user);
        wrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, mysqlQualifiedName);
        mysqlDbPrivMapper.delete(wrapper);
    }

    @Override
    public void delete(String clusterId, String namespace, String middlewareName) {
        QueryWrapper<BeanMysqlDbPriv> wrapper = new QueryWrapper<>();
        wrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, getMysqlQualifiedName(clusterId, namespace, middlewareName));
        mysqlDbPrivMapper.delete(wrapper);

    }

    @Override
    public void deleteByDb(String mysqlQualifiedName, String db) {
        QueryWrapper<BeanMysqlDbPriv> wrapper = new QueryWrapper<>();
        wrapper.eq("db", db);
        wrapper.eq(MysqlConstant.MYSQL_QUALIFIED_NAME, mysqlQualifiedName);
        mysqlDbPrivMapper.delete(wrapper);
    }

}
