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
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `background_image` mediumblob COMMENT '背景图',
  `background_image_path` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '背景图地址',
  `login_logo` mediumblob COMMENT '登录页logo',
  `login_logo_path` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '登录页logo地址',
  `home_logo` mediumblob COMMENT '主页logo',
  `home_logo_path` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '主页logo地址',
  `platform_name` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '平台名称',
  `slogan` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '标语',
  `copyright_notice` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '版权声明',
  `title` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '浏览器标题',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `status` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '是否默认',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 新增邮箱信息表
DROP TABLE IF EXISTS `mail_info`;
CREATE TABLE `mail_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `mail_server` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '邮箱服务器',
  `port` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '端口',
  `username` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '用户',
  `password` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '密码',
  `mail_path` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '邮箱地址',
  `creat_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 新增被通知人表
DROP TABLE IF EXISTS `mail_to_user`;
CREATE TABLE `mail_to_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` int(11) DEFAULT NULL COMMENT '用户ID',
  `username` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '用户',
  `alias_name` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '账户',
  `email` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '电话',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 新增钉钉机器人表
DROP TABLE IF EXISTS `ding_robot_info`;
CREATE TABLE `ding_robot_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `webhook` varchar(128) COLLATE utf8_bin NOT NULL,
  `secret_key` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '加签密钥',
  `enable_ding` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '是否启用该钉钉机器人 1 启用 0 否',
  `creat_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 新增告警规则表
DROP TABLE IF EXISTS `alert_rule_id`;
CREATE TABLE `alert_rule_id` (
  `alert_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `cluster_id` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '集群ID',
  `namespace` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '命名空间',
  `middleware_name` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '中间件名称',
  `alert` text COLLATE utf8_bin COMMENT '规则名称',
  `name` varchar(16) COLLATE utf8_bin DEFAULT NULL,
  `silence` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '沉默时间',
  `symbol` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '符号',
  `threshold` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '阈值',
  `time` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '分钟周期',
  `type` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '中间件类型',
  `unit` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '单位',
  `expr` varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT '执行规则',
  `description` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '监控项',
  `annotations` text COLLATE utf8_bin COMMENT '备注',
  `labels` text COLLATE utf8_bin COMMENT '标签',
  `id` varchar(32) COLLATE utf8_bin DEFAULT NULL,
  `alert_time` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '告警时间',
  `alert_times` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '告警次数',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `status` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '状态',
  `enable` int(11) DEFAULT NULL COMMENT '是否启用',
  `content` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '告警内容',
  `lay` varchar(12) COLLATE utf8_bin DEFAULT NULL COMMENT 'system 系统告警 service 服务告警',
  PRIMARY KEY (`alert_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
-- 告警记录表添加lay
ALTER TABLE `alert_record` ADD COLUMN lay varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '告警层面';
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
-- 2021.12.6 yushuaikang
INSERT INTO `resource_menu` VALUES (17, 'systemAlarm', '系统告警', 'systemManagement/systemAlarm', 74, NULL, 7, NULL);
UPDATE `resource_menu` SET alias_name = "服务告警" WHERE `name` = "alarmCenter";
INSERT INTO `resource_menu_role` VALUES (17, 1, 17, 1);
-- 2021.12.7 yushuaikang
ALTER TABLE `personal_config` MODIFY title VARCHAR(64)
ALTER TABLE `alert_rule_id` MODIFY name VARCHAR(64)




