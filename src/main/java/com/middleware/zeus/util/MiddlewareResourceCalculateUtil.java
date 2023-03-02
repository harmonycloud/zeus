package com.middleware.zeus.util;

import static com.middleware.caas.common.enums.middleware.ElasticSearchRoleEnum.KIBANA;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import com.middleware.caas.common.enums.middleware.ElasticSearchRoleEnum;
import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.enums.middleware.ResourceUnitEnum;
import com.middleware.caas.common.enums.middleware.RocketMQModeEnum;
import com.middleware.caas.common.model.middleware.Middleware;
import com.middleware.caas.common.model.middleware.MiddlewareQuota;
import com.middleware.tool.numeric.ResourceCalculationUtil;

/**
 * @author xutianhong
 * @Date 2023/1/12 5:06 下午
 */
public class MiddlewareResourceCalculateUtil {

    public static Map<String, Double> middlewareResourceCalculate(Middleware middleware) {
        Map<String, Double> map;
        if (middleware.getType().equals(MiddlewareTypeEnum.MYSQL.getType())) {
            map = calculateMysqlResource(middleware);
        } else if (middleware.getType().equals(MiddlewareTypeEnum.REDIS.getType())) {
            map = calculateRedisResource(middleware);
        } else if (middleware.getType().equals(MiddlewareTypeEnum.ELASTIC_SEARCH.getType())) {
            map = calculateElasticsearchResource(middleware);
        } else if (middleware.getType().equals(MiddlewareTypeEnum.POSTGRESQL.getType())) {
            map = calculatePostgresqlResource(middleware);
        } else if (middleware.getType().equals(MiddlewareTypeEnum.ROCKET_MQ.getType())) {
            map = calculateRocketMqResource(middleware);
        } else if (middleware.getType().equals(MiddlewareTypeEnum.KAFKA.getType())) {
            map = calculateKafkaResource(middleware);
        } else if (middleware.getType().equals(MiddlewareTypeEnum.ZOOKEEPER.getType())) {
            map = calculateZookeeperResource(middleware);
        } else {
            map = calculateZookeeperResource(middleware);
        }
        return map;
    }

    private static Map<String, Double> calculateMysqlResource(Middleware middleware) {
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        Map<String, Double> map = getCommonResource(quota);
        int replicas = middleware.getMysqlDTO().getReplicaCount() + 1;

        // mysql + logrotate + exporter
        double totalCpu = (map.get(CPU) + 0.2 + 0.2) * replicas;
        double totalMemory = (map.get(MEMORY) + 0.2 + 0.2) * replicas;
        double totalStorage = map.get(STORAGE) * replicas;

        if (middleware.getReadWriteProxy() != null && middleware.getReadWriteProxy().getEnabled()) {
            totalCpu += Double.parseDouble(calculateProxyResource(String.valueOf(map.get(CPU)))) * replicas;
            double proxyMemory = Double.parseDouble(calculateProxyResource(String.valueOf(map.get(MEMORY))));
            if (proxyMemory < 0.256){
                proxyMemory = 0.256;
            }
            totalMemory += proxyMemory * replicas;
        }

        map.put(CPU, totalCpu);
        map.put(MEMORY, totalMemory);
        map.put(quota.getStorageClassName(), totalStorage);
        return map;
    }

    private static Map<String, Double> calculateRedisResource(Middleware middleware) {
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        Map<String, Double> map = getCommonResource(quota);
        int replicas = middleware.getQuota().get(middleware.getType()).getNum();

        // redis + exporter
        double totalCpu = (map.get(CPU) + 0.025) * replicas;
        double totalMemory = (map.get(MEMORY) + 0.05) * replicas;
        double totalStorage = map.get(STORAGE) * replicas;

        // 哨兵模式
        if (SENTINEL.equals(middleware.getMode())) {
            MiddlewareQuota sentinelQuota = middleware.getQuota().get(SENTINEL);
            Map<String, Double> sentinel = getCommonResource(sentinelQuota);

            totalCpu += sentinel.get(CPU) * sentinelQuota.getNum();
            totalMemory += sentinel.get(MEMORY) * sentinelQuota.getNum();
        }

        // 读写分离
        if (middleware.getReadWriteProxy() != null && middleware.getReadWriteProxy().getEnabled()) {
            int proxyReplicas = replicas / 2 == 1 ? replicas : replicas / 2;
            totalCpu += proxyReplicas;
            double proxyMemory = Double.parseDouble(calculateProxyResource(String.valueOf(map.get(MEMORY))));
            if (proxyMemory < 0.256) {
                proxyMemory = 0.256;
            } else if (proxyMemory > 2) {
                proxyMemory = 2;
            }
            totalMemory += proxyMemory * proxyReplicas;
        }

        map.put(CPU, totalCpu);
        map.put(MEMORY, totalMemory);
        map.put(quota.getStorageClassName(), totalStorage);
        return map;
    }

