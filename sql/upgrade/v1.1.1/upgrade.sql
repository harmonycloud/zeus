-- 2022.6.9 xutianhong
-- 添加中间件类型映照表
DROP TABLE IF EXISTS `middleware_cr_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `middleware_cr_type` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `chart_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包名称',
  `cr_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'cr类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='中间件类型映照表';

-- 2022.6.15 yushuaikang
-- 备份位置表
DROP TABLE IF EXISTS `middleware_backup_address`;
CREATE TABLE `middleware_backup_address` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address_id` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '标识',
  `name` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '中文名称',
  `type` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '类型',
  `bucket_name` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'bucket名称',
  `access_key_id` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '用户ID',
  `secret_access_key` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '密码',
  `endpoint` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '地址',
  `ftp_host` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'FTP主机服务器',
  `ftp_user` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'FTP登录用户名',
  `ftp_password` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'FTP登录密码',
  `ftp_port` int(11) DEFAULT NULL COMMENT 'FTP端口',
  `server_host` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '服务器地址',
  `server_user` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '服务器用户名',
  `server_password` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '服务器密码',
  `server_port` int(11) DEFAULT NULL COMMENT '服务器端口',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8 COLLATE=utf8_bin

-- 2022.6.25 yushuaikang
-- 备份任务映射表
DROP TABLE IF EXISTS `backup_name`;
CREATE TABLE `backup_name` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `backup_id` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '备份任务标识',
  `backup_name` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '备份任务名称',
  `cluster_id` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '集群ID',
  `backup_type` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '备份类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8 COLLATE=utf8_bin

-- 2022.6.15 yushuaikang
-- 备份位置集群映射表
DROP TABLE IF EXISTS `backup_address_cluster`;
CREATE TABLE `backup_address_cluster` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `backup_address_id` int(11) DEFAULT NULL COMMENT '备份地址ID',
  `cluster_id` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '集群ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8 COLLATE=utf8_bin

-- 2020.6.20 xutianhong
-- 菜单更新
DELETE FROM resource_menu;
INSERT INTO `resource_menu` VALUES (1,'dataOverview','数据总览','dataOverview',1,'icon-shujuzonglan',0,NULL);
INSERT INTO `resource_menu` VALUES (2,'middlewareRepository','中间件市场','middlewareRepository',2,'icon-cangku',0,NULL);
INSERT INTO `resource_menu` VALUES (3,'myProject','我的项目','myProject',3,'icon-wodexiangmu',0,NULL);
INSERT INTO `resource_menu` VALUES (4,'serviceList','服务列表','serviceList',4,'icon-fuwuliebiao',0,NULL);
INSERT INTO `resource_menu` VALUES (5,'serviceAvailable','服务暴露','serviceAvailable',5,'icon-fuwutiaokuan',0,NULL);
INSERT INTO `resource_menu` VALUES (6,'storageManagement','存储管理','storageManagement',6,'icon-cunchuguanli',0,NULL);
INSERT INTO `resource_menu` VALUES (7,'backupService','备份服务','backupService',7,'icon-beifenfuwu',0,NULL);
INSERT INTO `resource_menu` VALUES (8,'monitorAlarm','监控告警','monitorAlarm',8,'icon-gaojingshijian',0,NULL);
INSERT INTO `resource_menu` VALUES (9,'disasterBackup','灾备中心','disasterBackup',9,'icon-rongzaibeifen',0,NULL);
INSERT INTO `resource_menu` VALUES (10,'systemManagement','系统管理','systemManagement',10,'icon-shezhi01',0,NULL);
INSERT INTO `resource_menu` VALUES (11,'backupTask','备份任务','backupService/backupTask',71,'icon-fuwutiaokuan',7,NULL);
INSERT INTO `resource_menu` VALUES (12,'backupPosition','备份位置','backupService/backupPosition',72,'icon-fuwutiaokuan',7,NULL);
INSERT INTO `resource_menu` VALUES (13,'dataMonitor','数据监控','monitorAlarm/dataMonitor',81,NULL,8,NULL);
INSERT INTO `resource_menu` VALUES (14,'logDetail','日志详情','monitorAlarm/logDetail',82,NULL,8,NULL);
INSERT INTO `resource_menu` VALUES (15,'alarmCenter','服务告警','monitorAlarm/alarmCenter',83,NULL,8,NULL);
INSERT INTO `resource_menu` VALUES (16,'resourcePoolManagement','集群管理','systemManagement/resourcePoolManagement',101,NULL,10,NULL);
INSERT INTO `resource_menu` VALUES (17,'userManagement','用户管理','systemManagement/userManagement',102,NULL,10,NULL);
INSERT INTO `resource_menu` VALUES (18,'projectManagement','项目管理','systemManagement/projectManagement',103,NULL,10,NULL);
INSERT INTO `resource_menu` VALUES (19,'roleManagement','角色管理','systemManagement/roleManagement',104,NULL,10,NULL);
INSERT INTO `resource_menu` VALUES (20,'systemAlarm','系统告警','systemManagement/systemAlarm',105,NULL,10,NULL);
INSERT INTO `resource_menu` VALUES (21,'operationAudit','操作审计','systemManagement/operationAudit',106,NULL,10,NULL);