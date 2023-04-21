-- 20230419 xutianhong
-- 角色列表添加类型字段
alter table role add type varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色类型' after name;
update `role` set type='manager' where weight='1';
update `role` set type='manager' where weight='2';
update `role` set type='manager' where weight='3';
update `role` set type='normal';