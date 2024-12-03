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
values('servicePurpose','读写(Pgbouncer)','zh-HK','servicePurpose','讀寫(Pgbouncer)');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','读写(Pgbouncer)','en-US','servicePurpose','Read Write(Pgbouncer)');

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
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = '运维面板';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','运维面板','zh-HK','childModuleChDesc','運維面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','运维面板','en-US','childModuleChDesc','O&M Dashboard');

-- 繁体中文
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = 'Mysql管理面板';
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = 'Mysql运维面板';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Mysql运维面板','zh-HK','childModuleChDesc','Mysql運維面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Mysql运维面板','en-US','childModuleChDesc','Mysql Dashboard');
-- 繁体中文
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = 'Postgresql管理面板';
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = 'Postgresql运维面板';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Postgresql运维面板','zh-HK','childModuleChDesc','Postgresql運維面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Postgresql运维面板','en-US','childModuleChDesc','Postgresql Dashboard');
-- 繁体中文
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = 'Redis管理面板';
delete from sys_resource_translate_config where `group_name` = 'operation_audit' and `unique_value` = 'Redis运维面板';
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Redis运维面板','zh-HK','childModuleChDesc','Redis運維面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Redis运维面板','en-US','childModuleChDesc','Redis Dashboard');
-- 更新已有的审计日志
update operation_audit set child_module_ch_desc = '运维面板' where module_ch_desc = '服务列表' and child_module_ch_desc = '服务管理';
update operation_audit set child_module_ch_desc = 'Mysql运维面板' where module_ch_desc = '服务列表' and child_module_ch_desc = 'Mysql管理面板';
update operation_audit set child_module_ch_desc = 'Postgresql运维面板' where module_ch_desc = '服务列表' and child_module_ch_desc = 'Postgresql管理面板';
update operation_audit set child_module_ch_desc = 'Redis运维面板' where module_ch_desc = '服务列表' and child_module_ch_desc = 'Redis管理面板';
update operation_audit set child_module_ch_desc = 'Sql Console' where module_ch_desc = '服务列表' and child_module_ch_desc = 'sql console';

