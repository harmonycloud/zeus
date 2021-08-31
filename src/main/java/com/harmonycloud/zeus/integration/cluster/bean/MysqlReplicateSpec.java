package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MysqlReplicateSpec {

    private boolean enable;

    private From from;

    private String clusterName;

    public MysqlReplicateSpec() {
    }

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
    @Data
    public static class From {

        private String host;

        private int port;

        private String user;

        private String password;

        public From(String host, int port, String user, String password) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
        }
    }
}

