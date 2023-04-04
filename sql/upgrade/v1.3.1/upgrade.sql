-- 20230404 xutianhong
-- 同步v1.2.6相关语句
-- 删除备份服务器地址表
DROP TABLE IF EXISTS `middleware_backup_address`;
-- 修改backup_name表，添加备份位置id
ALTER TABLE `backup_name` ADD COLUMN `position_id` INT(11) COMMENT '备份位置id';
-- 新增表
DROP TABLE IF EXISTS `backup_server_detail`;
CREATE TABLE `backup_server_detail` (
    `id`               int(11) NOT NULL AUTO_INCREMENT,
    `backup_server_id` int(11) NOT NULL COMMENT '备份服务器id',
    `server_usage`     varchar(32) COLLATE utf8_bin  DEFAULT NULL COMMENT '用途：A可用区A,B:可用区B',
    `type`             int(11)                       DEFAULT NULL COMMENT '类型：1: S3. 2: ftp: 3: server',
    `protocol`         varchar(45) COLLATE utf8_bin  DEFAULT NULL COMMENT '协议',
    `host`             varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT '主机',
    `port`             varchar(45) COLLATE utf8_bin  DEFAULT NULL COMMENT '端口',
    `username`         varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT '用户名',
    `password`         varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT '密码',
    `create_time`      varchar(45) COLLATE utf8_bin  DEFAULT NULL COMMENT '创建时间',
PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 17 DEFAULT CHARSET = utf8 COLLATE = utf8_bin COMMENT ='备份服务器详情';
-- 新增表
DROP TABLE IF EXISTS `backup_server`;
CREATE TABLE `backup_server`(
    `id`          int(11)                       NOT NULL AUTO_INCREMENT,
    `name`        varchar(512) COLLATE utf8_bin NOT NULL COMMENT '备份服务器名称',
    `cluster_id`  varchar(256) COLLATE utf8_bin      DEFAULT NULL COMMENT '所属集群',
    `type`        int(11)                       NOT NULL COMMENT '备份服务器类型（1:普通，2:双活）',
    `create_time` timestamp                     NULL DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 13 DEFAULT CHARSET = utf8 COLLATE = utf8_bin COMMENT ='备份服务器';
-- 新增表
DROP TABLE IF EXISTS `project_backup_server`;
CREATE TABLE `project_backup_server`(
    `id`               int(11)                       NOT NULL AUTO_INCREMENT,
    `project_id`       varchar(128) COLLATE utf8_bin NOT NULL COMMENT '项目id',
    `backup_server_id` int(11)                       NOT NULL COMMENT '备份服务器id',
PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 9 DEFAULT CHARSET = utf8 COLLATE = utf8_bin COMMENT ='项目备份服务器关联表';
-- 新增表
DROP TABLE IF EXISTS `backup_position`;
CREATE TABLE `backup_position`(
    `id`               int(11)                       NOT NULL AUTO_INCREMENT,
    `name`             varchar(512) COLLATE utf8_bin NOT NULL COMMENT '备份位置名称',
    `project_id`       varchar(128) COLLATE utf8_bin NOT NULL COMMENT '项目id',
    `backup_server_id` int(11)                       NOT NULL COMMENT '备份服务器id',
    `backup_position`  varchar(512) COLLATE utf8_bin NOT NULL COMMENT '备份路径（对于minio则是bucket）',
    `create_time`      timestamp                     NULL DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 5 DEFAULT CHARSET = utf8 COLLATE = utf8_bin COMMENT ='备份位置表';


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
INSERT INTO `resource_menu` VALUES (10,'disasterBackup','平台灾备','disasterBackup',12,'icon-rongzaibeifen',0,NULL);
INSERT INTO `resource_menu` VALUES (11,'systemManagement','系统管理','systemManagement',13,'icon-shezhi01',0,NULL);
INSERT INTO `resource_menu` VALUES (12,'backupTask','备份任务','backupService/backupTask',101,'icon-fuwutiaokuan',10,NULL);
INSERT INTO `resource_menu` VALUES (13,'backupServer','备份服务器','backupService/backupServer',102,'icon-fuwutiaokuan',10,NULL);
INSERT INTO `resource_menu` VALUES (14,'dataMonitor','数据监控','monitorAlarm/dataMonitor',111,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (15,'logDetail','日志详情','monitorAlarm/logDetail',112,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (16,'alarmCenter','服务告警','monitorAlarm/alarmCenter',113,NULL,11,NULL);
INSERT INTO `resource_menu` VALUES (17,'resourcePoolManagement','集群管理','systemManagement/resourcePoolManagement',131,NULL,13,NULL);
INSERT INTO `resource_menu` VALUES (18,'userManagement','用户管理','systemManagement/userManagement',132,NULL,13,NULL);
INSERT INTO `resource_menu` VALUES (19,'organizationManagement','组织管理','systemManagement/organizationManagement',133,NULL,13,NULL);
INSERT INTO `resource_menu` VALUES (20,'roleManagement','角色管理','systemManagement/roleManagement',134,NULL,13,NULL);
INSERT INTO `resource_menu` VALUES (21,'systemAlarm','系统告警','systemManagement/systemAlarm',135,NULL,13,NULL);
INSERT INTO `resource_menu` VALUES (22,'operationAudit','操作审计','systemManagement/operationAudit',136,NULL,13,NULL);
INSERT INTO `resource_menu` VALUES (23,'organUserManagement','成员管理','organUserManagement',5,'icon-zuzhichengyuanguanli1',0,NULL);
INSERT INTO `resource_menu` VALUES (24,'myOrganizationManagement','组织概览','myOrganizationManagement',3,'icon-shuxiangjiegou',0,NULL);



-- 修改角色表列
alter table role change parent weight int null comment '权重';
update `role` set weight='1' where name='超级管理员';
update `role` set weight='2' where name='组织管理员';
update `role` set weight='3' where name='项目管理员';
update `role` set weight='4' where name='运维人员';
update `role` set weight='5' where name='普通用户';

-- 20230320 xutianhong
-- 添加组织角色权限
select id INTO @roleid from role where weight = '2';
INSERT INTO `resource_menu_role` VALUES (null,1,23,0);
INSERT INTO `resource_menu_role` VALUES (null,2,23,0);
INSERT INTO `resource_menu_role` VALUES (null,3,23,0);
INSERT INTO `resource_menu_role` VALUES (null,4,23,0);
INSERT INTO `resource_menu_role` VALUES (null,1,24,0);
INSERT INTO `resource_menu_role` VALUES (null,2,24,0);
INSERT INTO `resource_menu_role` VALUES (null,3,24,0);
INSERT INTO `resource_menu_role` VALUES (null,4,24,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,1,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,2,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,3,1);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,4,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,5,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,5,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,6,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,7,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,8,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,9,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,10,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,11,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,12,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,13,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,14,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,15,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,16,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,17,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,18,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,19,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,20,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,21,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,22,0);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,23,1);
INSERT INTO `resource_menu_role` VALUES (null,@roleid,24,1);

-- 20230328 wangpenglei
-- system_config表config_value更改类型为text
ALTER TABLE system_config modify config_value text NULL;


