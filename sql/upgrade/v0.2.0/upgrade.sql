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
  `creat_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;