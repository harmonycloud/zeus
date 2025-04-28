
-- xutianhong 20250428
-- 新增中间件版本禁用表
drop table if exists `middleware_disable_version`;
CREATE TABLE `middleware_disable_version`
(
    `id`         int NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '集群id',
    `chart_name`  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT 'chart类型',
    `chart_version` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart版本',
    `disable_version`       varchar(128) DEFAULT NULL COMMENT '禁用版本',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='中间件禁用版本';