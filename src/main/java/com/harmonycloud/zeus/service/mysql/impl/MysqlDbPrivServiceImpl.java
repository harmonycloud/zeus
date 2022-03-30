package com.harmonycloud.zeus.service.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.zeus.bean.BeanMysqlDbPriv;
import com.harmonycloud.zeus.dao.BeanMysqlDbPrivMapper;
import com.harmonycloud.zeus.service.mysql.MysqlDbPrivService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/3/25 10:50 上午
 */
@Service
public class MysqlDbPrivServiceImpl implements MysqlDbPrivService {

    @Autowired
    private BeanMysqlDbPrivMapper mysqlDbPrivMapper;

    @Override
    public BeanMysqlDbPriv selectByUser(String mysqlQualifiedName, String user, String db) {
        QueryWrapper<BeanMysqlDbPriv> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mysql_qualified_name", mysqlQualifiedName);
        queryWrapper.eq("user", user);
        queryWrapper.eq("db", db);
        return mysqlDbPrivMapper.selectOne(queryWrapper);
    }

    @Override
    public BeanMysqlDbPriv selectByDb(String mysqlQualifiedName, String user, String db) {
        QueryWrapper<BeanMysqlDbPriv> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mysql_qualified_name", mysqlQualifiedName);
        queryWrapper.eq("user", user);
        queryWrapper.eq("db", db);
        return mysqlDbPrivMapper.selectOne(queryWrapper);
    }

    @Override
    public void createOrUpdate(BeanMysqlDbPriv beanMysqlDbPriv) {
        QueryWrapper<BeanMysqlDbPriv> wrapper = new QueryWrapper<>();
        wrapper.eq("db", beanMysqlDbPriv.getDb());
        wrapper.eq("user", beanMysqlDbPriv.getUser());
        wrapper.eq("mysql_qualified_name", beanMysqlDbPriv.getMysqlQualifiedName());
        BeanMysqlDbPriv dbPriv = mysqlDbPrivMapper.selectOne(wrapper);
        if (dbPriv == null) {
            mysqlDbPrivMapper.insert(beanMysqlDbPriv);
        } else {
            dbPriv.setAuthority(beanMysqlDbPriv.getAuthority());
            mysqlDbPrivMapper.updateById(dbPriv);
        }
    }

}
