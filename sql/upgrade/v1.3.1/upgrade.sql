-- 20230313 xutianhong
-- 创建组织表
DROP TABLE IF EXISTS `organization`;
CREATE TABLE `organization` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `organ_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组织id',
    `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组织名称',
    `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织表';

-- 创建组织用户关联表
DROP TABLE IF EXISTS `organization_user`;
CREATE TABLE `organization_user` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `organ_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组织id',
    `username` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
    `role_id` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '角色id',
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
    `cluster_id` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目id',
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
update `resource_menu` set name='organizationManagement',alias_name='组织管理',url='systemManagement/organizationManagement' where id='19';
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

--20230323 wangpenglei
--灾备中心更名为平台灾备
UPDATE resource_menu SET alias_name = '平台灾备' WHERE id = 10;

--20230328 wangpenglei
--system_config表config_value更改类型为text
ALTER TABLE system_config modify config_value text NULL;


