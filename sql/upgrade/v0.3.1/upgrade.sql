-- 2022.3.10
-- 新增镜像仓库表
DROP TABLE IF EXISTS `mirror_image`;
CREATE TABLE `mirror_image` (
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

-- 2022.3.14 xutianhong
-- 自定义参数
DROP TABLE IF EXISTS `middleware_param_top`;
CREATE TABLE `middleware_param_top`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `cluster_id` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '集群id',
  `namespace` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '分区',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '名称',
  `param` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '参数名',
  PRIMARY KEY (`id`)
);

ALTER TABLE `custom_config_template`
ADD COLUMN `create_time` timestamp(0) NULL COMMENT '创建时间' AFTER `config`;

-- 2022.3.14 liyinlong
-- 新增ldap配置表
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

-- 2022.3.21 liyinlong
-- 新增开放中心菜单
DELETE FROM `resource_menu_role` WHERE `resource_menu_id` = 18;
INSERT INTO `resource_menu_role` (role_id, resource_menu_id, available) VALUES (1, 18, 1);
DELETE FROM `resource_menu` WHERE `name` = 'openCenter';
INSERT INTO `resource_menu` VALUES (18, 'openCenter', '开放中心', 'systemManagement/openCenter', 76, NULL, 7, NULL);