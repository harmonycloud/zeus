-- 20230419 xutianhong
-- 角色列表添加类型字段
alter table role add type varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色类型' after name;
update `role` set type='manager' where weight='1';
update `role` set type='manager' where weight='2';
update `role` set type='manager' where weight='3';
update `role` set type='normal';

-- 20230424 wangpenglei
-- 参数相关新增role字段
alter table custom_config add role varchar(16) null comment '节点类型';
update custom_config set role = 'master' where role is null;
alter table custom_config_history add role varchar(16) null comment '节点类型';
update custom_config_history set role = 'master' where role is null;
alter table custom_config_template add role varchar(16) null comment '节点类型';
update custom_config_template set role = 'master' where role is null;
alter table middleware_param_top add role varchar(16) null comment '节点类型';
update middleware_param_top set role = 'master' where role is null;
