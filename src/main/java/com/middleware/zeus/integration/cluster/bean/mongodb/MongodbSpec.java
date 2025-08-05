package com.middleware.zeus.integration.cluster.bean.mongodb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2025/8/1 12:54
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MongodbSpec {
    private AdditionalMongodConfig additionalMongodConfig;
    private Backup backup;
    private String credentials;
    private String logLevel;
    private Integer members;
    private OpsManager opsManager;
    private PodSpec podSpec;
    private Prometheus prometheus;
    private Security security;
    private String type;
    private String version;

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdditionalMongodConfig {
        private SetParameter setParameter;

        @Data
        @Accessors(chain = true)
        public static class SetParameter {
            private Boolean allowDiskUseByDefault;
            private Integer connPoolMaxConnsPerHost;
            private Integer cursorTimeoutMillis;
            private Integer maxNumActiveUserIndexBuilds;
            private Boolean ttlMonitorEnabled;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class Backup {
        private String mode;
        private SnapshotSchedule snapshotSchedule;

        @Data
        @Accessors(chain = true)
        public static class SnapshotSchedule {
            @ApiModelProperty("快照间隔小时：6 12 18 24")
            private Integer snapshotIntervalHours;

            @ApiModelProperty("最近快照的保留天数: 2 3 4 5")
            private Integer snapshotRetentionDays;

            @ApiModelProperty("每日快照保留时间")
            private Integer dailySnapshotRetentionDays;

            @ApiModelProperty("每周快照保留时间")
            private Integer weeklySnapshotRetentionWeeks;

            @ApiModelProperty("每月快照保留时间")
            private Integer monthlySnapshotRetentionMonths;

            @ApiModelProperty("支持创建时间点快照的历史时间范围")
            private Integer pointInTimeWindowHours;

            @ApiModelProperty("执行全量备份的时间")
            private String fullIncrementalDayOfWeek;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class OpsManager {
        private ConfigMapRef configMapRef;

        @Data
        @Accessors(chain = true)
        public static class ConfigMapRef {
            private String name;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class PodSpec {
        private Persistence persistence;
        private PodTemplate podTemplate;

        @Data
        @Accessors(chain = true)
        public static class Persistence {
            private Single single;

            @Data
            @Accessors(chain = true)
            public static class Single {
                private String storage;
                private String storageClass;
            }
        }

        @Data
        @Accessors(chain = true)
        public static class PodTemplate {
            private Metadata metadata;
            private Spec spec;

            @Data
            @Accessors(chain = true)
            public static class Metadata {
                private Map<String, String> annotations;
            }

            @Data
            @Accessors(chain = true)
            public static class Spec {
                private Affinity affinity;
                private List<Container> containers;
                private List<InitContainer> initContainers;
                private List<Toleration> tolerations;
                private List<TopologySpreadConstraint> topologySpreadConstraints;

                @Data
                @Accessors(chain = true)
                public static class Affinity {
                    private PodAntiAffinity podAntiAffinity;

                    @Data
                    @Accessors(chain = true)
                    public static class PodAntiAffinity {
                        private List<RequiredDuringSchedulingIgnoredDuringExecution> requiredDuringSchedulingIgnoredDuringExecution;

                        @Data
                        @Accessors(chain = true)
                        public static class RequiredDuringSchedulingIgnoredDuringExecution {
                            private LabelSelector labelSelector;
                            private String topologyKey;

                            @Data
                            @Accessors(chain = true)
                            public static class LabelSelector {
                                private List<MatchExpression> matchExpressions;

                                @Data
                                @Accessors(chain = true)
                                public static class MatchExpression {
                                    private String key;
                                    private String operator;
                                    private List<String> values;
                                }
                            }
                        }
                    }
                }

                @Data
                @Accessors(chain = true)
                public static class Container {
                    private List<Env> env;
                    private String image;
                    private String name;
                    private Resources resources;

                    @Data
                    @Accessors(chain = true)
                    public static class Env {
                        private String name;
                        private String value;
                    }

                    @Data
                    @Accessors(chain = true)
                    public static class Resources {
                        private Limits limits;
                        private Requests requests;

                        @Data
                        @Accessors(chain = true)
                        public static class Limits {
                            private String cpu;
                            private String memory;
                        }

                        @Data
                        @Accessors(chain = true)
                        public static class Requests {
                            private String cpu;
                            private String memory;
                        }
                    }
                }

                @Data
                @Accessors(chain = true)
                public static class InitContainer {
                    private String name;
                    private Container.Resources resources;
                }

                @Data
                @Accessors(chain = true)
                public static class Toleration {
                    private String effect;
                    private String key;
                    private String operator;
                    private String value;
                }

                @Data
                @Accessors(chain = true)
                public static class TopologySpreadConstraint {
                    private LabelSelector labelSelector;
                    private Integer maxSkew;
                    private String topologyKey;
                    private String whenUnsatisfiable;

                    @Data
                    @Accessors(chain = true)
                    public static class LabelSelector {
                        private Map<String, String> matchLabels;
                    }
                }
            }
        }
    }

    @Data
    @Accessors(chain = true)
    public static class Prometheus {
        private PasswordSecretRef passwordSecretRef;
        private String username;

        @Data
        @Accessors(chain = true)
        public static class PasswordSecretRef {
            private String name;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class Security {
        private Authentication authentication;

        @Data
        @Accessors(chain = true)
        public static class Authentication {
            private Boolean enabled;
            private Boolean ignoreUnknownUsers;
            private List<String> modes;
        }
    }


}
