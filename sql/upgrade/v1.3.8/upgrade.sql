
-- xutianhong 20250428
-- 新增中间件版本禁用表
drop table if exists `middleware_disable_version`;
CREATE TABLE `middleware_disable_version`
(
    `id`         int NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '集群id',
    `chart_name`  varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT 'chart类型',
    `chart_version` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart版本',
    `disable_version`       varchar(128) DEFAULT NULL COMMENT '禁用版本',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='中间件禁用版本';

-- 20250724 xutianhong
-- 新增翻译
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '设置中间件禁用版本';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','设置中间件禁用版本','en-US','actionChDesc','Set middleware disabled version');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','设置中间件禁用版本','zh-HK','actionChDesc','設定中間件禁用版本');
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '启用/停用ldap';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','启用/停用ldap','en-US','actionChDesc','enable/disable ldap');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','启用/停用ldap','zh-HK','actionChDesc','啟用/停用ldap');
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '保存ldap配置';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存ldap配置','en-US','actionChDesc','save ldap config');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存ldap配置','zh-HK','actionChDesc','保存ldap配置');
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '创建日志告警规则';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建日志告警规则','en-US','actionChDesc','create log alert rule');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建日志告警规则','zh-HK','actionChDesc','建立日志告警規則');
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '更新日志告警规则';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新日志告警规则','en-US','actionChDesc','update log alert rule');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新日志告警规则','zh-HK','actionChDesc','更新日志告警規則');
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '删除日志告警规则';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除日志告警规则','en-US','actionChDesc','delete log alert rule');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除日志告警规则','zh-HK','actionChDesc','刪除日志告警規則');