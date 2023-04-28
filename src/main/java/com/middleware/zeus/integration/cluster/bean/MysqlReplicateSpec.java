package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MysqlReplicateSpec {

    private boolean enable;

    private From from;

    private String clusterName;


    public MysqlReplicateSpec(boolean enable, String clusterName, String host, int port, String user, String password) {
        this.enable = enable;
        this.from = new From(host, port, user, password);
        this.clusterName = clusterName;
    }

    /**
     * 源实例的信息
     *
     * @author liyinlong
     * @date 2021/8/11 2:40 下午
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class From {

        private String host;

        private int port;

        private String user;

        private String password;

    }
}

