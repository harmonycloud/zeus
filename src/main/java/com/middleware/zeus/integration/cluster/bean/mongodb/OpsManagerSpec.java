package com.middleware.zeus.integration.cluster.bean.mongodb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbBackupServerDto;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2025/8/1 11:08
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpsManagerSpec {
    private String adminCredentials;
    private ApplicationDatabase applicationDatabase;
    private Backup backup;
    private Configuration configuration;
    private ExternalConnectivity externalConnectivity;
    private int replicas;
    private StatefulSet statefulSet;
    private String version;

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplicationDatabase {
        private int members;
        private String topology;
        private String version;

        // Getters and Setters
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Backup {
        private boolean enabled;
        private List<Store> s3OpLogStores;
        private List<Store> s3Stores;


        @Data
        @Accessors(chain = true)
        @JsonIgnoreProperties(ignoreUnknown = true)
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Store {
            private boolean customCertificate;
            private List<CustomCertificateSecretRef> customCertificateSecretRefs;
            private String name;
            private boolean pathStyleAccessEnabled;
            private String s3BucketEndpoint;
            private String s3BucketName;
            private S3SecretRef s3SecretRef;

            @Data
            @Accessors(chain = true)
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class CustomCertificateSecretRef {
                private String key;
                private String name;
            }

            @Data
            @Accessors(chain = true)
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class S3SecretRef {
                private String name;
            }

            public Store(MongodbBackupServerDto.BackupSerer backupSerer) {
                this.customCertificate = true;
                this.name = backupSerer.getBucket();
                this.pathStyleAccessEnabled = true;
                this.s3BucketEndpoint = backupSerer.getProtocol() + "://" + backupSerer.getUrl()
                    + (backupSerer.getPort() == null ? "" : ":" + backupSerer.getPort());
                this.s3BucketName = backupSerer.getBucket();
                this.s3SecretRef = new S3SecretRef();
                this.s3SecretRef.setName("mongodb-s3-credentials");
            }
        }
        }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration {
        private String automationVersionsSource;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExternalConnectivity {
        private int port;
        private String type;
    }


}
