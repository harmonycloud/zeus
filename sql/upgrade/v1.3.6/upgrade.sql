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

-- 20241009 xutianhong
-- 个性化配置表单添加language字段
alter table personal_config add language varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL comment '语言';
delete from personal_config where status = '0';

-- 操作审计国际化
delete from sys_resource_translate_config where group_name = 'operation_audit';
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','系统管理','zh-HK','moduleChDesc','系統管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','系统管理','en-US','moduleChDesc','Administration');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','用户管理','zh-HK','childModuleChDesc','用戶管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','用户管理','en-US','childModuleChDesc','Users');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务列表','zh-HK','childModuleChDesc','服務列表');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务列表','en-US','childModuleChDesc','Middlewares');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务管理','zh-HK','childModuleChDesc','服務管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务管理','en-US','childModuleChDesc','Middlewares');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Mysql管理面板','zh-HK','childModuleChDesc','Mysql管理面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Mysql管理面板','en-US','childModuleChDesc','Mysql Management Dashboard');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Postgresql管理面板','zh-HK','childModuleChDesc','Postgresql管理面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Postgresql管理面板','en-US','childModuleChDesc','Postgresql Management dashboard');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Redis管理面板','zh-HK','childModuleChDesc','Redis管理面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Redis管理面板','en-US','childModuleChDesc','Redis Management dashboard');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','集群管理','zh-HK','moduleChDesc','集群管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','集群管理','en-US','moduleChDesc','Clusters');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','基础资源','zh-HK','childModuleChDesc','基礎資源');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','基础资源','en-US','childModuleChDesc','Basic Resources');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务暴露','zh-HK','childModuleChDesc','服務暴露');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务暴露','en-US','childModuleChDesc','Services Exposure');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','实例列表','zh-HK','childModuleChDesc','實例列表');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','实例列表','en-US','childModuleChDesc','Instances');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','平台管理','zh-HK','moduleChDesc','平台管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','平台管理','en-US','moduleChDesc','Platform Management');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','存储管理','zh-HK','childModuleChDesc','存儲管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','存储管理','en-US','childModuleChDesc','StorageClasses');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Yaml格式校验','zh-HK','childModuleChDesc','Yaml格式校驗');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Yaml格式校验','en-US','childModuleChDesc','Yaml Format Validation');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','日志详情','zh-HK','childModuleChDesc','日誌詳情');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','日志详情','en-US','childModuleChDesc','Logs');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目管理','zh-HK','moduleChDesc','項目管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目管理','en-US','moduleChDesc','Projects');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','备份位置','zh-HK','childModuleChDesc','備份位置');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','备份位置','en-US','childModuleChDesc','Backup Location');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','备份服务器','zh-HK','childModuleChDesc','備份服務器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','备份服务器','en-US','childModuleChDesc','Backup Server');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','参数管理','zh-HK','childModuleChDesc','餐數管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','参数管理','en-US','childModuleChDesc','Parameters');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','监控告警','zh-HK','moduleChDesc','監控告警');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','监控告警','en-US','moduleChDesc','Monitoring Alerts');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务告警','zh-HK','childModuleChDesc','服務告警');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务告警','en-US','childModuleChDesc','Middleware Alerts');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','镜像仓库','zh-HK','childModuleChDesc','鏡像倉庫');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','镜像仓库','en-US','childModuleChDesc','Image Repository');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','数据安全','zh-HK','moduleChDesc','數據安全');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','数据安全','en-US','moduleChDesc','Middleware Backup');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','备份服务','zh-HK','childModuleChDesc','備份服務');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','备份服务','en-US','childModuleChDesc','Middleware Backup');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','可用区','zh-HK','childModuleChDesc','可用區');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','可用区','en-US','childModuleChDesc','Zone');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','告警中心','zh-HK','moduleChDesc','告警中心');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','告警中心','en-US','moduleChDesc','Alerts');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','平台灾备','zh-HK','moduleChDesc','平台災備');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','平台灾备','en-US','moduleChDesc','Platform Disaster Recovery');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Feature列表','zh-HK','childModuleChDesc','Feature列表');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','Feature列表','en-US','childModuleChDesc','Features');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','开放中心','zh-HK','childModuleChDesc','開放中心');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','开放中心','en-US','childModuleChDesc','Open Center');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','license管理','zh-HK','childModuleChDesc','License管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','license管理','en-US','childModuleChDesc','License');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','操作审计','zh-HK','childModuleChDesc','操作審計');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','操作审计','en-US','childModuleChDesc','ActionTrail');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','钉钉','zh-HK','childModuleChDesc','釘釘');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','钉钉','en-US','childModuleChDesc','Ding Talk');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','组织管理','zh-HK','childModuleChDesc','組織管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','组织管理','en-US','childModuleChDesc','Organizations');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','角色管理','zh-HK','childModuleChDesc','角色管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','角色管理','en-US','childModuleChDesc','Roles');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','用户认证','zh-HK','childModuleChDesc','用戶認證');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','用户认证','en-US','childModuleChDesc','Authentication');

