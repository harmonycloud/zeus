-- 2021.9.24 xutianhong
-- 添加记录admin.conf
DROP TABLE IF EXISTS `kube_config`;
CREATE TABLE `kube_config` (
  `id` int(32) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `conf` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'admin.conf',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;