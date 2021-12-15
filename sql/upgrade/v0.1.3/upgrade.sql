-- 2021.12.13 xutianhong
-- 存储已删除中间件的相关信息
DROP TABLE IF EXISTS `cache_middleware`;
CREATE TABLE `cache_middleware` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `namespace` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分区',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件名称',
  `type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件类型',
  `chart_version` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件版本',
  `pvc` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'pvc列表',
  `values_yaml` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='已删除中间件信息缓存表';

-- 角色集群权限关系
DROP TABLE IF EXISTS `cluster_role`;
CREATE TABLE `cluster_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `role_id` int(11) DEFAULT NULL COMMENT '角色id',
  `cluster_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `namespace` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分区',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='角色集群权限表';