-- 更新部分模块名称
update operation_audit set module_ch_desc = '集群管理' where module_ch_desc = '平台工具箱';
update operation_audit set module_ch_desc = '平台管理' where module_ch_desc = '系统管理' and child_module_ch_desc = '用户管理';
update operation_audit set module_ch_desc = '平台管理' where module_ch_desc = '存储管理' and child_module_ch_desc = '存储管理';
update operation_audit set module_ch_desc = '服务列表' where module_ch_desc = '中间件面板' and child_module_ch_desc = 'sql console';
update operation_audit set module_ch_desc = '平台管理' where action_ch_desc = '新增角色' or action_ch_desc = '删除角色' or action_ch_desc = '修改角色';
update operation_audit set child_module_ch_desc = '角色管理' where action_ch_desc = '新增角色' or action_ch_desc = '删除角色' or action_ch_desc = '修改角色';
update operation_audit set module_ch_desc = '平台管理' where module_ch_desc = '系统管理' and child_module_ch_desc = '项目管理';
update operation_audit set module_ch_desc = '服务列表' where module_ch_desc = '中间件面板' and child_module_ch_desc = 'postgresql面板';
update operation_audit set child_module_ch_desc = '实例列表' where module_ch_desc = '服务列表' and child_module_ch_desc = '服务实例';
update operation_audit set module_ch_desc = '平台管理' where module_ch_desc = '系统管理' and child_module_ch_desc = '组织管理';
update operation_audit set module_ch_desc = '平台管理' where module_ch_desc = '系统管理' and child_module_ch_desc = '操作审计';
update operation_audit set child_module_ch_desc = null where module_ch_desc = '中间件市场' and child_module_ch_desc = '中间件管理';
update operation_audit set child_module_ch_desc = '参数管理' where module_ch_desc = '服务列表' and child_module_ch_desc = '服务配置';
update operation_audit set module_ch_desc = '数据安全', child_module_ch_desc = '备份服务' where module_ch_desc = '容灾备份' and child_module_ch_desc = '数据安全';
update operation_audit set module_ch_desc = '平台管理', child_module_ch_desc = '邮箱' where module_ch_desc = '邮件发送' and child_module_ch_desc = '邮件发送';
update operation_audit set module_ch_desc = '服务列表' where module_ch_desc = '监控告警' and child_module_ch_desc = '日志详情';
update operation_audit set module_ch_desc = '平台管理', child_module_ch_desc = 'license管理' where module_ch_desc = '系统管理' and child_module_ch_desc = '平台认证';
update operation_audit set module_ch_desc = '服务列表', child_module_ch_desc = '服务暴露' where module_ch_desc = '服务暴露' and child_module_ch_desc = '对外访问';
update operation_audit set module_ch_desc = '集群管理', child_module_ch_desc = '镜像仓库' where module_ch_desc = '系统管理' and child_module_ch_desc = '集群管理';
update operation_audit set child_module_ch_desc = 'Feature列表' where module_ch_desc = '平台管理' and child_module_ch_desc = '功能列表';
update operation_audit set module_ch_desc = '平台灾备', child_module_ch_desc = null where module_ch_desc = '平台管理' and child_module_ch_desc = '灾备中心';
update operation_audit set module_ch_desc = '平台管理', child_module_ch_desc = '钉钉' where module_ch_desc = '钉钉告警' and child_module_ch_desc = '钉钉告警';
update operation_audit set child_module_ch_desc = '参数管理' where module_ch_desc = '服务列表' and child_module_ch_desc = '参数设置';
update operation_audit set module_ch_desc = '平台管理' where module_ch_desc = '备份服务' and child_module_ch_desc = '备份服务器';
update operation_audit set module_ch_desc = '项目管理' where module_ch_desc = '备份服务' and child_module_ch_desc = '备份位置';
update operation_audit set module_ch_desc = '平台管理', child_module_ch_desc = '用户认证' where action_ch_desc = '登录' or action_ch_desc = '登出';
update operation_audit set module_ch_desc = '告警中心', child_module_ch_desc = null where module_ch_desc = '监控告警' and child_module_ch_desc = '告警中心';
