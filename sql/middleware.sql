DROP DATABASE IF EXISTS `middleware_platform`;
CREATE DATABASE `middleware_platform`
default character set utf8
default collate utf8_bin;

-- 如果没有middleware数据库，则需要先创建
-- CREATE DATABASE `middleware_platform`
-- default character set utf8mb4
-- default collate utf8mb4_general_ci;

USE `middleware_platform`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for alert_record
-- ----------------------------
DROP TABLE IF EXISTS `alert_record`;
CREATE TABLE `alert_record` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `namespace` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分区',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件名称',
  `alert` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警名称',
  `summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '简讯',
  `message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警信息',
  `level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警等级',
  `time` timestamp NULL DEFAULT NULL COMMENT '告警时间',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警记录表';

-- ----------------------------
-- Table structure for alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `chart_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型名称',
  `chart_version` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版本',
  `alert` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'prometheusRule内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警规则表';

-- ----------------------------
-- Table structure for custom_config
-- ----------------------------
DROP TABLE IF EXISTS `custom_config`;
CREATE TABLE `custom_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段名称',
  `chart_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包名称',
  `default_value` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '默认值',
  `restart` tinyint(1) DEFAULT NULL COMMENT '是否重启',
  `ranges` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '阈值',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `chart_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart版本',
  `pattern` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '正则校验',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义配置表';

-- ----------------------------
-- Table structure for custom_config_history
-- ----------------------------
DROP TABLE IF EXISTS `custom_config_history`;
CREATE TABLE `custom_config_history` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '集群id',
  `namespace` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分区',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中间件名称',
  `item` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置名称',
  `last` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改前',
  `after` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改后',
  `restart` tinyint(1) DEFAULT NULL COMMENT '是否需要重启',
  `status` tinyint(1) DEFAULT NULL COMMENT '是否已启用',
  `date` timestamp NULL DEFAULT NULL COMMENT '日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义配置修改历史表';

-- ----------------------------
-- Table structure for custom_config_template
-- ----------------------------
DROP TABLE IF EXISTS `custom_config_template`;
CREATE TABLE `custom_config_template` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `alias_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模板中文名',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件类型',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义配置模板表';

