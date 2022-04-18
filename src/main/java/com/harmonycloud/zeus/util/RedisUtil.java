package com.harmonycloud.zeus.util;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.RedisAccessInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yushuaikang
 * @date 2022/3/30 下午3:11
 */
public class RedisUtil {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     *
     * @Title: set
     * @Description: 写入缓存并指定库
     * @param key
     * @param value 为对象时flag_json必须为true
     * @param db    缓存的数据库
     * @param flag_json  是否将value值转为json
     * @param timeOut  时效（秒）   永久传null
     * @return boolean
     */
    public boolean set(final String key, String value,int db,boolean flag_json,Long timeOut) {
        boolean result = false;
        try {
            RedisConnection redisConnection = redisTemplate.getConnectionFactory().getConnection();
            DefaultStringRedisConnection stringRedisConnection = new DefaultStringRedisConnection(redisConnection);
            stringRedisConnection.select(db);
            if(flag_json){
                stringRedisConnection.set(key, value);
            }else{
                stringRedisConnection.set(key, value.toString());
            }
            if(timeOut != null && timeOut != 0){
                stringRedisConnection.expire(key, timeOut);
            }
            stringRedisConnection.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     *
     * @Title: get
     * @Description: 读取指定db的缓存
     * @param key
     * @param db
     * @return Object
     */
    public Object get(final String key,int db) {
        Object result = null;
        try {
            RedisConnection redisConnection = redisTemplate.getConnectionFactory().getConnection();
            DefaultStringRedisConnection stringRedisConnection = new DefaultStringRedisConnection(redisConnection);
            stringRedisConnection.select(db);
            result = stringRedisConnection.get(key);
            stringRedisConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 删除指定db的key
     *
     * @param key
     * @param db
     */
    public void remove(final String key ,int db) {
        try {
            RedisConnection redisConnection = redisTemplate.getConnectionFactory().getConnection();
            DefaultStringRedisConnection stringRedisConnection = new DefaultStringRedisConnection(redisConnection);
            stringRedisConnection.select(db);
            if(stringRedisConnection.exists(key)){
                stringRedisConnection.del(key);
            }
            stringRedisConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断缓存中是否有对应的value
     * @param key
     * @return
     */
    public boolean exists(final String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * redis哨兵模式连接
     * @param redisAccessInfo
     * @return
     */
    public static Jedis getRedisSentinelIsOk(RedisAccessInfo redisAccessInfo) {
        try {
            Jedis jedis = new Jedis(redisAccessInfo.getHost(), Integer.parseInt(redisAccessInfo.getPort()));
            if (!StringUtils.isEmpty(redisAccessInfo.getPassword())) {
                jedis.auth(redisAccessInfo.getPassword());
            }
            return jedis;
        } catch (Exception e) {
            throw new BusinessException(ErrorMessage.REDIS_SERVER_CONNECT_FAILED);
        }
    }

    /**
     * redis集群模式连接
     * @param redisAccessInfo
     * @return
     */
    public static JedisCluster getRedisClusterIsOk(RedisAccessInfo redisAccessInfo) {
        //创建jedisCluster对象，有一个参数 nodes是Set类型，Set包含若干个HostAndPort对象
        JedisPoolConfig config = new JedisPoolConfig();
        config .setMaxTotal(500);
        config .setMinIdle(2);
        config .setMaxIdle(500);
        config .setMaxWaitMillis(10000);
        config .setTestOnBorrow(true);
        config .setTestOnReturn(true);
        Set<HostAndPort> nodes = new HashSet<>();
        nodes.add(new HostAndPort("10.108.123.56",30121));

//        redisAccessInfo.getHostAndPort().forEach(hostAndPort -> {
//            for (String key : hostAndPort.keySet()) {
//                nodes.add(new HostAndPort(key,hostAndPort.get(key)));
//            }
//        });
        return new JedisCluster(nodes,1000,1000,1,redisAccessInfo.getPassword(),config);
    }

}
