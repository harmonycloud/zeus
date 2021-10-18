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

-- 2021.10.18 xutianhong
-- 菜单栏更新
DROP TABLE IF EXISTS `resource_menu`;
CREATE TABLE `resource_menu` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `alias_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中文名称',
  `url` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路径',
  `weight` int(11) DEFAULT NULL COMMENT '权重(排序使用)',
  `icon_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT 'icon名称',
  `parent_id` int(11) DEFAULT NULL COMMENT '父菜单id',
  `module` varchar(0) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模块',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='菜单资源表';

BEGIN;
INSERT INTO `resource_menu` VALUES (1, 'dataOverview', '数据总览', 'dataOverview', 1, 'icon-shujuzonglan', 0, NULL);
INSERT INTO `resource_menu` VALUES (2, 'middlewareRepository', '中间件市场', 'middlewareRepository', 2, 'icon-cangku', 0, NULL);
INSERT INTO `resource_menu` VALUES (3, 'serviceList', '服务列表', 'serviceList', 3, 'icon-fuwuliebiao', 0, NULL);
INSERT INTO `resource_menu` VALUES (4, 'serviceAvailable', '服务暴露', 'serviceAvailable', 4, 'icon-fuwutiaokuan', 0, NULL);
INSERT INTO `resource_menu` VALUES (5, 'monitorAlarm', '监控告警', 'monitorAlarm', 5, 'icon-gaojingshijian', 0, NULL);
INSERT INTO `resource_menu` VALUES (6, 'disasterBackup', '容灾备份', 'disasterBackup', 6, 'icon-rongzaibeifen', 0, NULL);
INSERT INTO `resource_menu` VALUES (7, 'systemManagement', '系统管理', 'systemManagement', 7, 'icon-shezhi01', 0, NULL);
INSERT INTO `resource_menu` VALUES (8, 'dataMonitor', '数据监控', 'monitorAlarm/dataMonitor', 51, NULL, 5, NULL);
INSERT INTO `resource_menu` VALUES (9, 'logDetail', '日志详情', 'monitorAlarm/logDetail', 52, NULL, 5, NULL);
INSERT INTO `resource_menu` VALUES (10, 'alarmCenter', '告警中心', 'monitorAlarm/alarmCenter', 53, NULL, 5, NULL);
INSERT INTO `resource_menu` VALUES (11, 'disasterCenter', '灾备中心', 'disasterBackup/disasterCenter', 61, NULL, 6, NULL);
INSERT INTO `resource_menu` VALUES (12, 'dataSecurity', '数据安全', 'disasterBackup/dataSecurity', 62, NULL, 6, NULL);
INSERT INTO `resource_menu` VALUES (13, 'userManagement', '用户管理', 'systemManagement/userManagement', 71, NULL, 7, NULL);
INSERT INTO `resource_menu` VALUES (14, 'roleManagement', '角色管理', 'systemManagement/roleManagement', 72, NULL, 7, NULL);
INSERT INTO `resource_menu` VALUES (15, 'operationAudit', '操作审计', 'systemManagement/operationAudit', 73, NULL, 7, NULL);
INSERT INTO `resource_menu` VALUES (16, 'resourcePoolManagement', '资源池管理', 'systemManagement/resourcePoolManagement', 74, NULL, 7, NULL);
COMMIT;