-- 默认角色权限修改
delete from resource_menu_role where role_id = 1;
delete from resource_menu_role where role_id = 2;
delete from resource_menu_role where role_id = 3;
delete from resource_menu_role where role_id = 4;
delete from resource_menu_role where role_id = 5;
INSERT INTO `resource_menu_role` VALUES (1,1,1,1);
INSERT INTO `resource_menu_role` VALUES (2,1,2,1);
INSERT INTO `resource_menu_role` VALUES (3,1,3,0);
INSERT INTO `resource_menu_role` VALUES (4,1,4,1);
INSERT INTO `resource_menu_role` VALUES (5,1,5,1);
INSERT INTO `resource_menu_role` VALUES (6,1,6,1);
INSERT INTO `resource_menu_role` VALUES (7,1,7,1);
INSERT INTO `resource_menu_role` VALUES (8,1,8,1);
INSERT INTO `resource_menu_role` VALUES (9,1,9,1);
INSERT INTO `resource_menu_role` VALUES (10,1,10,1);
INSERT INTO `resource_menu_role` VALUES (11,1,11,1);
INSERT INTO `resource_menu_role` VALUES (12,1,12,1);
INSERT INTO `resource_menu_role` VALUES (13,1,13,1);
INSERT INTO `resource_menu_role` VALUES (14,1,14,1);
INSERT INTO `resource_menu_role` VALUES (15,1,15,1);
INSERT INTO `resource_menu_role` VALUES (16,1,16,1);
INSERT INTO `resource_menu_role` VALUES (17,1,17,1);
INSERT INTO `resource_menu_role` VALUES (18,1,18,1);
INSERT INTO `resource_menu_role` VALUES (19,1,19,1);
INSERT INTO `resource_menu_role` VALUES (20,1,20,1);
INSERT INTO `resource_menu_role` VALUES (21,1,21,1);
INSERT INTO `resource_menu_role` VALUES (22,1,22,1);
INSERT INTO `resource_menu_role` VALUES (23,1,23,0);
INSERT INTO `resource_menu_role` VALUES (24,1,24,0);
INSERT INTO `resource_menu_role` VALUES (25,2,1,0);
INSERT INTO `resource_menu_role` VALUES (26,2,2,0);
INSERT INTO `resource_menu_role` VALUES (27,2,3,1);
INSERT INTO `resource_menu_role` VALUES (28,2,4,1);
INSERT INTO `resource_menu_role` VALUES (29,2,5,1);
INSERT INTO `resource_menu_role` VALUES (30,2,6,0);
INSERT INTO `resource_menu_role` VALUES (31,2,7,0);
INSERT INTO `resource_menu_role` VALUES (32,2,8,1);
INSERT INTO `resource_menu_role` VALUES (33,2,9,1);
INSERT INTO `resource_menu_role` VALUES (34,2,10,1);
INSERT INTO `resource_menu_role` VALUES (35,2,11,0);
INSERT INTO `resource_menu_role` VALUES (36,2,12,1);
INSERT INTO `resource_menu_role` VALUES (37,2,13,0);
INSERT INTO `resource_menu_role` VALUES (38,2,14,1);
INSERT INTO `resource_menu_role` VALUES (39,2,15,1);
INSERT INTO `resource_menu_role` VALUES (40,2,16,0);
INSERT INTO `resource_menu_role` VALUES (41,2,17,0);
INSERT INTO `resource_menu_role` VALUES (42,2,18,0);
INSERT INTO `resource_menu_role` VALUES (43,2,19,0);
INSERT INTO `resource_menu_role` VALUES (44,2,20,0);
INSERT INTO `resource_menu_role` VALUES (45,2,21,0);
INSERT INTO `resource_menu_role` VALUES (46,2,22,0);
INSERT INTO `resource_menu_role` VALUES (47,2,23,0);
INSERT INTO `resource_menu_role` VALUES (48,2,24,0);
INSERT INTO `resource_menu_role` VALUES (49,3,1,0);
INSERT INTO `resource_menu_role` VALUES (50,3,2,0);
INSERT INTO `resource_menu_role` VALUES (51,3,3,1);
INSERT INTO `resource_menu_role` VALUES (52,3,4,1);
INSERT INTO `resource_menu_role` VALUES (53,3,5,1);
INSERT INTO `resource_menu_role` VALUES (54,3,6,0);
INSERT INTO `resource_menu_role` VALUES (55,3,7,0);
INSERT INTO `resource_menu_role` VALUES (56,3,8,1);
INSERT INTO `resource_menu_role` VALUES (57,3,9,1);
INSERT INTO `resource_menu_role` VALUES (58,3,10,1);
INSERT INTO `resource_menu_role` VALUES (59,3,11,0);
INSERT INTO `resource_menu_role` VALUES (60,3,12,1);
INSERT INTO `resource_menu_role` VALUES (61,3,13,0);
INSERT INTO `resource_menu_role` VALUES (62,3,14,1);
INSERT INTO `resource_menu_role` VALUES (63,3,15,1);
INSERT INTO `resource_menu_role` VALUES (64,3,16,0);
INSERT INTO `resource_menu_role` VALUES (65,3,17,0);
INSERT INTO `resource_menu_role` VALUES (66,3,18,0);
INSERT INTO `resource_menu_role` VALUES (67,3,19,0);
INSERT INTO `resource_menu_role` VALUES (68,3,20,0);
INSERT INTO `resource_menu_role` VALUES (69,3,21,0);
INSERT INTO `resource_menu_role` VALUES (70,3,22,0);
INSERT INTO `resource_menu_role` VALUES (71,3,23,0);
INSERT INTO `resource_menu_role` VALUES (72,4,24,0);
INSERT INTO `resource_menu_role` VALUES (73,4,1,0);
INSERT INTO `resource_menu_role` VALUES (74,4,2,0);
INSERT INTO `resource_menu_role` VALUES (75,4,3,1);
INSERT INTO `resource_menu_role` VALUES (76,4,4,1);
INSERT INTO `resource_menu_role` VALUES (77,4,5,0);
INSERT INTO `resource_menu_role` VALUES (78,4,6,0);
INSERT INTO `resource_menu_role` VALUES (79,4,7,0);
INSERT INTO `resource_menu_role` VALUES (80,4,8,0);
INSERT INTO `resource_menu_role` VALUES (81,4,9,0);
INSERT INTO `resource_menu_role` VALUES (82,4,10,0);
INSERT INTO `resource_menu_role` VALUES (83,4,11,0);
INSERT INTO `resource_menu_role` VALUES (84,4,12,0);
INSERT INTO `resource_menu_role` VALUES (85,4,13,0);
INSERT INTO `resource_menu_role` VALUES (86,4,14,0);
INSERT INTO `resource_menu_role` VALUES (87,4,15,0);
INSERT INTO `resource_menu_role` VALUES (88,4,16,0);
INSERT INTO `resource_menu_role` VALUES (89,4,17,0);
INSERT INTO `resource_menu_role` VALUES (90,4,18,0);
INSERT INTO `resource_menu_role` VALUES (91,4,19,0);
INSERT INTO `resource_menu_role` VALUES (92,4,20,0);
INSERT INTO `resource_menu_role` VALUES (93,4,21,0);
INSERT INTO `resource_menu_role` VALUES (94,4,22,0);
INSERT INTO `resource_menu_role` VALUES (95,4,23,0);
INSERT INTO `resource_menu_role` VALUES (96,4,24,0);
INSERT INTO `resource_menu_role` VALUES (97,5,1,0);
INSERT INTO `resource_menu_role` VALUES (98,5,2,0);
INSERT INTO `resource_menu_role` VALUES (99,5,3,1);
INSERT INTO `resource_menu_role` VALUES (100,5,4,0);
INSERT INTO `resource_menu_role` VALUES (101,5,5,0);
INSERT INTO `resource_menu_role` VALUES (102,5,6,0);
INSERT INTO `resource_menu_role` VALUES (103,5,7,0);
INSERT INTO `resource_menu_role` VALUES (104,5,8,0);
INSERT INTO `resource_menu_role` VALUES (105,5,9,0);
INSERT INTO `resource_menu_role` VALUES (106,5,10,0);
INSERT INTO `resource_menu_role` VALUES (107,5,11,0);
INSERT INTO `resource_menu_role` VALUES (108,5,12,0);
INSERT INTO `resource_menu_role` VALUES (109,5,13,0);
INSERT INTO `resource_menu_role` VALUES (110,5,14,0);
INSERT INTO `resource_menu_role` VALUES (111,5,15,0);
INSERT INTO `resource_menu_role` VALUES (112,5,16,0);
INSERT INTO `resource_menu_role` VALUES (113,5,17,0);
INSERT INTO `resource_menu_role` VALUES (114,5,18,0);
INSERT INTO `resource_menu_role` VALUES (115,5,19,0);
INSERT INTO `resource_menu_role` VALUES (116,5,20,0);
INSERT INTO `resource_menu_role` VALUES (117,5,21,0);
INSERT INTO `resource_menu_role` VALUES (118,5,22,0);
INSERT INTO `resource_menu_role` VALUES (119,5,23,1);
INSERT INTO `resource_menu_role` VALUES (120,5,24,1);