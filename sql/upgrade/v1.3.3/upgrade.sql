-- 20230419 xutianhong
-- 角色列表添加类型字段
alter table role add type varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色类型' after name;
update `role` set type='normal';
update `role` set type='manager' where weight='1';
update `role` set type='manager' where weight='2';
update `role` set type='manager' where weight='3';


-- 20230424 wangpenglei
-- 参数相关新增role字段
alter table custom_config add role varchar(16) null comment '节点类型';
update custom_config set role = 'major' where role is null;
alter table custom_config_history add role varchar(16) null comment '节点类型';
update custom_config_history set role = 'major' where role is null;
alter table custom_config_template add role varchar(16) null comment '节点类型';
update custom_config_template set role = 'major' where role is null;
alter table middleware_param_top add role varchar(16) null comment '节点类型';
update middleware_param_top set role = 'major' where role is null;

-- 20230506 xutianhong
-- 告警记录表字段修改
alter table alert_record change time alert_time timestamp null comment '权重';
alter table alert_record add alert_receive_time timestamp null comment '权重' after alert_time;

-- 新增告警用户表
DROP TABLE IF EXISTS `alert_user`;
CREATE TABLE `alert_user`(
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名称',
    `alert_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '告警对象类型',
    `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群id',
    `namespace` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '命名空间',
    `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
    `mail_alert` tinyint(1) DEFAULT NULL COMMENT '邮箱告警',
    `message_alert` tinyint(1) DEFAULT NULL COMMENT '短信告警',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='告警用户表';

-- alert_record表添加字段
alter table alert_record add alias_name varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '别名' after name;

-- menu菜单更新
DELETE FROM middleware_platform.resource_menu WHERE id = 21;
UPDATE resource_menu t SET t.alias_name = '告警中心', t.url = 'monitorAlarm/alarmCenter/system' WHERE t.id = 16;