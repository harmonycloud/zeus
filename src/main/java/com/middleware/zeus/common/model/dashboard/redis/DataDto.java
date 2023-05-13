package com.middleware.zeus.common.model.dashboard.redis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.util.DateTimeUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author liyinlong
 * @since 2022/10/26 11:00 上午
 */
@ApiModel("redis data对象")
@Accessors(chain = true)
@Data
@Slf4j
public class DataDto {

    @ApiModelProperty("key")
    private String key;

    @ApiModelProperty("key类型(string,set,zset,list,hash)")
    private String keyType;

    @ApiModelProperty("过期时间(s)")
    private Integer expiration;

    @ApiModelProperty("string类型value值")
    private String stringValue;

    @ApiModelProperty("list类型value值")
    private List<String> listValue;

    @ApiModelProperty("hash类型value值")
    private List<HashDto> hashValue;

    @ApiModelProperty("set类型value值")
    private Set<String> setValue;

    @ApiModelProperty("zset类型value值")
    private List<ZSetDto> zsetValue;

    /**
     * redis kv对象
     */
    private JSONObject data;

    public DataDto() {
    }

    public DataDto(JSONObject data) {
        this.setValue(data);
    }

    private void setValue(JSONObject data) {
        this.keyType = data.getString("type");
        this.key = data.getString("key");
        this.expiration = DateTimeUtil.convertExpirationToSeconds(data.getString("expiration"));
        if (!StringUtils.isEmpty(keyType)) {
            switch (keyType) {
                case "string":
                    setStringValue(data);
                    break;
                case "list":
                    setListValue(data);
                    break;
                case "hash":
                    setHashValue(data);
                    break;
                case "set":
                    setSetValue(data);
                    break;
                case "zset":
                    setZSetValue(data);
                    break;
                default:
                    log.error("不支持该类型");
            }

        }
    }

    private void setStringValue(JSONObject data) {
        this.stringValue = data.getString("value");
    }

    private void setListValue(JSONObject data) {
        JSONArray jsonArray = data.getJSONArray("value");
        this.listValue = new ArrayList<>();
        for (Object o : jsonArray) {
            this.listValue.add(o.toString());
        }
    }

    private void setHashValue(JSONObject data) {
        JSONObject object = data.getJSONObject("value");
        this.hashValue = new ArrayList<>();
        for (String key : object.keySet()) {
            HashDto hashDto = new HashDto();
            hashDto.setField(key);
            hashDto.setValue(object.getString(key));
            this.hashValue.add(hashDto);
        }
    }

    private void setSetValue(JSONObject data) {
        JSONArray value = data.getJSONArray("value");
        this.setValue = new HashSet<>();
        for (Object o : value) {
            this.setValue.add(o.toString());
        }
    }

    private void setZSetValue(JSONObject data) {
        JSONArray value = data.getJSONArray("value");
        this.zsetValue = new ArrayList<>();
        for (Object o : value) {
            JSONObject jsonObject = (JSONObject) o;
            ZSetDto zSetDto = new ZSetDto();
            zSetDto.setMember(jsonObject.getString("Member"));
            zSetDto.setScore(jsonObject.getString("Score"));
            this.zsetValue.add(zSetDto);
        }
    }

}
