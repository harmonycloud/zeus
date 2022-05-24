
-- 20220523 xutianhong
-- 添加可用区表
DROP TABLE IF EXISTS `active_area`;
create table `active_area` (
    `id`         int NOT NULL auto_increment comment '自增id',
    `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL comment '集群id',
    `area_name`  varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL comment '可用区名称',
    `alias_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL comment '可用区中文别名',
    `init` tinyint(1) DEFAULT NULL COMMENT '是否初始化',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin comment '可用区'