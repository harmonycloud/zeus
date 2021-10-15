-- zeus-v0.0.2 -> zeus-v0.1.0 sql 记录

-- 2021.9.22 xutianhong
-- 1.middleware_info 添加  chart列
ALTER TABLE `middleware_info` ADD COLUMN chart mediumblob DEFAULT NULL COMMENT 'helm chart包';
-- 2.删除middleware_info cluster_id列
ALTER TABLE `middleware_info` DROP COLUMN cluster_id;
-- 3.删除middleware_info status列
ALTER TABLE `middleware_info` DROP COLUMN status;
-- 4.添加middleware_info operator_name列
ALTER TABLE `middleware_info` ADD COLUMN operator_name varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'operator名称';
-- 5.添加集群-中间件关联表
DROP TABLE IF EXISTS `cluster_middleware_info`;
CREATE TABLE `cluster_middleware_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `chart_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包名称',
  `chart_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包版本',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'operator状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='集群-中间件关联表';
-- 6.角色表添加create_time
ALTER TABLE `role` ADD COLUMN create_time timestamp NULL DEFAULT NULL COMMENT '创建时间';
-- 7.用户表password_time 和 creator
ALTER TABLE `user` ADD COLUMN password_time timestamp NULL DEFAULT NULL COMMENT '密码修改时间';
ALTER TABLE `user` ADD COLUMN creator varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建者';
-- 8.角色表添加列parent
ALTER TABLE `role` ADD COLUMN parent int(64) DEFAULT NULL COMMENT '父角色id';


-- 2021.10.14 xutianhong
-- 修改admin初始化密码
UPDATE `user` SET password = '6DA05F9A0ED31ABEEFD41C768B2E7233' WHERE username = 'admin';