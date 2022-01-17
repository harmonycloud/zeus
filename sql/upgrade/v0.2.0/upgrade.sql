-- 2021.11.12 xutianhong
-- 新增集群组件表
DROP TABLE IF EXISTS `cluster_components`;
CREATE TABLE `cluster_components` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '集群Id',
  `component` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '组件名称',
  `status` int(11) NULL COMMENT '0-未安装接入 1-已接入 2-安装中 3-运行正常 4-运行异常 5-卸载中',
	PRIMARY KEY (`id`)
) COMMENT = '集群组件表';

-- 2021.11.15 yushuaikang
-- 新增个性化配置表
DROP TABLE IF EXISTS `personal_config`;
CREATE TABLE `personal_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `background_image` mediumblob COMMENT '背景图',
  `background_image_path` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '背景图地址',
  `login_logo` mediumblob COMMENT '登录页logo',
  `login_logo_path` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '登录页logo地址',
  `home_logo` mediumblob COMMENT '主页logo',
  `home_logo_path` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '主页logo地址',
  `platform_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '平台名称',
  `slogan` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '标语',
  `copyright_notice` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '版权声明',
  `title` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '浏览器标题',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `status` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '是否默认',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 新增邮箱信息表
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

-- 新增被通知人表
DROP TABLE IF EXISTS `mail_to_user`;
CREATE TABLE `mail_to_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `alert_rule_id` int DEFAULT NULL COMMENT '规则ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 新增钉钉机器人表
DROP TABLE IF EXISTS `ding_robot_info`;
CREATE TABLE `ding_robot_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `webhook` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `secret_key` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '加签密钥',
  `enable_ding` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '是否启用该钉钉机器人 1 启用 0 否',
  `creat_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 新增告警规则表
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
  PRIMARY KEY (`alert_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 告警记录表添加字段
ALTER TABLE `alert_record`
ADD COLUMN `lay` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警层面',
ADD COLUMN `expr` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则描述',
ADD COLUMN `alert_id` int(16) DEFAULT NULL COMMENT '规则ID',
ADD COLUMN `content` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警内容';

-- 2021.11.30 xutianhong
-- 新增集群ingress组件表
DROP TABLE IF EXISTS `cluster_ingress_components`;
CREATE TABLE `cluster_ingress_components`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'ingress class name',
  `cluster_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '集群id',
  `status` int(1) NULL COMMENT '状态',
  PRIMARY KEY (`id`)
) COMMENT = '集群ingress组件表';

-- 2021.12.3 xutianhong
-- resource_menu更新
UPDATE `resource_menu` SET alias_name = "资源池" WHERE `name` = "resourcePoolManagement";

-- 2021.12.7 xutianhong
-- 增加middleware_info version字段长度
ALTER TABLE `middleware_info`
MODIFY COLUMN `version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '版本' AFTER `type`;

-- 2021.12.8 xutianhong
-- cluster ingress components 添加ingress_class_name列
ALTER TABLE `cluster_ingress_components`
MODIFY COLUMN `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'ingress name' AFTER `id`,
ADD COLUMN `ingress_class_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'ingress class name' AFTER `name`;

-- 组件添加创建时间
ALTER TABLE `cluster_ingress_components`
ADD COLUMN `create_time` timestamp(0) NULL COMMENT '创建时间' AFTER `status`;

ALTER TABLE `cluster_components`
ADD COLUMN `create_time` timestamp(0) NULL COMMENT '创建时间' AFTER `status`;

-- 2021.12.24 xutianhong
-- 增加已删除中间件pvc字段长度
ALTER TABLE `cache_middleware` MODIFY COLUMN `pvc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'pvc' AFTER `chart_version`;

-- 2021.12.27 xutianhong
-- 自定义配置表相关字段长度优化
ALTER TABLE `custom_config`
MODIFY COLUMN `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段名称' AFTER `id`,
MODIFY COLUMN `default_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '默认值' AFTER `chart_name`,
MODIFY COLUMN `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '描述' AFTER `ranges`;

-- 修改配置修改历史表字段长度
ALTER TABLE `custom_config_history`
MODIFY COLUMN `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中间件名称' AFTER `namespace`,
MODIFY COLUMN `item` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配置名称' AFTER `name`;

-- 2022.1.17 yushuaikang
UPDATE `resource_menu` SET weight = 74 WHERE `name` = "operationAudit";
UPDATE `resource_menu` SET weight = 75 WHERE `name` = "resourcePoolManagement";
UPDATE `resource_menu` SET alias_name = "服务告警" WHERE `name` = "alarmCenter";
INSERT INTO `resource_menu` VALUES (17, 'systemAlarm', '系统告警', 'systemManagement/systemAlarm', 73, NULL, 7, NULL);
INSERT INTO `resource_menu_role` VALUES (null, 1, 17, 1);

-- 2022.1.17 liyinlong
-- 增加服务升级所需最低operator版本字段
ALTER TABLE `middleware_info`
    ADD COLUMN `compatible_versions` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '升级所需最低版本';


