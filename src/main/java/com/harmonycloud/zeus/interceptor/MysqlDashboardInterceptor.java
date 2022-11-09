package com.harmonycloud.zeus.interceptor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.interceptor.Interceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class MysqlDashboardInterceptor implements Interceptor<JSONObject> {

    @Override
    public void onSuccess(JSONObject data, ForestRequest req, ForestResponse res) {
        log.debug(String.valueOf(data));
        JSONObject err = data.getJSONObject("err");
        data.put("success", true);
        if (err != null) {
            data.put("success", false);
            data.put("message", err.getString("Message"));
        }
        if ("GET".equalsIgnoreCase(req.getType().getName()) || "showTableData".equals(req.getMethod().getMethodName()) || "execSql".equals(req.getMethod().getMethodName())) {
            JSONArray dataAry = convertResult(data);
            data.put("dataAry", dataAry);
        }
    }

    @Override
    public void onError(ForestRuntimeException ex, ForestRequest request, ForestResponse response) {
        Interceptor.super.onError(ex, request, response);
    }

    public JSONArray convertResult(JSONObject jsonObject) {
        JSONArray columnNameAry = jsonObject.getJSONArray("column");
        JSONArray dataAry = jsonObject.getJSONArray("data");
        // 如果一行数据都没查询到，则返回一个空的JsonArray
        if (CollectionUtils.isEmpty(columnNameAry) || CollectionUtils.isEmpty(dataAry)) {
            return new JSONArray();
        }

        JSONArray resArray = new JSONArray();
        for (int i = 0; i < dataAry.size(); ++i) {
            JSONObject obj = new JSONObject();
            JSONArray dataItem = dataAry.getJSONArray(i);
            for (int j = 0; j < columnNameAry.size(); ++j) {
                obj.put(columnNameAry.getString(j), dataItem.getString(j));
            }
            resArray.add(obj);
        }
        return resArray;
    }
}
