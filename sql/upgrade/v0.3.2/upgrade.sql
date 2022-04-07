-- 2022.3.23 yushuaikang
-- 个性化表新增tab栏图标自定义
ALTER TABLE `personal_config`
ADD COLUMN `tab_logo_path` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT 'tab栏logo地址' AFTER `home_logo_path`,
ADD COLUMN `tab_logo` mediumblob COMMENT 'tab栏logo' AFTER `home_logo_path`;

-- 2022.3.25 yushuaikang
-- 新增集群表
CREATE TABLE `middleware_cluster` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `clusterId` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '集群ID',
  `middleware_cluster` text COLLATE utf8_bin COMMENT '集群对象',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 2022.4.5 liyinlong
-- 新增mysql数据库表
CREATE TABLE `mysql_db` (
                            `id` int(11) NOT NULL AUTO_INCREMENT,
                            `mysql_qualified_name` varchar(512) NOT NULL COMMENT 'mysql服务限定名',
                            `db` char(64) NOT NULL COMMENT '数据库名',
                            `createtime` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
                            `description` varchar(512) DEFAULT NULL COMMENT '备注',
                            `charset` varchar(32) NOT NULL COMMENT '字符集',
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='mysql数据库';

-- 2022.4.5 liyinlong
-- 新增mysql用户表
CREATE TABLE `mysql_user` (
                              `id` int(11) NOT NULL AUTO_INCREMENT,
                              `mysql_qualified_name` varchar(512) NOT NULL COMMENT 'mysql服务限定名',
                              `user` char(32) NOT NULL COMMENT '用户名',
                              `password` varchar(255) NOT NULL COMMENT '密码',
                              `createtime` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
                              `description` varchar(512) DEFAULT NULL COMMENT '备注',
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='mysql用户';

-- 2022.4.5 liyinlong
-- 新增mysql数据库授权表
CREATE TABLE `mysql_db_priv` (
                                 `id` int(11) NOT NULL AUTO_INCREMENT,
                                 `mysql_qualified_name` varchar(512) NOT NULL COMMENT 'mysql服务限定名',
                                 `db` char(64) NOT NULL COMMENT '数据库名',
                                 `user` char(32) NOT NULL COMMENT '用户名',
                                 `authority` int(11) NOT NULL COMMENT '权限：1：只读，2：读写，3：仅DDL，4：仅DML',
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Mysql数据库授权';

-- 2022.4.5 xutianhong
-- 菜单
delete from `resource_menu`;
BEGIN;
INSERT INTO `resource_menu` VALUES (1, 'dataOverview', '数据总览', 'dataOverview', 1, 'icon-shujuzonglan', 0, NULL);
INSERT INTO `resource_menu` VALUES (2, 'middlewareRepository', '中间件市场', 'middlewareRepository', 2, 'icon-cangku', 0, NULL);
INSERT INTO `resource_menu` VALUES (3, 'myProject', '我的项目', 'myProject', 3, 'icon-cangku', 0, NULL);
INSERT INTO `resource_menu` VALUES (4, 'serviceList', '服务列表', 'serviceList', 4, 'icon-fuwuliebiao', 0, NULL);
INSERT INTO `resource_menu` VALUES (5, 'serviceAvailable', '服务暴露', 'serviceAvailable', 5, 'icon-fuwutiaokuan', 0, NULL);
INSERT INTO `resource_menu` VALUES (6, 'monitorAlarm', '监控告警', 'monitorAlarm', 6, 'icon-gaojingshijian', 0, NULL);
INSERT INTO `resource_menu` VALUES (7, 'disasterBackup', '容灾备份', 'disasterBackup', 7, 'icon-rongzaibeifen', 0, NULL);
INSERT INTO `resource_menu` VALUES (8, 'systemManagement', '系统管理', 'systemManagement', 8, 'icon-shezhi01', 0, NULL);
INSERT INTO `resource_menu` VALUES (9, 'dataMonitor', '数据监控', 'monitorAlarm/dataMonitor', 61, NULL, 6, NULL);
INSERT INTO `resource_menu` VALUES (10, 'logDetail', '日志详情', 'monitorAlarm/logDetail', 62, NULL, 6, NULL);
INSERT INTO `resource_menu` VALUES (11, 'alarmCenter', '服务告警', 'monitorAlarm/alarmCenter', 63, NULL, 6, NULL);
INSERT INTO `resource_menu` VALUES (12, 'disasterCenter', '灾备中心', 'disasterBackup/disasterCenter', 71, NULL, 7, NULL);
INSERT INTO `resource_menu` VALUES (13, 'dataSecurity', '数据安全', 'disasterBackup/dataSecurity', 72, NULL, 7, NULL);
INSERT INTO `resource_menu` VALUES (14, 'resourcePoolManagement', '集群管理', 'systemManagement/resourcePoolManagement', 81, NULL, 8, NULL);
INSERT INTO `resource_menu` VALUES (15, 'userManagement', '用户管理', 'systemManagement/userManagement', 82, NULL, 8, NULL);
INSERT INTO `resource_menu` VALUES (16, 'projectManagement', '项目管理', 'systemManagement/projectManagement', 83, NULL, 8, NULL);
INSERT INTO `resource_menu` VALUES (17, 'roleManagement', '角色管理', 'systemManagement/roleManagement', 84, NULL, 8, NULL);
INSERT INTO `resource_menu` VALUES (18, 'systemAlarm', '系统告警', 'systemManagement/systemAlarm', 85, NULL, 8, NULL);
INSERT INTO `resource_menu` VALUES (19, 'operationAudit', '操作审计', 'systemManagement/operationAudit', 86, NULL, 8, NULL);
COMMIT;

-- 角色
-- 删除除admin以外的所有角色
DELETE from role WHERE id != '1';
-- 添加新的内置角色
insert into role values(2, '项目管理员', '拥有项目管理权限', null, null, null);
insert into role values(3, '运维人员', '拥有中间件运维权限', null, null, null);
insert into role values(4, '普通用户', '拥有平台查看权限', null, null, null);
-- 修改user_role 添加字段
alter table role_user add COLUMN `project_id` varchar(64) collate utf8mb4_general_ci DEFAULT null comment '项目ID' AFTER `id`;
-- 创建角色权限对应表
DROP TABLE IF EXISTS `role_authority`;
CREATE TABLE `role_authority` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `role_id` int(11) DEFAULT NULL COMMENT '角色id',
  `type` varchar(128) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '中间件类型',
  `power` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '能力:查\\增\\删\\运维',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- 初始化角色权限
BEGIN;
INSERT INTO `role_authority` VALUES (1, 1, 'mysql', '1111');
INSERT INTO `role_authority` VALUES (2, 1, 'redis', '1111');
INSERT INTO `role_authority` VALUES (3, 1, 'elasticsearch', '1111');
INSERT INTO `role_authority` VALUES (4, 1, 'rocketmq', '1111');
INSERT INTO `role_authority` VALUES (5, 1, 'zookeeper', '1111');
INSERT INTO `role_authority` VALUES (6, 1, 'kafka', '1111');
INSERT INTO `role_authority` VALUES (7, 2, 'mysql', '1111');
INSERT INTO `role_authority` VALUES (8, 2, 'redis', '1111');
INSERT INTO `role_authority` VALUES (9, 2, 'elasticsearch', '1111');
INSERT INTO `role_authority` VALUES (10, 2, 'rocketmq', '1111');
INSERT INTO `role_authority` VALUES (11, 2, 'zookeeper', '1111');
INSERT INTO `role_authority` VALUES (12, 2, 'kafka', '1111');
INSERT INTO `role_authority` VALUES (13, 3, 'mysql', '1111');
INSERT INTO `role_authority` VALUES (14, 3, 'redis', '1111');
INSERT INTO `role_authority` VALUES (15, 3, 'elasticsearch', '1111');
INSERT INTO `role_authority` VALUES (16, 3, 'rocketmq', '1111');
INSERT INTO `role_authority` VALUES (17, 3, 'zookeeper', '1111');
INSERT INTO `role_authority` VALUES (18, 3, 'kafka', '1111');
INSERT INTO `role_authority` VALUES (19, 4, 'mysql', '1000');
INSERT INTO `role_authority` VALUES (20, 4, 'redis', '1000');
INSERT INTO `role_authority` VALUES (21, 4, 'elasticsearch', '1000');
INSERT INTO `role_authority` VALUES (22, 4, 'rocketmq', '1000');
INSERT INTO `role_authority` VALUES (23, 4, 'zookeeper', '1000');
INSERT INTO `role_authority` VALUES (24, 4, 'kafka', '1000');
COMMIT;

-- project
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `project_id` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '项目id',
  `name` varchar(128) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '项目名称',
  `alias_name` varchar(128) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '项目别名',
  `description` text CHARACTER SET utf8mb4 COMMENT '项目描述',
  `user` text CHARACTER SET utf8mb4 COMMENT '用户',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

DROP TABLE IF EXISTS `project_namespace`;
CREATE TABLE `project_namespace` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `project_id` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '项目id',
  `namespace` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '分区',
  `alias_name` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '分区别名',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '集群id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
