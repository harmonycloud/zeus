package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import redis.clients.jedis.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author yushuaikang
 * @date 2022/3/31 上午10:18
 */
@Accessors(chain = true)
@Data
@ApiModel("redis参数")
public class RedisDbDTO {

    private String db;

    private String key;

    private String type;

    private String timeOut;

    @ApiModelProperty("out代表外部新增，inside代表内部新增")
    private String status;

    /**
     * 入参
     */
    @ApiModelProperty("type为string时的value")
    private String value;

    @ApiModelProperty("type为list时的value")
    private Map<String, String> list;

    @ApiModelProperty("type为hash时的value")
    private Map<String, String> hash;
    private Map<String, String> oldHash;

    @ApiModelProperty("type为set时的value")
    private String set;
    private String oldSet;

    @ApiModelProperty("type为set时的value")
    private Map<String, String> zset;
    private Map<String, String> oldZset;

    /**
     * 出参
     */
    @ApiModelProperty("type为string时的value")
    private String values;

    @ApiModelProperty("type为list时的value")
    private List<String> lists;

    @ApiModelProperty("type为hash时的value")
    private Map<String, String> hashs;

    @ApiModelProperty("type为set时的value")
    private Set<String> sets;

    @ApiModelProperty("type为set时的value")
    private Set<Tuple> zsets;

}
