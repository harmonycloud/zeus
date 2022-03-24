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
  `lay` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警层面',
  `expr` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则描述',
  `alert_id` int(16) DEFAULT NULL COMMENT '规则ID',
  `content` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警内容',
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
-- Table structure for cache_middleware
-- ----------------------------
DROP TABLE IF EXISTS `cache_middleware`;
CREATE TABLE `cache_middleware` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `namespace` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分区',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件名称',
  `type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件类型',
  `chart_version` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件版本',
  `pvc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'pvc列表',
  `values_yaml` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='已删除中间件信息缓存表';

-- ----------------------------
-- Table structure for alert_rule_id
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule_id`;
CREATE TABLE `alert_rule_id` (
  `alert_id` int NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `cluster_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '集群ID',
  `namespace` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '命名空间',
  `middleware_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '中间件名称',
  `alert` text CHARACTER SET utf8 COLLATE utf8_bin COMMENT '规则名称',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `silence` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '沉默时间',
  `symbol` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '符号',
  `threshold` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '阈值',
  `time` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '分钟周期',
  `type` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '中间件类型',
  `unit` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '单位',
  `expr` varchar(512) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '执行规则',
  `description` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '监控项',
  `annotations` text CHARACTER SET utf8 COLLATE utf8_bin COMMENT '备注',
  `labels` text CHARACTER SET utf8 COLLATE utf8_bin COMMENT '标签',
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `alert_time` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '告警时间',
  `alert_times` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '告警次数',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `status` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '状态',
  `enable` int DEFAULT NULL COMMENT '是否启用',
  `content` varchar(512) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '告警内容',
  `lay` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT 'system 系统告警 service 服务告警',
  `ding` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '是否选择钉钉通知',
  `mail` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '是否选择邮箱通知',
  `alert_expr` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '告警规则',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ip地址',
  PRIMARY KEY (`alert_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Table structure for cluster_components
-- ----------------------------
DROP TABLE IF EXISTS `cluster_components`;
CREATE TABLE `cluster_components` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群Id',
  `component` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件名称',
  `status` int DEFAULT NULL COMMENT '0-未安装接入 1-已接入 2-安装中 3-运行正常 4-运行异常 5-卸载中',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='集群组件表';

-- ----------------------------
-- Table structure for cluster_ingress_components
-- ----------------------------
DROP TABLE IF EXISTS `cluster_ingress_components`;
CREATE TABLE `cluster_ingress_components` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ingress name',
  `ingress_class_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ingress class name',
  `cluster_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `status` int DEFAULT NULL COMMENT '状态',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='集群ingress组件表';

-- ----------------------------
-- Table structure for cluster_middleware_info
-- ----------------------------
DROP TABLE IF EXISTS `cluster_middleware_info`;
CREATE TABLE `cluster_middleware_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `chart_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包名称',
  `chart_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包版本',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'operator状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='集群-中间件关联表';

-- ----------------------------
-- Table structure for cluster_role
-- ----------------------------
DROP TABLE IF EXISTS `cluster_role`;
CREATE TABLE `cluster_role` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `role_id` int DEFAULT NULL COMMENT '角色id',
  `cluster_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `namespace` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分区',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='角色集群权限表';

-- ----------------------------
-- Table structure for custom_config
-- ----------------------------
DROP TABLE IF EXISTS `custom_config`;
CREATE TABLE `custom_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段名称',
  `chart_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包名称',
  `default_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '默认值',
  `restart` tinyint(1) DEFAULT NULL COMMENT '是否重启',
  `ranges` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '阈值',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
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
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中间件名称',
  `item` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置名称',
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
  `uid` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板uid',
  `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中间件类型',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '模板描述',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置内容',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义配置模板表';

-- ----------------------------
-- Table structure for ding_robot_info
-- ----------------------------
DROP TABLE IF EXISTS `ding_robot_info`;
CREATE TABLE `ding_robot_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `webhook` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `secret_key` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '加签密钥',
  `enable_ding` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '是否启用该钉钉机器人 1 启用 0 否',
  `creat_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Table structure for k8s_default_cluster
-- ----------------------------
DROP TABLE IF EXISTS `k8s_default_cluster`;
CREATE TABLE `k8s_default_cluster` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `url` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路径',
  `token` text CHARACTER SET utf8 COLLATE utf8_bin COMMENT 'service account',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Table structure for kube_config
