package com.harmonycloud.zeus.integration.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.*;
import com.harmonycloud.caas.common.model.dashboard.redis.KeyValueDto;
import com.harmonycloud.zeus.interceptor.MiddlewareApiInterceptor;
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
     * 查询dbsize
     */
    @Get(url = "/redis/{host}/db/{db}/size")
    JSONObject DBSize(@Var("host") String host, @Var("db") Integer db);

    /**
     * 查询所有key
     */
    @Get(url = "/redis/{host}/db/{db}/keys")
    JSONObject getAllKeys(@Var("host") String host, @Var("db") Integer db);

    /**
     * 获取key的值
     */
    @Get(url = "/redis/{host}/db/{db}/key/{key}")
    JSONObject getKeyValue(@Var("host") String host, @Var("db") Integer db,@Var("key") String key);

    /**
     * 查询所有key,并根据keyword过滤
     */
    @Get(url = "/redis/{host}/db/{db}/keys/pattern/{keyword}")
    JSONObject getKeys(@Var("host") String host, @Var("db") Integer db,@Var("keyword") String keyword);

    /**
     * 设置key的值
     * hash value格式 field value，即用空格分隔key value
     *
     *
     */
    @Post(url = "/redis/{host}/db/{db}/key/{key}")
    JSONObject setKeyValue(@Var("host") String host, @Var("db") Integer db, @Var("key") String key, @JSONBody KeyValueDto keyValueDto);

    /**
     * 删除key
     */
    @Delete(url = "/redis/{host}/db/{db}/key/{key}")
    JSONObject deleteKey(@Var("host") String host, @Var("db") Integer db,@Var("key") String key);

    /**
     * 删除value
     */
    @Delete(url = "/redis/{host}/db/{db}/key/{key}/value")
    JSONObject removeValue(@Var("host") String host, @Var("db") Integer db, @Var("key") String key, @JSONBody KeyValueDto keyValueDto);

    /**
     * 重命名key
     */
    @Put(url = "/redis/{host}/db/{db}/key/{key}/rename/{newName}")
    JSONObject renameKey(@Var("host") String host, @Var("db") Integer db,@Var("key") String key,@Var("newName") String newName);

    /**
     * 设置key过期时间
     */
    @Put(url = "/redis/{host}/db/{db}/key/{key}/rename/{newName}")
    JSONObject setKeyExpiration(@Var("host") String host, @Var("db") Integer db, @Var("key") String key, @JSONBody KeyValueDto keyValueDto);

    /**
     * 执行命令
     */
    @Post(url = "/redis/{host}/db/{db}/exec")
    JSONObject execCMD(@Var("host") String host, @Var("db") Integer db, @Body("cmd") String cmd);

}
