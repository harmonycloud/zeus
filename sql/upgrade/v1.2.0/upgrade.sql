-- 2022.8.31 liyinlong
-- 修改分区字段长度
ALTER TABLE `alert_rule_id` MODIFY `namespace` VARCHAR(256);
-- 添加ingress类型字段
ALTER TABLE `cluster_ingress_components` ADD COLUMN `type` VARCHAR(32) DEFAULT NULL COMMENT "ingress类型 nginx或traefik";
UPDATE `cluster_ingress_components` SET `type`="nginx" WHERE `type` != "traefik" OR `type` IS NULL;

-- 2022.09.14 liyinlong
-- 添加可用区表
DROP TABLE IF EXISTS `active_area`;
CREATE TABLE `active_area` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `cluster_id` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '集群id',
    `area_name` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '可用区名称',
    `alias_name` varchar(128) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '可用区中文别名',
    `init` tinyint(1) DEFAULT NULL COMMENT '是否初始化',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='可用区';

-- 2022.09.14 xutianhong
-- menu更新
DELETE FROM resource_menu;
INSERT INTO `resource_menu` VALUES (1,'dataOverview','数据总览','dataOverview',1,'icon-shujuzonglan',0,NULL);
INSERT INTO `resource_menu` VALUES (2,'middlewareRepository','中间件市场','middlewareRepository',2,'icon-cangku',0,NULL);
INSERT INTO `resource_menu` VALUES (3,'myProject','我的项目','myProject',3,'icon-wodexiangmu',0,NULL);
INSERT INTO `resource_menu` VALUES (4,'serviceList','服务列表','serviceList',4,'icon-fuwuliebiao',0,NULL);
INSERT INTO `resource_menu` VALUES (5,'serviceAvailable','服务暴露','serviceAvailable',5,'icon-fuwutiaokuan',0,NULL);
INSERT INTO `resource_menu` VALUES (6,'storageManagement','存储管理','storageManagement',6,'icon-cunchuguanli',0,NULL);
INSERT INTO `resource_menu` VALUES (7,'activeActive','同城双活','activeActive',7,'icon-gky',0,NULL);
INSERT INTO `resource_menu` VALUES (8,'backupService','备份服务','backupService',8,'icon-beifenfuwu',0,NULL);
INSERT INTO `resource_menu` VALUES (9,'monitorAlarm','监控告警','monitorAlarm',9,'icon-gaojingshijian',0,NULL);
INSERT INTO `resource_menu` VALUES (10,'disasterBackup','灾备中心','disasterBackup',10,'icon-rongzaibeifen',0,NULL);
INSERT INTO `resource_menu` VALUES (11,'systemManagement','系统管理','systemManagement',11,'icon-shezhi01',0,NULL);
INSERT INTO `resource_menu` VALUES (12,'backupTask','备份任务','backupService/backupTask',81,'icon-fuwutiaokuan',8,NULL);
INSERT INTO `resource_menu` VALUES (13,'backupPosition','备份位置','backupService/backupPosition',82,'icon-fuwutiaokuan',8,NULL);
INSERT INTO `resource_menu` VALUES (14,'dataMonitor','数据监控','monitorAlarm/dataMonitor',91,NULL,9,NULL);
INSERT INTO `resource_menu` VALUES (15,'logDetail','日志详情','monitorAlarm/logDetail',892,NULL,9,NULL);
INSERT INTO `resource_menu` VALUES (16,'alarmCenter','服务告警','monitorAlarm/alarmCenter',93,NULL,9,NULL);
INSERT INTO `resource_menu` VALUES (17,'resourcePoolManagement','集群管理','systemManagement/resourcePoolManagement',111,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (18,'userManagement','用户管理','systemManagement/userManagement',112,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (19,'projectManagement','项目管理','systemManagement/projectManagement',113,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (20,'roleManagement','角色管理','systemManagement/roleManagement',114,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (21,'systemAlarm','系统告警','systemManagement/systemAlarm',115,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (22,'operationAudit','操作审计','systemManagement/operationAudit',116,NULL,11,NULL);