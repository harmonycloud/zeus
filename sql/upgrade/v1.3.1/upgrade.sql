-- 20230313 xutianhong
-- 创建组织表
DROP TABLE IF EXISTS `organization`;
CREATE TABLE `organization` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `organ_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组织id',
    `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组织名称',
    `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织表';

-- 创建组织用户关联表
DROP TABLE IF EXISTS `organization_user`;
CREATE TABLE `organization_user` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `organ_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组织id',
    `username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
    `role_id` int DEFAULT NULL COMMENT '角色id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织用户关联表';

-- 创建组织备份服务器表
DROP TABLE IF EXISTS `organization_backup_server`;
CREATE TABLE `organization_backup_server` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
    `organ_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组织id',
    `backup_server_id` int DEFAULT NULL COMMENT '备份服务器id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织备份服务器表';

-- 创建组织项目表
DROP TABLE IF EXISTS `organization_project`;
CREATE TABLE `organization_project` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `organ_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组织id',
    `project_id` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织项目关联表';

-- 创建平台配额表
DROP TABLE IF EXISTS `platform_quota`;
CREATE TABLE `platform_quota` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `uid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'uid',
    `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型: ORGAN|PROJECT',
    `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
    `target` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'CPU|MEMORY|STORAGE',
    `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
    `quota` double DEFAULT NULL COMMENT '配额',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台配额表';

-- 用户角色表添加组织id字段
alter table role_user add organ_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci null comment '组织id' after id;
-- 项目分区表添加组织id字段
alter table project_namespace add organ_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci null comment '组织id' after id;
-- 项目备份服务器表添加组织id字段
alter table project_backup_server add organ_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci null comment '组织id' after id;
-- 项目备份服务器地址表添加组织id字段
alter table backup_position add organ_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci null comment '组织id' after name;
-- 项目表添加组织id字段
alter table project add organ_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci null comment '组织id' after id;

-- 初始化组织管理员角色
insert into `role` values(null, '组织管理员', '拥有组织管理权限', null, null, null);
-- menu修改
delete from `resource_menu`;
INSERT INTO `resource_menu` VALUES (1,'dataOverview','数据总览','dataOverview',1,'icon-shujuzonglan',0,NULL);
INSERT INTO `resource_menu` VALUES (2,'middlewareRepository','中间件市场','middlewareRepository',2,'icon-cangku',0,NULL);
INSERT INTO `resource_menu` VALUES (3,'myProject','我的项目','myProject',4,'icon-wodexiangmu',0,NULL);
INSERT INTO `resource_menu` VALUES (4,'serviceList','服务列表','serviceList',6,'icon-fuwuliebiao',0,NULL);
INSERT INTO `resource_menu` VALUES (5,'serviceAvailable','服务暴露','serviceAvailable',7,'icon-fuwutiaokuan',0,NULL);
INSERT INTO `resource_menu` VALUES (6,'storageManagement','存储管理','storageManagement',8,'icon-cunchuguanli',0,NULL);
INSERT INTO `resource_menu` VALUES (7,'activeActive','同城双活','activeActive',9,'icon-gky',0,NULL);
INSERT INTO `resource_menu` VALUES (8,'backupService','备份服务','backupService',10,'icon-beifenfuwu',0,NULL);
INSERT INTO `resource_menu` VALUES (9,'monitorAlarm','监控告警','monitorAlarm',11,'icon-gaojingshijian',0,NULL);
INSERT INTO `resource_menu` VALUES (10,'disasterBackup','灾备中心','disasterBackup',12,'icon-rongzaibeifen',0,NULL);
INSERT INTO `resource_menu` VALUES (11,'systemManagement','系统管理','systemManagement',13,'icon-shezhi01',0,NULL);
INSERT INTO `resource_menu` VALUES (12,'backupTask','备份任务','backupService/backupTask',101,'icon-fuwutiaokuan',10,NULL);
INSERT INTO `resource_menu` VALUES (13,'backupServer','备份服务器','backupService/backupServer',102,'icon-fuwutiaokuan',10,NULL);
INSERT INTO `resource_menu` VALUES (14,'dataMonitor','数据监控','monitorAlarm/dataMonitor',111,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (15,'logDetail','日志详情','monitorAlarm/logDetail',112,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (16,'alarmCenter','服务告警','monitorAlarm/alarmCenter',113,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (17,'resourcePoolManagement','集群管理','systemManagement/resourcePoolManagement',121,NULL,12,NULL);
INSERT INTO `resource_menu` VALUES (18,'userManagement','用户管理','systemManagement/userManagement',122,NULL,12,NULL);
INSERT INTO `resource_menu` VALUES (19,'organizationManagement','组织管理','systemManagement/organizationManagement',123,NULL,12,NULL);
INSERT INTO `resource_menu` VALUES (20,'roleManagement','角色管理','systemManagement/roleManagement',124,NULL,12,NULL);
INSERT INTO `resource_menu` VALUES (21,'systemAlarm','系统告警','systemManagement/systemAlarm',125,NULL,12,NULL);
INSERT INTO `resource_menu` VALUES (22,'operationAudit','操作审计','systemManagement/operationAudit',116,NULL,12,NULL);
INSERT INTO `resource_menu` VALUES (23,'organUserManagement','成员管理','organUserManagement',5,'icon-zuzhichengyuanguanli1',0,NULL);
INSERT INTO `resource_menu` VALUES (24,'myOrganizationManagement','组织管理','myOrganizationManagement',3,'icon-myOrganizationManagement',0,NULL);



-- 修改角色表列
alter table role change parent weight int null comment '权重';
update `role` set weight='1' where name='超级管理员';
update `role` set weight='2' where name='组织管理员';
update `role` set weight='3' where name='项目管理员';
update `role` set weight='4' where name='运维人员';
update `role` set weight='5' where name='普通用户';

-- 20230320 xutianhong
-- 初始化组织成员管理页面
INSERT INTO `resource_menu` VALUES (23,'organUserManagement','成员管理','organUserManagement',3,'icon-zuzhichengyuanguanli1',0,NULL);

-- 添加组织角色权限
INSERT INTO `resource_menu_role` VALUES (null,1,23,0);
INSERT INTO `resource_menu_role` VALUES (null,2,23,0);
INSERT INTO `resource_menu_role` VALUES (null,3,23,0);
INSERT INTO `resource_menu_role` VALUES (null,4,23,0);
INSERT INTO `resource_menu_role` VALUES (null,1,24,0);
INSERT INTO `resource_menu_role` VALUES (null,2,24,0);
INSERT INTO `resource_menu_role` VALUES (null,3,24,0);
INSERT INTO `resource_menu_role` VALUES (null,4,24,0);
INSERT INTO `resource_menu_role` VALUES (null,5,1,0);
INSERT INTO `resource_menu_role` VALUES (null,5,2,0);
INSERT INTO `resource_menu_role` VALUES (null,5,3,1);
INSERT INTO `resource_menu_role` VALUES (null,5,4,0);
INSERT INTO `resource_menu_role` VALUES (null,5,5,0);
INSERT INTO `resource_menu_role` VALUES (null,5,6,0);
INSERT INTO `resource_menu_role` VALUES (null,5,7,0);
INSERT INTO `resource_menu_role` VALUES (null,5,8,0);
INSERT INTO `resource_menu_role` VALUES (null,5,9,0);
INSERT INTO `resource_menu_role` VALUES (null,5,10,0);
INSERT INTO `resource_menu_role` VALUES (null,5,11,0);
INSERT INTO `resource_menu_role` VALUES (null,5,12,0);
INSERT INTO `resource_menu_role` VALUES (null,5,13,0);
INSERT INTO `resource_menu_role` VALUES (null,5,14,0);
INSERT INTO `resource_menu_role` VALUES (null,5,15,0);
INSERT INTO `resource_menu_role` VALUES (null,5,16,0);
INSERT INTO `resource_menu_role` VALUES (null,5,17,0);
INSERT INTO `resource_menu_role` VALUES (null,5,18,0);
INSERT INTO `resource_menu_role` VALUES (null,5,19,0);
INSERT INTO `resource_menu_role` VALUES (null,5,20,0);
INSERT INTO `resource_menu_role` VALUES (null,5,21,0);
INSERT INTO `resource_menu_role` VALUES (null,5,22,0);
INSERT INTO `resource_menu_role` VALUES (null,5,23,1);
INSERT INTO `resource_menu_role` VALUES (null,5,24,1);

--20230323 wangpenglei
--灾备中心更名为平台灾备
UPDATE resource_menu SET alias_name = '平台灾备' WHERE id = 10;

--20230328 wangpenglei
--system_config表config_value更改类型为text
ALTER TABLE system_config modify config_value text NULL;


