package com.middleware.zeus.common.model.dashboard.redis;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/10/27 2:56 下午
 */
@ApiModel("redis kv对象")
@Accessors(chain = true)
@Data
public class KeyValueDto {

    @ApiModelProperty("key")
    private String key;

    @ApiModelProperty("key类型(string,set,zset,list,hash)")
    private String keyType;

    @ApiModelProperty("过期时间(s)")
    private String expiration;

    @ApiModelProperty("string或set 类型的value值")
    private String value;

    @ApiModelProperty("list类型的value值")
    private ListDto listValue;

    @ApiModelProperty("zset类型的value")
    private ZSetDto zsetValue;

    @ApiModelProperty("hash类型的value")
    private HashDto hashValue;

    public String wrapValue() {
        switch (keyType) {
            case "string":
            case "set":
                return value;
            case "list":
                return convertListToString();
            case "zset":
                return convertZSetToString();
            case "hash":
                return convertHashToString();
            default:
                return "";
        }
    }

    private String convertListToString() {
        return JSONObject.toJSONString(this.listValue);
    }

    private String convertZSetToString() {
        return JSONObject.toJSONString(this.zsetValue);
    }

    private String convertHashToString() {
        return JSONObject.toJSONString(hashValue);
    }

}