-- ----------------------------
DROP TABLE IF EXISTS `kube_config`;
CREATE TABLE `kube_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `conf` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'admin.conf',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Table structure for mail_info
-- ----------------------------
DROP TABLE IF EXISTS `mail_info`;
CREATE TABLE `mail_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `mail_server` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '邮箱服务器',
  `port` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '端口',
  `username` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '用户',
  `password` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '密码',
  `mail_path` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '邮箱地址',
  `creat_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Table structure for mail_to_user
-- ----------------------------
DROP TABLE IF EXISTS `mail_to_user`;
CREATE TABLE `mail_to_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `alert_rule_id` int DEFAULT NULL COMMENT '规则ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Table structure for middleware_info
-- ----------------------------
DROP TABLE IF EXISTS `middleware_info`;
CREATE TABLE `middleware_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中间件名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型',
  `version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版本',
  `image` mediumblob COMMENT '图片',
  `image_path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片地址',
  `chart_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包名称',
  `chart_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart版本',
  `grafana_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'grafana的id',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modifier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `official` tinyint(1) DEFAULT NULL COMMENT '官方中间件',
  `chart` mediumblob COMMENT 'helm chart包',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'operator名称',
  `compatible_versions` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '升级所需最低版本',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='中间件表';

-- ----------------------------
-- Table structure for operation_audit
-- ----------------------------
DROP TABLE IF EXISTS `operation_audit`;
CREATE TABLE `operation_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '请求参数',
  `response` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '响应内容',
  `remote_ip` char(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求ip',
  `status` char(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态码',
  `begin_time` datetime NOT NULL COMMENT '请求开始时间',
  `action_time` datetime NOT NULL COMMENT '请求响应时间',
  `execute_time` int NOT NULL COMMENT '执行时长(ms)',
  `cluster_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求token',
  PRIMARY KEY (`id`),
  KEY `index_user_name` (`user_name`) USING BTREE,
  KEY `index_account` (`account`) USING BTREE,
  KEY `index_url` (`url`(32)) USING BTREE,
  KEY `index_remote_ip` (`remote_ip`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作审计';

-- ----------------------------
-- Table structure for personal_config
-- ----------------------------
DROP TABLE IF EXISTS `personal_config`;
CREATE TABLE `personal_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `background_image` mediumblob COMMENT '背景图',
  `background_image_path` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '背景图地址',
  `login_logo` mediumblob COMMENT '登录页logo',
  `login_logo_path` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '登录页logo地址',
  `home_logo` mediumblob COMMENT '主页logo',
  `home_logo_path` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '主页logo地址',
  `tab_logo` mediumblob COMMENT 'tab栏logo',
  `tab_logo_path` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT 'tab栏logo地址',
  `platform_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '平台名称',
  `slogan` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '标语',
  `copyright_notice` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '版权声明',
  `title` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '浏览器标题',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `status` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '是否默认',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Table structure for resource_menu
-- ----------------------------
DROP TABLE IF EXISTS `resource_menu`;
CREATE TABLE `resource_menu` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `alias_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中文名称',
  `url` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路径',
  `weight` int DEFAULT NULL COMMENT '权重(排序使用)',
  `icon_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT 'icon名称',
  `parent_id` int DEFAULT NULL COMMENT '父菜单id',
  `module` varchar(0) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模块',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='菜单资源表';

-- ----------------------------
-- Table structure for image_repository
-- ----------------------------
DROP TABLE IF EXISTS `image_repository`;
CREATE TABLE `image_repository` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cluster_id` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '集群',
  `protocol` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '协议',
  `address` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'harbor地址',
  `host_address` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT 'harbor主机地址',
  `port` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '端口',
  `project` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'harbor项目',
  `username` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '用户名',
  `password` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '密码',
  `description` varchar(512) COLLATE utf8_bin DEFAULT NULL COMMENT '描述',
  `is_default` int(11) COLLATE utf8_bin DEFAULT NULL COMMENT '是否默认',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='镜像仓库表';

-- ----------------------------
-- Records of resource_menu
-- ----------------------------
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
INSERT INTO `resource_menu` VALUES (10, 'alarmCenter', '服务告警', 'monitorAlarm/alarmCenter', 53, NULL, 5, NULL);
INSERT INTO `resource_menu` VALUES (11, 'disasterCenter', '灾备中心', 'disasterBackup/disasterCenter', 61, NULL, 6, NULL);
INSERT INTO `resource_menu` VALUES (12, 'dataSecurity', '数据安全', 'disasterBackup/dataSecurity', 62, NULL, 6, NULL);
INSERT INTO `resource_menu` VALUES (13, 'resourcePoolManagement', '资源池', 'systemManagement/resourcePoolManagement', 71, NULL, 7, NULL);
INSERT INTO `resource_menu` VALUES (14, 'userManagement', '用户管理', 'systemManagement/userManagement', 72, NULL, 7, NULL);
INSERT INTO `resource_menu` VALUES (15, 'roleManagement', '角色管理', 'systemManagement/roleManagement', 73, NULL, 7, NULL);
INSERT INTO `resource_menu` VALUES (16, 'systemAlarm', '系统告警', 'systemManagement/systemAlarm', 74, NULL, 7, NULL);
INSERT INTO `resource_menu` VALUES (17, 'operationAudit', '操作审计', 'systemManagement/operationAudit', 75, NULL, 7, NULL);
COMMIT;

-- ----------------------------
-- Table structure for resource_menu_role
-- ----------------------------
DROP TABLE IF EXISTS `resource_menu_role`;
CREATE TABLE `resource_menu_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int DEFAULT NULL,
  `resource_menu_id` int DEFAULT NULL,
  `available` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='资源菜单角色关联表';

-- ----------------------------
-- Records of resource_menu_role
-- ----------------------------
BEGIN;
INSERT INTO `resource_menu_role` VALUES (1, 1, 1, 1);
INSERT INTO `resource_menu_role` VALUES (2, 1, 2, 1);
INSERT INTO `resource_menu_role` VALUES (3, 1, 3, 1);
INSERT INTO `resource_menu_role` VALUES (4, 1, 4, 1);
INSERT INTO `resource_menu_role` VALUES (5, 1, 5, 1);
INSERT INTO `resource_menu_role` VALUES (6, 1, 6, 1);
INSERT INTO `resource_menu_role` VALUES (7, 1, 7, 1);
INSERT INTO `resource_menu_role` VALUES (8, 1, 8, 1);
INSERT INTO `resource_menu_role` VALUES (9, 1, 9, 1);
INSERT INTO `resource_menu_role` VALUES (10, 1, 10, 1);
INSERT INTO `resource_menu_role` VALUES (11, 1, 11, 1);
INSERT INTO `resource_menu_role` VALUES (12, 1, 12, 1);
INSERT INTO `resource_menu_role` VALUES (13, 1, 13, 1);
INSERT INTO `resource_menu_role` VALUES (14, 1, 14, 1);
INSERT INTO `resource_menu_role` VALUES (15, 1, 15, 1);
INSERT INTO `resource_menu_role` VALUES (16, 1, 16, 1);
INSERT INTO `resource_menu_role` VALUES (17, 1, 17, 1);
COMMIT;

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `parent` int DEFAULT NULL COMMENT '父角色id',
  `status` tinyint(1) DEFAULT NULL COMMENT '是否已被删除',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='角色表';

-- ----------------------------
-- Records of role
-- ----------------------------
BEGIN;
INSERT INTO `role` VALUES (1, '超级管理员', '拥有所有最高权限', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for role_user
-- ----------------------------
DROP TABLE IF EXISTS `role_user`;
CREATE TABLE `role_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `role_id` int DEFAULT NULL COMMENT '角色id',
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
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `alias_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户别名',
  `password` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `password_time` timestamp NULL DEFAULT NULL COMMENT '密码修改时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建者',
  PRIMARY KEY (`id`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='用户表';

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO `user` VALUES (1, 'admin', '超级管理员', '6DA05F9A0ED31ABEEFD41C768B2E7233', NULL, NULL, NULL, NULL, NULL);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Table structure for ldap_config
-- ----------------------------
CREATE TABLE `ldap_config` (
                               `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
                               `config_name` varchar(64) NOT NULL COMMENT '配置名',
                               `config_value` varchar(2048) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
                               `create_user` varchar(64) DEFAULT NULL COMMENT '创建人',
                               `update_user` varchar(64) DEFAULT NULL COMMENT '修改人',
                               `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                               PRIMARY KEY (`id`) USING BTREE,
                               UNIQUE KEY `config_name` (`config_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='ldap配置表';