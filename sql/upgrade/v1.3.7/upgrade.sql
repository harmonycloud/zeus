ALTER TABLE backup_position ADD backup_server_detail_id int NOT NULL comment '备份服务器详情id';

CREATE TEMPORARY TABLE temp_ids AS
SELECT  t1.name, t1.organ_id, t1.project_id, t1.backup_server_id, t1.backup_position, t1.create_time, t2.id AS backup_server_detail_id
FROM backup_position t1
         JOIN backup_server_detail t2 ON t1.backup_server_id = t2.backup_server_id;

SELECT * FROM temp_ids;

DELETE FROM backup_position WHERE TRUE;

INSERT INTO backup_position(name, organ_id, project_id, backup_server_id, backup_position, create_time, backup_server_detail_id)
SELECT * FROM temp_ids;

DROP TABLE temp_ids;

-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','读写(pgbouncer)','zh-HK','servicePurpose','讀寫(pgbouncer)');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','读写(pgbouncer)','en-US','servicePurpose','Read Write(pgbouncer)');

-- 20241128 xutianhong
-- 新增翻译
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '获取项目下中间件资源';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取项目下中间件资源','en-US','actionChDesc','Query middleware resources within the project');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取项目下中间件资源','zh-HK','actionChDesc','獲取項目下中間件資源');
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '查询集群下中间件资源详情';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询集群下中间件资源详情','en-US','actionChDesc','Query middleware resources within the cluster');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询集群下中间件资源详情','zh-HK','actionChDesc','獲取集群下中間件資源');
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '禁用/启用备份任务';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','禁用/启用备份任务','en-US','actionChDesc','enable/disable backup');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','禁用/启用备份任务','zh-HK','actionChDesc','禁用/啟用備份熱任務');
-- 英语
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '查询项目下namespace资源详情';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询集群下namespace资源详情','en-US','actionChDesc','Query namespace resource within the cluster');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询集群下namespace资源详情','zh-HK','actionChDesc','查詢集群下namespace資源詳情');

-- 修改服务管理为运维面板
-- 繁体中文
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '服务管理';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','运维面板','zh-HK','childModuleChDesc','運維面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','运维面板','en-US','childModuleChDesc','O&M Dashboard');

-- 繁体中文
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = 'Mysql管理面板';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Mysql运维面板','zh-HK','childModuleChDesc','Mysql運維面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Mysql运维面板','en-US','childModuleChDesc','Mysql Dashboard');
-- 繁体中文
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = 'Postgresql管理面板';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Postgresql运维面板','zh-HK','childModuleChDesc','Postgresql運維面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Postgresql运维面板','en-US','childModuleChDesc','Postgresql dashboard');
-- 繁体中文
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = 'Redis管理面板';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Redis运维面板','zh-HK','childModuleChDesc','Redis運維面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Redis运维面板','en-US','childModuleChDesc','Redis dashboard');
-- 更新已有的审计日志
update operation_audit set child_module_ch_desc = '运维面板' where module_ch_desc = '服务列表' and child_module_ch_desc = '服务管理';
update operation_audit set child_module_ch_desc = 'Mysql运维面板' where module_ch_desc = '服务列表' and child_module_ch_desc = 'Mysql管理面板';
update operation_audit set child_module_ch_desc = 'Postgresql运维面板' where module_ch_desc = '服务列表' and child_module_ch_desc = 'Postgresql管理面板';
update operation_audit set child_module_ch_desc = 'Redis运维面板' where module_ch_desc = '服务列表' and child_module_ch_desc = 'Redis管理面板';