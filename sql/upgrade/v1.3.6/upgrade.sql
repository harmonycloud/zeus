-- xutianhong
-- 20240825 国际化功能研发表创建
drop table if exists sys_resource_translate_config;
create table sys_resource_translate_config
(
    `id`            int(8) primary key auto_increment comment '表ID 主健',
    `group_name`    varchar(64)  not null comment '那个对象组',
    `unique_value`  varchar(64)  not null comment '翻译的对象的instance_id',
    `language_code` char(16)     not null comment '语种code',
    `property`      varchar(128) not null comment '记录需要翻译的字段',
    `translation`   varchar(256) not null comment '翻译',
    unique index table_key_translation(`language_code`,`group_name`,`unique_value`,`property`) comment '联合索引查找顺序'
);

drop table if exists sys_regex_resource_config;
create table sys_regex_resource_config
(
    `id`         int(8) primary key auto_increment comment '表ID 主健',
    `group_name` varchar(64)  not null comment '哪个正则组 利用正则组概念可以减少正则匹配次数',
    `regex`      varchar(512) not null comment '正则表达式',
    unique index group_regex(`group_name`,`regex`) comment '同一个正则组下的正则表达式不可以重复'
);

drop table if exists sys_regex_resource_translate_config;
create table sys_regex_resource_translate_config
(
    `id`            int(8) primary key auto_increment comment '表ID 主健',
    `regex_id`      int(8) comment '正则表ID 定位到某一条匹配的正则表达式',
    `language_code` char(16)     not null comment '语种code',
    `translation`   varchar(512) not null comment '翻译',
    unique index group_regex_translate(`regex_id`,`language_code`) comment '同一条正则在某个语种下只能存在一条翻译'
);


truncate table sys_resource_translate_config;
-- 翻译默认角色名称
-- 超级管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','1','zh-HK','name','超級管理員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','1','en-US','name','Super Administrator');

-- 项目管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','2','zh-HK','name','項目管理員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','2','en-US','name','Project Administrator');

-- 运维人员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','3','zh-HK','name','運維人員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','3','en-US','name','O&M Member');

-- 普通用户
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','4','zh-HK','name','普通用戶');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','4','en-US','name','Ordinary Member');

-- 组织管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','5','zh-HK','name','組織管理員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','5','en-US','name','Organization Administrator');

-- 翻译菜单名称
-- 数据总览
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','1','zh-HK','aliasName','數據總覽');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','1','en-US','aliasName','Overview');

-- 中间件市场
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','2','zh-HK','aliasName','中間件市場');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','2','en-US','aliasName','Middleware Market');

-- 我的项目
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','3','zh-HK','aliasName','我的項目');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','3','en-US','aliasName','My Project');

-- 服务列表
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','4','zh-HK','aliasName','服務列表');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','4','en-US','aliasName','Middlewares');

-- 服务暴露
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','5','zh-HK','aliasName','服務列表');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','5','en-US','aliasName','Services Exposure');

-- 存储管理
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','6','zh-HK','aliasName','存儲管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','6','en-US','aliasName','StorageClasses');

-- 同城双活
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','7','zh-HK','aliasName','同城雙活');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','7','en-US','aliasName','Co-location Dual Activation');

-- 备份服务
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','8','zh-HK','aliasName','備份服務');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','8','en-US','aliasName','Middleware Backup');

-- 监控告警
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','9','zh-HK','aliasName','監控告警');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','9','en-US','aliasName','Monitoring Alerts');

-- 平台灾备
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','10','zh-HK','aliasName','平台災備');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','10','en-US','aliasName','Platform Disaster Recovery');

-- 系统管理
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','11','zh-HK','aliasName','系統管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','11','en-US','aliasName','Administration');

-- 备份任务
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','12','zh-HK','aliasName','備份任務');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','12','en-US','aliasName','Backup');

-- 备份服务器
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','13','zh-HK','aliasName','備份服務器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','13','en-US','aliasName','Backup Server');

-- 数据监控
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','14','zh-HK','aliasName','數據監控');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','14','en-US','aliasName','Monitoring');

-- 日志详情
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','15','zh-HK','aliasName','日誌詳情');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','15','en-US','aliasName','Logs');

-- 告警中心
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','16','zh-HK','aliasName','告警中心');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','16','en-US','aliasName','Alerts');

-- 集群管理
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','17','zh-HK','aliasName','集群管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','17','en-US','aliasName','Clusters');

-- 用户管理
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','18','zh-HK','aliasName','用戶管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','18','en-US','aliasName','Users');

-- 组织管理
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','19','zh-HK','aliasName','組織管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','19','en-US','aliasName','Organizations');

-- 角色管理
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','20','zh-HK','aliasName','角色管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','20','en-US','aliasName','Roles');

-- 操作审计
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','22','zh-HK','aliasName','操作審計');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','22','en-US','aliasName','ActionTrail');

-- 成员管理
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','23','zh-HK','aliasName','成員管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','23','en-US','aliasName','Users');

-- 组织概览
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','24','zh-HK','aliasName','組織概覽');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('resource_menu','24','en-US','aliasName','Organizational Overview');