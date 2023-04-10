package com.middleware.zeus.integration.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.middleware.caas.common.model.dashboard.redis.KeyValueDto;
import com.middleware.zeus.interceptor.MiddlewareApiInterceptor;
import org.springframework.stereotype.Component;

/**
 * @author liyinlong
 * @since 2022/10/26 10:38 上午
 */
@Component
@Address(source = MiddlewareApiAddress.class)
@BaseRequest(interceptor = {MiddlewareApiInterceptor.class})
public interface RedisClient {

    /**
     * 登录
     * @param db
     * @param version redis版本 5或6
     * @param mod redis模式 single、cluster、sentinel
     * @param username 用户名(redis6以上才支持用户名)
     * @param password 密码
     * @param port 端口(single模式才填)
     * @param clusterAddrs redis集群模式连接信息
     * @param sentinelAddrs redis哨兵模式连接信息
     * @return
     */
    @Post(url = "/redis/db/{db}/login")
    JSONObject login(@Var("db") Integer db,
                     @Body(name = "otherMapInfo[host]") String host,
                     @Body(name = "otherMapInfo[version]") String version,
                     @Body(name = "otherMapInfo[mod]") String mod,
                     @Body(name = "otherMapInfo[username]") String username,
                     @Body(name = "otherMapInfo[password]") String password,
                     @Body(name = "otherMapInfo[port]") String port,
                     @Body(name = "otherMapInfo[clusterAddrs]") String clusterAddrs,
                     @Body(name = "otherMapInfo[sentinelAddrs]") String sentinelAddrs);

    /**
     * 查询dbsize
     */
    @Get(url = "/redis/db/{db}/size")
    JSONObject DBSize(@Query("host") String host, @Query("port") String port, @Var("db") Integer db);

    /**
     * 查询所有key
     */
    @Get(url = "/redis/db/{db}/keys")
    JSONObject getAllKeys(@Query("host") String host, @Query("port") String port, @Var("db") Integer db);

    /**
     * 获取key的值
     */
    @Get(url = "/redis/db/{db}/key/{key}")
    JSONObject getKeyValue(@Var("db") Integer db,@Var("key") String key);

    /**
     * 查询所有key,并根据keyword过滤
     */
    @Get(url = "/redis/db/{db}/keys/pattern/{keyword}")
    JSONObject getKeys(@Query("host") String host, @Query("port") String port, @Var("db") Integer db, @Var("keyword") String keyword);

    /**
     * 设置key的值
     * hash value格式 field value，即用空格分隔key value
     *
     *
     */
    @Post(url = "/redis/db/{db}/key/{key}")
    JSONObject setKeyValue(@Var("db") Integer db, @Var("key") String key, @JSONBody KeyValueDto keyValueDto);

    /**
     * 删除key
     */
    @Delete(url = "/redis/db/{db}/key/{key}")
    JSONObject deleteKey(@Var("db") Integer db,@Var("key") String key);

    /**
     * 删除value
     */
    @Delete(url = "/redis/db/{db}/key/{key}/value")
    JSONObject removeValue(@Var("db") Integer db, @Var("key") String key, @JSONBody KeyValueDto keyValueDto);

    /**
     * 重命名key
     */
    @Put(url = "/redis/db/{db}/key/{key}/rename/{newName}")
    JSONObject renameKey(@Var("db") Integer db,@Var("key") String key,@Var("newName") String newName);

    /**
     * 设置key过期时间
     */
    @Put(url = "/redis/db/{db}/key/{key}/expiration")
    JSONObject setKeyExpiration(@Var("db") Integer db, @Var("key") String key, @JSONBody KeyValueDto keyValueDto);

    /**
     * 执行命令
     */
    @Post(url = "/redis/db/{db}/exec")
    JSONObject execCMD(@Query("host") String host, @Query("port") String port, @Var("db") Integer db, @Body("cmd") String cmd);

    /**
     * 渐进式遍历
     */
    @Get(url = "/redis/db/{db}/scan")
    JSONObject scan(@Query("host") String host, @Query("port") String port, @Var("db") Integer db, @Query("keyword") String keyword, @Query("cursor") Integer cursor, @Query("count") Integer count);

}
