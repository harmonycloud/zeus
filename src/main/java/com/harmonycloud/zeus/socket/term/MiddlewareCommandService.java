package com.harmonycloud.zeus.socket.term;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.ARGS;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MYSQL_ARGS;

/**
 * @author xutianhong
 * @Date 2022/2/23 11:37 上午
 */
@Component
public class MiddlewareCommandService {

    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;

    public String getRedisCommand(String clusterId, String namespace, String name) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        JSONObject values = helmChartService.getInstalledValues(name, namespace, cluster);
        String password = values.getString("redisPassword");
        return "redis-cli -h " + name + "-p 6379 -a " + password;
    }

    public String getMysqlCommand(String clusterId, String namespace, String name) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        JSONObject values = helmChartService.getInstalledValues(name, namespace, cluster);
        JSONObject args;
        if (values.containsKey(MYSQL_ARGS)) {
            args = values.getJSONObject(MYSQL_ARGS);
        } else {
            args = values.getJSONObject(ARGS);
        }
        String password = args.getString("root_password");
        return "mysql -uroot -p" + password + "-S /data/db_" + name + "/conf/mysql.sock";
    }

}