-- ----------------------------
-- Table structure for middleware_info
-- ----------------------------
DROP TABLE IF EXISTS `middleware_info`;
CREATE TABLE `middleware_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中间件名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型',
  `version` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版本',
  `image` mediumblob COMMENT '图片',
  `image_path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片地址',
  `status` tinyint(1) DEFAULT '1' COMMENT '是否可用：0-不可用 1-可用',
  `chart_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包名称',
  `chart_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart版本',
  `grafana_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'grafana的id',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `official` tinyint(1) DEFAULT NULL COMMENT '官方中间件',
  `compatible_versions` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '升级所需最低版本',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='中间件表';

-- ----------------------------
-- Table structure for operation_audit
-- ----------------------------
DROP TABLE IF EXISTS `operation_audit`;
CREATE TABLE `operation_audit` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `account` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '账户名称',
  `user_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名称',
  `role_name` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色名称',
  `phone` char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'url',
  `module_ch_desc` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模块名称',
  `child_module_ch_desc` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '子模块名称',
  `action_ch_desc` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作名称',
  `method` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法',
  `request_method` char(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求方法类型',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求参数',
  `response` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '响应内容',
  `remote_ip` char(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求ip',
  `status` char(8) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态码',
  `begin_time` datetime NOT NULL COMMENT '请求开始时间',
  `action_time` datetime NOT NULL COMMENT '请求响应时间',
  `execute_time` int(11) NOT NULL COMMENT '执行时长(ms)',
  `cluster_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求token',
  PRIMARY KEY (`id`),
  KEY `index_user_name` (`user_name`) USING BTREE,
  KEY `index_account` (`account`) USING BTREE,
  KEY `index_url` (`url`(32)) USING BTREE,
  KEY `index_remote_ip` (`remote_ip`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作审计';

-- ----------------------------
-- Table structure for resource_menu
-- ----------------------------
DROP TABLE IF EXISTS `resource_menu`;
CREATE TABLE `resource_menu` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `alias_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中文名称',
  `url` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路径',
  `weight` int(11) DEFAULT NULL COMMENT '权重(排序使用)',
  `icon_name` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'icon名称',
  `parent_id` int(11) DEFAULT NULL COMMENT '父菜单id',
  `module` varchar(0) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模块',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='菜单资源表';

-- ----------------------------
-- Records of resource_menu
-- ----------------------------
BEGIN;
INSERT INTO `resource_menu` VALUES (1, 'workbench', '工作台', 'workbench', 1, 'icon-gongzuotai', 0, NULL);
INSERT INTO `resource_menu` VALUES (2, 'operations', '平台运维', 'operations', 2, 'icon-pingtaiyunwei', 0, NULL);
INSERT INTO `resource_menu` VALUES (3, 'management', '系统管理', 'management', 3, 'icon-guanlizhongxin', 0, NULL);
INSERT INTO `resource_menu` VALUES (4, 'spaceOverview', '空间概览', 'spaceOverview', 11, NULL, 1, NULL);
INSERT INTO `resource_menu` VALUES (5, 'instanceList', '实例列表', 'instanceList', 13, NULL, 1, NULL);
INSERT INTO `resource_menu` VALUES (6, 'outboundRoute', '对外路由', 'outboundRoute', 14, NULL, 1, NULL);
INSERT INTO `resource_menu` VALUES (7, 'platformOverview', '资源总览', 'platformOverview', 21, NULL, 2, NULL);
INSERT INTO `resource_menu` VALUES (8, 'basicResource', '基础资源', 'basicResource', 31, NULL, 3, NULL);
INSERT INTO `resource_menu` VALUES (10, 'operationAudit', '操作审计', 'operationAudit', 33, NULL, 3, NULL);
INSERT INTO `resource_menu` VALUES (11, 'userManage', '用户管理', 'userManage', 34, NULL, 3, NULL);
INSERT INTO `resource_menu` VALUES (12, 'serviceCatalog', '服务目录', 'serviceCatalog', 12, NULL, 1, NULL);
COMMIT;

-- ----------------------------
-- Table structure for resource_menu_role
-- ----------------------------
DROP TABLE IF EXISTS `resource_menu_role`;
CREATE TABLE `resource_menu_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `role_id` int(11) DEFAULT NULL,
  `resource_menu_id` int(11) DEFAULT NULL,
  `available` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='资源菜单角色关联表';

-- ----------------------------
-- Records of resource_menu_role
-- ----------------------------
BEGIN;
INSERT INTO `resource_menu_role` VALUES (12, 1, 1, 1);
INSERT INTO `resource_menu_role` VALUES (13, 1, 2, 1);
INSERT INTO `resource_menu_role` VALUES (14, 1, 3, 1);
INSERT INTO `resource_menu_role` VALUES (15, 1, 4, 1);
INSERT INTO `resource_menu_role` VALUES (16, 1, 5, 1);
INSERT INTO `resource_menu_role` VALUES (17, 1, 6, 1);
INSERT INTO `resource_menu_role` VALUES (18, 1, 7, 1);
INSERT INTO `resource_menu_role` VALUES (19, 1, 8, 1);
INSERT INTO `resource_menu_role` VALUES (20, 1, 9, 1);
INSERT INTO `resource_menu_role` VALUES (21, 1, 10, 1);
INSERT INTO `resource_menu_role` VALUES (22, 1, 11, 1);
INSERT INTO `resource_menu_role` VALUES (23, 2, 1, 1);
INSERT INTO `resource_menu_role` VALUES (24, 2, 2, 1);
INSERT INTO `resource_menu_role` VALUES (25, 2, 3, 1);
INSERT INTO `resource_menu_role` VALUES (26, 2, 4, 1);
INSERT INTO `resource_menu_role` VALUES (27, 2, 5, 1);
INSERT INTO `resource_menu_role` VALUES (28, 2, 6, 1);
INSERT INTO `resource_menu_role` VALUES (29, 2, 7, 1);
INSERT INTO `resource_menu_role` VALUES (30, 2, 8, 1);
INSERT INTO `resource_menu_role` VALUES (31, 2, 9, 1);
INSERT INTO `resource_menu_role` VALUES (32, 2, 10, 1);
INSERT INTO `resource_menu_role` VALUES (33, 2, 11, 1);
INSERT INTO `resource_menu_role` VALUES (34, 3, 1, 1);
INSERT INTO `resource_menu_role` VALUES (35, 3, 2, 1);
INSERT INTO `resource_menu_role` VALUES (36, 3, 3, 0);
INSERT INTO `resource_menu_role` VALUES (37, 3, 4, 1);
INSERT INTO `resource_menu_role` VALUES (38, 3, 5, 1);
INSERT INTO `resource_menu_role` VALUES (39, 3, 6, 1);
INSERT INTO `resource_menu_role` VALUES (40, 3, 7, 1);
INSERT INTO `resource_menu_role` VALUES (41, 3, 8, 0);
INSERT INTO `resource_menu_role` VALUES (42, 3, 9, 0);
INSERT INTO `resource_menu_role` VALUES (43, 3, 10, 0);
INSERT INTO `resource_menu_role` VALUES (44, 3, 11, 0);
INSERT INTO `resource_menu_role` VALUES (45, 4, 1, 1);
INSERT INTO `resource_menu_role` VALUES (46, 4, 2, 0);
INSERT INTO `resource_menu_role` VALUES (47, 4, 3, 0);
INSERT INTO `resource_menu_role` VALUES (48, 4, 4, 1);
INSERT INTO `resource_menu_role` VALUES (49, 4, 5, 1);
INSERT INTO `resource_menu_role` VALUES (50, 4, 6, 1);
INSERT INTO `resource_menu_role` VALUES (51, 4, 7, 0);
INSERT INTO `resource_menu_role` VALUES (52, 4, 8, 0);
INSERT INTO `resource_menu_role` VALUES (53, 4, 9, 0);
INSERT INTO `resource_menu_role` VALUES (54, 4, 10, 0);
INSERT INTO `resource_menu_role` VALUES (55, 4, 11, 0);
INSERT INTO `resource_menu_role` VALUES (56, 1, 12, 1);
INSERT INTO `resource_menu_role` VALUES (57, 2, 12, 1);
INSERT INTO `resource_menu_role` VALUES (58, 3, 12, 1);
INSERT INTO `resource_menu_role` VALUES (59, 4, 12, 1);
COMMIT;

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` int(64) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT NULL COMMENT '是否已被删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='角色表';

-- ----------------------------
-- Records of role
-- ----------------------------
BEGIN;
INSERT INTO `role` VALUES (1, 'admin', '超级管理员', NULL);
INSERT INTO `role` VALUES (2, '系统管理员', '系统最高权限账户', NULL);
INSERT INTO `role` VALUES (3, '运维管理员', '负责平台运维', NULL);
INSERT INTO `role` VALUES (4, '平台管理员', '负责具体中间件业务相关功能版块操作', NULL);
COMMIT;

-- ----------------------------
-- Table structure for role_user
-- ----------------------------
DROP TABLE IF EXISTS `role_user`;
CREATE TABLE `role_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `role_id` int(11) DEFAULT NULL COMMENT '角色id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='用户角色关联表';

-- ----------------------------
-- Records of role_user
-- ----------------------------
BEGIN;
INSERT INTO `role_user` VALUES (1, 'admin', 1);
COMMIT;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `alias_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户别名',
  `password` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='用户表';

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO `user` VALUES (1, 'admin', 'admin', '5B99164F828AED74140E5FDA077B634C', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for k8s_default_cluster
-- ----------------------------
DROP TABLE IF EXISTS `k8s_default_cluster`;
CREATE TABLE `k8s_default_cluster` (
  `id` int(2) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `url` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路径',
  `token` text CHARACTER SET utf8 COLLATE utf8_bin COMMENT 'service account',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


SET FOREIGN_KEY_CHECKS = 1;