    private static Map<String, Double> calculateElasticsearchResource(Middleware middleware) {
        double cpu = 0;
        double memory = 0;
        Map<String, Double> map = new HashMap<>();
        for (String key : ElasticSearchRoleEnum.getValues().keySet()) {
            MiddlewareQuota quota = middleware.getQuota().get(key);
            if (quota == null) {
                continue;
            }
            if (key.equals(KIBANA.getRole())) {
                cpu += Double.parseDouble(quota.getCpu());
                memory +=
                    ResourceCalculationUtil.getResourceValue(quota.getMemory(), MEMORY, ResourceUnitEnum.GI.getUnit());
            } else {
                Map<String, Double> tMap = getCommonResource(quota);
                cpu += tMap.get(CPU) * quota.getNum();
                memory += tMap.get(MEMORY) * quota.getNum();
                if (map.containsKey(quota.getStorageClassName())) {
                    map.put(quota.getStorageClassName(),
                        map.get(quota.getStorageClassName()) + tMap.get(STORAGE) * quota.getNum());
                } else {
                    map.put(quota.getStorageClassName(), tMap.get(STORAGE) * quota.getNum());
                }
            }
        }
        map.put(CPU, cpu);
        map.put(MEMORY, memory);
        return map;
    }

    private static Map<String, Double> calculatePostgresqlResource(Middleware middleware) {
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        Map<String, Double> map = getCommonResource(quota);
        int replicas = middleware.getQuota().get(middleware.getType()).getNum() + 1;

        // postgresql + exporter
        double totalCpu = (map.get(CPU) + 0.1) * replicas;
        double totalMemory = (map.get(MEMORY) + 0.125) * replicas;
        double totalStorage = map.get(STORAGE) * replicas;

        map.put(CPU, totalCpu);
        map.put(MEMORY, totalMemory);
        map.put(quota.getStorageClassName(), totalStorage);
        return map;
    }

    private static Map<String, Double> calculateRocketMqResource(Middleware middleware) {
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        Map<String, Double> map = getCommonResource(quota);
        RocketMQModeEnum modeEnum = RocketMQModeEnum.findByMode(middleware.getMode());
        int replicas = 0;
        switch (modeEnum) {
            case TWO_MASTER:
                replicas = 2;
                break;
            case TWO_MASTER_TWO_SLAVE:
                replicas = 4;
                break;
            case THREE_MASTER_THREE_SLAVE:
                replicas = 6;
                break;
            case DLEDGER:
                replicas = middleware.getRocketMQParam().getReplicas() * middleware.getRocketMQParam().getGroup();
                break;
            default:
        }

        // rocketMq + console + exporter + nameserver
        double totalCpu = map.get(CPU) * replicas + 0.5 + 1 + 2;
        double totalMemory = map.get(MEMORY) * replicas + 1 + 2 + 2;
        double totalStorage = map.get(STORAGE) * replicas;

        map.put(CPU, totalCpu);
        map.put(MEMORY, totalMemory);
        map.put(quota.getStorageClassName(), totalStorage);
        return map;
    }

    private static Map<String, Double> calculateKafkaResource(Middleware middleware) {
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        Map<String, Double> map = getCommonResource(quota);
        int replicas = middleware.getQuota().get(middleware.getType()).getNum();

        // kafka + exporter + manager
        double totalCpu = map.get(CPU) * replicas + 0.2 + 0.5;
        double totalMemory = map.get(MEMORY) * replicas + 0.5 + 0.5;
        double totalStorage = map.get(STORAGE) * replicas;

        map.put(CPU, totalCpu);
        map.put(MEMORY, totalMemory);
        map.put(quota.getStorageClassName(), totalStorage);
        return map;
    }

    private static Map<String, Double> calculateZookeeperResource(Middleware middleware) {
        MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());
        Map<String, Double> map = getCommonResource(quota);
        int replicas = middleware.getQuota().get(middleware.getType()).getNum();

        // rocketMq + exporter + manager
        double totalCpu = map.get(CPU) * replicas;
        double totalMemory = map.get(MEMORY) * replicas;
        double totalStorage = map.get(STORAGE) * replicas;

        map.put(CPU, totalCpu);
        map.put(MEMORY, totalMemory);
        map.put(quota.getStorageClassName(), totalStorage);
        return map;
    }

    public static Map<String, Double> getCommonResource(MiddlewareQuota quota) {
        double cpu = Double.parseDouble(quota.getCpu());
        double memory =
            ResourceCalculationUtil.getResourceValue(quota.getMemory(), MEMORY, ResourceUnitEnum.GI.getUnit());
        double storage = ResourceCalculationUtil.getResourceValue(quota.getStorageClassQuota(), MEMORY,
            ResourceUnitEnum.GI.getUnit());

        Map<String, Double> map = new HashMap<>();
        map.put(CPU, cpu);
        map.put(MEMORY, memory);
        map.put(STORAGE, storage);
        return map;
    }

    public static String calculateProxyResource(String num) {
        BigDecimal bd = new BigDecimal(num).divide(new BigDecimal("4"));
        return bd.setScale(2, RoundingMode.UP).toString();
    }

}
