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
values('role','3','zh-HK','name','項目管理員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','3','en-US','name','Project Administrator');

-- 运维人员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','4','zh-HK','name','運維人員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','4','en-US','name','O&M Member');

-- 普通用户
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','5','zh-HK','name','普通用戶');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','5','en-US','name','Ordinary Member');

-- 组织管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','2','zh-HK','name','組織管理員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','2','en-US','name','Organization Administrator');

-- 翻译默认角色描述
-- 超级管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','1','zh-HK','description','擁有所有最高權限');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','1','en-US','description','Having all the highest permissions.');

-- 项目管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','3','zh-HK','description','擁有項目管理權限');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','3','en-US','description','Having project management permissions.');

-- 运维人员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','4','zh-HK','description','擁有中間件運維權限');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','4','en-US','description','Having middleware O&M permissions.');

-- 普通用户
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','5','zh-HK','description','擁有平臺查看權限');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','5','en-US','description','Having platform viewing permissions.');

-- 组织管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','2','zh-HK','description','擁有組織管理權限');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('role','2','en-US','description','Having organization management permissions.');

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
values('operation_audit','服务列表','zh-HK','moduleChDesc','服務列表');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务列表','en-US','moduleChDesc','Middlewares');
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
values('operation_audit','yaml格式校验','zh-HK','childModuleChDesc','Yaml格式校驗');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','yaml格式校验','en-US','childModuleChDesc','Yaml Format Validation');
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
values('operation_audit','项目管理','zh-HK','childModuleChDesc','項目管理');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目管理','en-US','childModuleChDesc','Projects');
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
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件市场','zh-HK','moduleChDesc','中間件市場');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件市场','en-US','moduleChDesc','Middleware Market');

-- 操作审计默认用户/角色翻译
-- 超级管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','超级管理员','zh-HK','userName','超級管理員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','超级管理员','en-US','userName','Super Administrator');
-- 超级管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','超级管理员','zh-HK','roleName','超級管理員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','超级管理员','en-US','roleName','Super Administrator');

-- 项目管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目管理员','zh-HK','roleName','項目管理員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目管理员','en-US','roleName','Project Administrator');

-- 运维人员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','运维人员','zh-HK','roleName','運維人員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','运维人员','en-US','roleName','O&M Member');

-- 普通用户
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','普通用户','zh-HK','roleName','普通用戶');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','普通用户','en-US','roleName','Ordinary Member');

-- 更新部分模块名称
update operation_audit set module_ch_desc = '集群管理',child_module_ch_desc = 'Yaml格式校验' where module_ch_desc = '平台工具箱' and child_module_ch_desc = '格式校验';
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


-- 用户表超级管理员国际化
delete from sys_resource_translate_config where group_name = 'UserDto';
-- 超级管理员
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('UserDto','超级管理员','zh-HK','aliasName','超級管理員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('UserDto','超级管理员','en-US','aliasName','Super Administrator');

-- 操作审计动作内容国际化
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建备份位置','zh-HK','actionChDesc','創建備份位置');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建备份位置','en-US','actionChDesc','Create Backup Position');

-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除备份位置','zh-HK','actionChDesc','刪除備份位置');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除备份位置','en-US','actionChDesc','Delete Backup Position');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新备份位置','zh-HK','actionChDesc','更新備份位置');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新备份位置','en-US','actionChDesc','Update Backup Position');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询备份服务器列表','zh-HK','actionChDesc','查詢備份服務器列表');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询备份服务器列表','en-US','actionChDesc','Query Backup Server List');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建备份服务器','zh-HK','actionChDesc','創建備份服務器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建备份服务器','en-US','actionChDesc','Create Backup Server');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新备份服务器','zh-HK','actionChDesc','更新備份服務器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新备份服务器','en-US','actionChDesc','Update Backup Server');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','分配备份服务器给集群','zh-HK','actionChDesc','分配備份服務器給集群');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','分配备份服务器给集群','en-US','actionChDesc','Assign Backup Server to Cluster');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除备份服务器','zh-HK','actionChDesc','刪除備份服務器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除备份服务器','en-US','actionChDesc','Delete Backup Server');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建自定义配置模板','zh-HK','actionChDesc','創建自定義配置模板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建自定义配置模板','en-US','actionChDesc','Create Custom Configuration Template');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新自定义配置模板','zh-HK','actionChDesc','更新自定義配置模板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新自定义配置模板','en-US','actionChDesc','Update Custom Configuration Template');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除自定义配置模板','zh-HK','actionChDesc','刪除自定義配置模板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除自定义配置模板','en-US','actionChDesc','Delete Custom Configuration Template');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','新增镜像仓库','zh-HK','actionChDesc','新增鏡像倉庫');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','新增镜像仓库','en-US','actionChDesc','Add Image Repository');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改镜像仓库','zh-HK','actionChDesc','修改鏡像倉庫');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改镜像仓库','en-US','actionChDesc','Modify Image Repository');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除镜像仓库','zh-HK','actionChDesc','刪除鏡像倉庫');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除镜像仓库','en-US','actionChDesc','Delete Image Repository');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询服务告警记录','zh-HK','actionChDesc','查詢服務告警記錄');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询服务告警记录','en-US','actionChDesc','Query Middleware Alert Records');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建服务告警规则','zh-HK','actionChDesc','創建服務告警規則');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建服务告警规则','en-US','actionChDesc','Create Middleware Alert Rule');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除服务告警规则','zh-HK','actionChDesc','刪除服務告警規則');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除服务告警规则','en-US','actionChDesc','Delete Middleware Alert Rule');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改服务告警规则','zh-HK','actionChDesc','修改服務告警規則');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改服务告警规则','en-US','actionChDesc','Modify Middleware Alert Rule');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除告警用户','zh-HK','actionChDesc','移除告警用戶');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除告警用户','en-US','actionChDesc','Remove Alert User');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改备份告警开关状态','zh-HK','actionChDesc','修改備份告警開關狀態');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改备份告警开关状态','en-US','actionChDesc','Modify Backup Alert Switch Status');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建全量备份','zh-HK','actionChDesc','創建全量備份');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建全量备份','en-US','actionChDesc','Create Full Backup');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建增量备份','zh-HK','actionChDesc','創建增量備份');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建增量备份','en-US','actionChDesc','Create Incremental Backup');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新增量备份','zh-HK','actionChDesc','更新增量備份');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新增量备份','en-US','actionChDesc','Update Incremental Backup');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改备份','zh-HK','actionChDesc','修改備份');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改备份','en-US','actionChDesc','Modify Backup');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除备份任务','zh-HK','actionChDesc','刪除備份任務');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除备份任务','en-US','actionChDesc','Delete Backup Task');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除备份记录','zh-HK','actionChDesc','刪除備份記錄');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除备份记录','en-US','actionChDesc','Delete Backup Record');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建备份恢复','zh-HK','actionChDesc','創建備份恢復');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建备份恢复','en-US','actionChDesc','Create Backup Recovery');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除克隆记录','zh-HK','actionChDesc','刪除克隆記錄');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除克隆记录','en-US','actionChDesc','Delete Clone Record');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新configmap','zh-HK','actionChDesc','更新configmap');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新configmap','en-US','actionChDesc','Update Configmap');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建中间件','zh-HK','actionChDesc','創建中間件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建中间件','en-US','actionChDesc','Create Middleware');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','恢复中间件','zh-HK','actionChDesc','恢復中間件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','恢复中间件','en-US','actionChDesc','Recover Middleware');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改中间件','zh-HK','actionChDesc','修改中間件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改中间件','en-US','actionChDesc','Modify Middleware');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除中间件','zh-HK','actionChDesc','刪除中間件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除中间件','en-US','actionChDesc','Delete Middleware');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除中间件相关存储','zh-HK','actionChDesc','刪除中間件相關存儲');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除中间件相关存储','en-US','actionChDesc','Delete Middleware Related Storage');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件切换','zh-HK','actionChDesc','中間件切換');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件切换','en-US','actionChDesc','Middleware Switch');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务版本升级','zh-HK','actionChDesc','服務版本升級');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务版本升级','en-US','actionChDesc','Service Version Upgrade');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务版本升级校验','zh-HK','actionChDesc','服務版本升級校驗');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','服务版本升级校验','en-US','actionChDesc','Service Version Upgrade Validation');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','重启服务','zh-HK','actionChDesc','重啟服務');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','重启服务','en-US','actionChDesc','Restart Service');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新存储','zh-HK','actionChDesc','更新存儲');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新存储','en-US','actionChDesc','Update Storage');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新自定义配置','zh-HK','actionChDesc','更新自定義配置');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新自定义配置','en-US','actionChDesc','Update Custom Configuration');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件下架','zh-HK','actionChDesc','中間件下架');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件下架','en-US','actionChDesc','Middleware Unavailable');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件上架','zh-HK','actionChDesc','中間件上架');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件上架','en-US','actionChDesc','Middleware Available');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件operator发布','zh-HK','actionChDesc','中間件operator發布');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件operator发布','en-US','actionChDesc','Middleware Operator Release');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件卸载','zh-HK','actionChDesc','中間件卸載');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件卸载','en-US','actionChDesc','Middleware Uninstall');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件更新升级','zh-HK','actionChDesc','中間件更新升級');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件更新升级','en-US','actionChDesc','Middleware Update Upgrade');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件存储扩容','zh-HK','actionChDesc','中間件存儲擴容');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件存储扩容','en-US','actionChDesc','Middleware Storage Expansion');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件存储回滚','zh-HK','actionChDesc','中間件存儲回滾');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件存储回滚','en-US','actionChDesc','Middleware Storage Rollback');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新values.yaml','zh-HK','actionChDesc','更新values.yaml');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新values.yaml','en-US','actionChDesc','Update values.yaml');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','灾备切换','zh-HK','actionChDesc','災備切換');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','灾备切换','en-US','actionChDesc','Disaster Recovery Switch');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询审计日志','zh-HK','actionChDesc','查詢審計日誌');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询审计日志','en-US','actionChDesc','Query Audit Log');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询告警记录','zh-HK','actionChDesc','查詢告警記錄');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询告警记录','en-US','actionChDesc','Query Alert Records');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','新建/接入告警对象','zh-HK','actionChDesc','新建/接入告警對象');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','新建/接入告警对象','en-US','actionChDesc','Create/Access Alert Object');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','新增告警用户','zh-HK','actionChDesc','新增告警用戶');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','新增告警用户','en-US','actionChDesc','Add Alert User');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','存储主备平台访问信息','zh-HK','actionChDesc','存儲主備平台訪問信息');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','存储主备平台访问信息','en-US','actionChDesc','Storage Master/Standby Platform Access Information');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','平台主备切换','zh-HK','actionChDesc','平台主備切換');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','平台主备切换','en-US','actionChDesc','Platform Master/Standby Switch');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','启用ldap','zh-HK','actionChDesc','啟用ldap');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','启用ldap','en-US','actionChDesc','Enable LDAP');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','连接测试','zh-HK','actionChDesc','連接測試');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','连接测试','en-US','actionChDesc','Connection Test');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','禁用ldap','zh-HK','actionChDesc','禁用ldap');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','禁用ldap','en-US','actionChDesc','Disable LDAP');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','license认证','zh-HK','actionChDesc','license認證');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','license认证','en-US','actionChDesc','License Authentication');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','登录','zh-HK','actionChDesc','登錄');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','登录','en-US','actionChDesc','Login');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','登出','zh-HK','actionChDesc','登出');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','登出','en-US','actionChDesc','Logout');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','设置钉钉机器人','zh-HK','actionChDesc','設置釘釘機器人');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','设置钉钉机器人','en-US','actionChDesc','Set DingTalk Robot');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','钉钉连接测试','zh-HK','actionChDesc','釘釘連接測試');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','钉钉连接测试','en-US','actionChDesc','DingTalk Connection Test');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除钉钉机器人','zh-HK','actionChDesc','刪除釘釘機器人');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除钉钉机器人','en-US','actionChDesc','Delete DingTalk Robot');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','设置邮箱','zh-HK','actionChDesc','設置郵箱');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','设置邮箱','en-US','actionChDesc','Set Email');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','邮箱连接测试','zh-HK','actionChDesc','郵箱連接測試');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','邮箱连接测试','en-US','actionChDesc','Email Connection Test');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建组织','zh-HK','actionChDesc','创建組織');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建组织','en-US','actionChDesc','Create Organization');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新组织信息','zh-HK','actionChDesc','更新組織信息');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新组织信息','en-US','actionChDesc','Update Organization Information');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除组织','zh-HK','actionChDesc','刪除組織');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除组织','en-US','actionChDesc','Delete Organization');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','分配资源','zh-HK','actionChDesc','分配資源');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','分配资源','en-US','actionChDesc','Assign Resources');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除组织存储配额','zh-HK','actionChDesc','移除組織存儲配額');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除组织存储配额','en-US','actionChDesc','Remove Organization Storage Quota');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除组织cpu memory配额','zh-HK','actionChDesc','移除組織cpu memory配額');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除组织cpu memory配额','en-US','actionChDesc','Remove Organization CPU Memory Quota');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除备份服务器','zh-HK','actionChDesc','移除備份服務器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除备份服务器','en-US','actionChDesc','Remove Backup Server');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加组织用户成员','zh-HK','actionChDesc','添加組織用戶成員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加组织用户成员','en-US','actionChDesc','Add Organization User Members');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新组织用户成员角色','zh-HK','actionChDesc','更新組織用戶成員角色');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新组织用户成员角色','en-US','actionChDesc','Update Organization User Member Role');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除组织用户成员角色','zh-HK','actionChDesc','移除組織用戶成員角色');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除组织用户成员角色','en-US','actionChDesc','Remove Organization User Member Role');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建项目','zh-HK','actionChDesc','創建項目');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建项目','en-US','actionChDesc','Create Project');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除项目','zh-HK','actionChDesc','刪除項目');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除项目','en-US','actionChDesc','Delete Project');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新项目','zh-HK','actionChDesc','更新項目');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新项目','en-US','actionChDesc','Update Project');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目绑定分区','zh-HK','actionChDesc','項目綁定分區');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目绑定分区','en-US','actionChDesc','Project Binding Partition');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目解绑分区','zh-HK','actionChDesc','項目解綁分區');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目解绑分区','en-US','actionChDesc','Project Unbind Partition');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目绑定成员','zh-HK','actionChDesc','項目綁定成員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目绑定成员','en-US','actionChDesc','Project Binding Member');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新项目下成员角色','zh-HK','actionChDesc','更新項目下成員角色');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新项目下成员角色','en-US','actionChDesc','Update Project Member Role');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目取消绑定成员','zh-HK','actionChDesc','項目取消綁定成員');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','项目取消绑定成员','en-US','actionChDesc','Project Unbind Member');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除项目存储配额','zh-HK','actionChDesc','移除項目存儲配額');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除项目存储配额','en-US','actionChDesc','Remove Project Storage Quota');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除项目cpu memory配额','zh-HK','actionChDesc','移除項目cpu memory配額');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除项目cpu memory配额','en-US','actionChDesc','Remove Project CPU Memory Quota');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','新增角色','zh-HK','actionChDesc','新增角色');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','新增角色','en-US','actionChDesc','Add Role');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除角色','zh-HK','actionChDesc','删除角色');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除角色','en-US','actionChDesc','Delete Role');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改角色','zh-HK','actionChDesc','修改角色');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改角色','en-US','actionChDesc','Modify Role');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建用户','zh-HK','actionChDesc','創建用戶');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建用户','en-US','actionChDesc','Create User');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改用户信息','zh-HK','actionChDesc','修改用戶信息');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改用户信息','en-US','actionChDesc','Modify User Information');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除用户','zh-HK','actionChDesc','刪除用戶');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除用户','en-US','actionChDesc','Delete User');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','重置密码','zh-HK','actionChDesc','重置密碼');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','重置密码','en-US','actionChDesc','Reset Password');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改密码','zh-HK','actionChDesc','修改密碼');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改密码','en-US','actionChDesc','Modify Password');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加个性化配置','zh-HK','actionChDesc','添加個性化配置');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加个性化配置','en-US','actionChDesc','Add Personalized Configuration');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','上传图片','zh-HK','actionChDesc','上傳圖片');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','上传图片','en-US','actionChDesc','Upload Picture');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存密码有效期天数','zh-HK','actionChDesc','保存密碼有效期天數');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存密码有效期天数','en-US','actionChDesc','Save Password Validity Days');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取用户k8s conf文件','zh-HK','actionChDesc','獲取用戶k8s conf文件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取用户k8s conf文件','en-US','actionChDesc','Get User k8s Conf File');

-- 操作审计动作内容国际化
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件登录','zh-HK','actionChDesc','中間件登錄');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件登录','en-US','actionChDesc','Middleware Login');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件登出','zh-HK','actionChDesc','中間件登出');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','中间件登出','en-US','actionChDesc','Middleware Logout');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建database','zh-HK','actionChDesc','創建database');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建database','en-US','actionChDesc','Create Database');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改database','zh-HK','actionChDesc','修改database');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改database','en-US','actionChDesc','Modify Database');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除database','zh-HK','actionChDesc','刪除database');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除database','en-US','actionChDesc','Delete Database');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取table数据','zh-HK','actionChDesc','獲取table數據');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取table数据','en-US','actionChDesc','Retrieve Table Data');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建table','zh-HK','actionChDesc','創建table');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建table','en-US','actionChDesc','Create Table');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改表信息','zh-HK','actionChDesc','修改表信息');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改表信息','en-US','actionChDesc','Modify Table Information');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除table','zh-HK','actionChDesc','刪除table');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除table','en-US','actionChDesc','Delete Table');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存数据表所有列','zh-HK','actionChDesc','保存數據表所有列');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存数据表所有列','en-US','actionChDesc','Save All Columns Of The Data Table');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存数据表所有索引','zh-HK','actionChDesc','保存數據表所有索引');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存数据表所有索引','en-US','actionChDesc','Save All The Indexes Of The Data Table');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存数据表所有外键','zh-HK','actionChDesc','保存數據表所有外鍵');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存数据表所有外键','en-US','actionChDesc','Save All Foreign Keys Of The Data Table');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新用户','zh-HK','actionChDesc','更新用戶');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新用户','en-US','actionChDesc','Update User');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改用户账号状态','zh-HK','actionChDesc','修改用戶賬號狀態');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改用户账号状态','en-US','actionChDesc','Modify The User Account State');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','授权数据库','zh-HK','actionChDesc','授權數據庫');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','授权数据库','en-US','actionChDesc','Authorize Database');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','授权数据表','zh-HK','actionChDesc','授權數據表');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','授权数据表','en-US','actionChDesc','Authorize Data Table');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','释放权限','zh-HK','actionChDesc','釋放權限');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','释放权限','en-US','actionChDesc','Release Permission');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','导出表sql','zh-HK','actionChDesc','導出表sql');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','导出表sql','en-US','actionChDesc','Export Table SQL');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','导出表结构Excel','zh-HK','actionChDesc','導出表結構Excel');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','导出表结构Excel','en-US','actionChDesc','Export Table Structure To Excel');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','执行sql','zh-HK','actionChDesc','執行sql');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','执行sql','en-US','actionChDesc','Execute SQL');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','执行sql语句','zh-HK','actionChDesc','執行sql語句');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','执行sql语句','en-US','actionChDesc','Execute SQL Statement');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新database','zh-HK','actionChDesc','更新database');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新database','en-US','actionChDesc','Update Database');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建schema','zh-HK','actionChDesc','創建schema');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建schema','en-US','actionChDesc','Create Schema');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新schema','zh-HK','actionChDesc','更新schema');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新schema','en-US','actionChDesc','Update Schema');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除schema','zh-HK','actionChDesc','刪除schema');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除schema','en-US','actionChDesc','Delete Schema');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新table','zh-HK','actionChDesc','更新table');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新table','en-US','actionChDesc','Update Table');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新column','zh-HK','actionChDesc','更新column');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新column','en-US','actionChDesc','Update Column');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取建表语句','zh-HK','actionChDesc','獲取建表語句');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取建表语句','en-US','actionChDesc','Get The Table Building Statement');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取table excel','zh-HK','actionChDesc','獲取table excel');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','获取table excel','en-US','actionChDesc','Retrieve The Excel Table');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删外键约束','zh-HK','actionChDesc','增刪外鍵約束');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删外键约束','en-US','actionChDesc','Add Or Delete Foreign Key Constraint');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删排它约束','zh-HK','actionChDesc','增刪排它約束');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删排它约束','en-US','actionChDesc','Add Or Remove Exclusion Constraint');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删唯一约束','zh-HK','actionChDesc','增刪唯一約束');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删唯一约束','en-US','actionChDesc','Add Or Delete Unique Constraint');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删检查约束','zh-HK','actionChDesc','增刪檢查約束');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删检查约束','en-US','actionChDesc','Add Or Delete Check Constraint');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删继承关系','zh-HK','actionChDesc','增刪繼承關係');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','增删继承关系','en-US','actionChDesc','Add Or Delete Inheritance Relationship');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加用户权限','zh-HK','actionChDesc','添加用戶權限');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加用户权限','en-US','actionChDesc','Add User Permission');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','取消用户权限','zh-HK','actionChDesc','取消用戶權限');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','取消用户权限','en-US','actionChDesc','Cancel User Permission');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','启用/禁用用户','zh-HK','actionChDesc','啓用/禁用用戶');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','启用/禁用用户','en-US','actionChDesc','Enable/Disable User');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建key-value','zh-HK','actionChDesc','創建key-value');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建key-value','en-US','actionChDesc','Create key value');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新value','zh-HK','actionChDesc','更新value');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新value','en-US','actionChDesc','Update value');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除key','zh-HK','actionChDesc','刪除key');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除key','en-US','actionChDesc','Delete key');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改key名','zh-HK','actionChDesc','修改key名');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改key名','en-US','actionChDesc','Change key name');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改key过期时间','zh-HK','actionChDesc','修改key過期時間');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改key过期时间','en-US','actionChDesc','Modify The Expiration Time Of The Key');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除value','zh-HK','actionChDesc','刪除value');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除value','en-US','actionChDesc','Delete value');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','执行cmd','zh-HK','actionChDesc','執行cmd');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','执行cmd','en-US','actionChDesc','Execute cmd');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询sql历史记录','zh-HK','actionChDesc','查詢sql歷史記錄');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询sql历史记录','en-US','actionChDesc','Query SQL History');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','划分可用区','zh-HK','actionChDesc','劃分可用區');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','划分可用区','en-US','actionChDesc','Divide The Available Area');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除节点','zh-HK','actionChDesc','移除節點');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','移除节点','en-US','actionChDesc','Remove Node');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新可用区','zh-HK','actionChDesc','更新可用區');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新可用区','en-US','actionChDesc','Update Availability Zone');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','部署集群组件','zh-HK','actionChDesc','部署集羣組件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','部署集群组件','en-US','actionChDesc','Deploy Cluster Component');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除集群组件','zh-HK','actionChDesc','刪除集羣組件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除集群组件','en-US','actionChDesc','Delete Cluster Component');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','批量部署集群组件','zh-HK','actionChDesc','批量部署集羣組件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','批量部署集群组件','en-US','actionChDesc','Batch Deployment Of Cluster Component');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新集群组件信息','zh-HK','actionChDesc','更新集羣組件信息');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新集群组件信息','en-US','actionChDesc','Update Cluster Component Information');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','对接集群组件','zh-HK','actionChDesc','對接集羣組件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','对接集群组件','en-US','actionChDesc','Integrate Cluster Component');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加集群','zh-HK','actionChDesc','添加集羣');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加集群','en-US','actionChDesc','Add Cluster');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改集群','zh-HK','actionChDesc','修改集羣');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改集群','en-US','actionChDesc','Modify Cluster');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除集群','zh-HK','actionChDesc','刪除集羣');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除集群','en-US','actionChDesc','Delete Cluster');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询平台集群资源配额情况','zh-HK','actionChDesc','查詢平臺集羣資源配額情況');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询平台集群资源配额情况','en-US','actionChDesc','Query The Platform Cluster Resource Quota Situation');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加集群ingress','zh-HK','actionChDesc','添加集羣ingress');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加集群ingress','en-US','actionChDesc','Add Cluster Ingress');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','对接集群ingress组件','zh-HK','actionChDesc','對接集羣ingress組件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','对接集群ingress组件','en-US','actionChDesc','Integrate Cluster Ingress Component');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新集群ingress组件信息','zh-HK','actionChDesc','更新集羣ingress組件信息');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新集群ingress组件信息','en-US','actionChDesc','Update Cluster Ingress Component Information');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除指定ingress组件','zh-HK','actionChDesc','刪除指定ingress組件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除指定ingress组件','en-US','actionChDesc','Delete The Specified Ingress Component');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','重启pod','zh-HK','actionChDesc','重啓pod');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','重启pod','en-US','actionChDesc','Restart Pod');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存ingress yaml','zh-HK','actionChDesc','保存ingress yaml');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','保存ingress yaml','en-US','actionChDesc','Save Ingress YAML');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建中间件对外访问','zh-HK','actionChDesc','創建中間件對外訪問');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建中间件对外访问','en-US','actionChDesc','Create External Access For Middleware');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除中间件对外访问','zh-HK','actionChDesc','刪除中間件對外訪問');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除中间件对外访问','en-US','actionChDesc','Remove External Access For Middleware');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建命名空间','zh-HK','actionChDesc','創建命名空間');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建命名空间','en-US','actionChDesc','Create Namespace');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除命名空间','zh-HK','actionChDesc','刪除命名空間');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除命名空间','en-US','actionChDesc','Delete Namespace');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新分区信息','zh-HK','actionChDesc','更新分區信息');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新分区信息','en-US','actionChDesc','Update Partition Information');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改分区可用域状态','zh-HK','actionChDesc','修改分區可用域狀態');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','修改分区可用域状态','en-US','actionChDesc','Modify The Available Domain Status Of The Partition');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','绑定/解绑项目','zh-HK','actionChDesc','綁定/解綁項目');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','绑定/解绑项目','en-US','actionChDesc','Bind/Unbind Project');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','pod主机迁移','zh-HK','actionChDesc','pod主機遷移');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','pod主机迁移','en-US','actionChDesc','Pod Host Migration');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','屏蔽迁移信息','zh-HK','actionChDesc','屏蔽遷移信息');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','屏蔽迁移信息','en-US','actionChDesc','Block Migration Information');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建/修改分区配额','zh-HK','actionChDesc','創建/修改分區配額');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','创建/修改分区配额','en-US','actionChDesc','Create/Modify Partition Quotas');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加存储','zh-HK','actionChDesc','添加存儲');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','添加存储','en-US','actionChDesc','Add Storage');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除存储','zh-HK','actionChDesc','刪除存儲');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','删除存储','en-US','actionChDesc','Delete Storage');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新存储信息','zh-HK','actionChDesc','更新存儲信息');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','更新存储信息','en-US','actionChDesc','Update Storage Information');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','yaml格式校验','zh-HK','actionChDesc','yaml格式校驗');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','yaml格式校验','en-US','actionChDesc','YAML Format Verification');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询日志','zh-HK','actionChDesc','查詢日誌');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询日志','en-US','actionChDesc','Query Logs');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询pod日志文件列表','zh-HK','actionChDesc','查詢pod日誌文件列表');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('operation_audit','查询pod日志文件列表','en-US','actionChDesc','Query The List Of Pod Log Files');

-- 服务暴露purpose国际化
delete from sys_resource_translate_config where `group_name` = 'servicePurpose';
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','只读','zh-HK','servicePurpose','只讀');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','只读','en-US','servicePurpose','Read Only');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','读写分离','zh-HK','servicePurpose','讀寫分離');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','读写分离','en-US','servicePurpose','R/W split Mode');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','读写','zh-HK','servicePurpose','讀寫');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','读写','en-US','servicePurpose','Read-Write');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','哨兵','zh-HK','servicePurpose','哨兵');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','哨兵','en-US','servicePurpose','Sentinel');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','管理页面','zh-HK','servicePurpose','管理頁面');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','管理页面','en-US','servicePurpose','Management');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','服务连接','zh-HK','servicePurpose','服務連接');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','服务连接','en-US','servicePurpose','Service Connection');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','集群外访问','zh-HK','servicePurpose','集群外訪問');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','集群外访问','en-US','servicePurpose','Access from outside the cluster');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','服务代理proxy-0','zh-HK','servicePurpose','服務代理proxy-0');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','服务代理proxy-0','en-US','servicePurpose','Service proxy 0 port configuration');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','服务代理proxy-1','zh-HK','servicePurpose','服務代理proxy-1');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('servicePurpose','服务代理proxy-1','en-US','servicePurpose','Service proxy 1 port configuration');

UPDATE sys_resource_translate_config set translation='R/W split' where group_name='servicePurpose' and unique_value='读写分离'
and language_code='en-US' and property='servicePurpose' and translation='R/W split Mode';

UPDATE sys_resource_translate_config set translation='Read-Only' where group_name='servicePurpose' and unique_value='只读'
and language_code='en-US' and property='servicePurpose' and translation='Read Only';

-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','platform','zh-HK','alias_name','平臺系統監控');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','platform','en-US','alias_name','Platform System Monitoring');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','logCollect','zh-HK','alias_name','日誌採集');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','logCollect','en-US','alias_name','Log Capture');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','elasticsearch','zh-HK','alias_name','日誌組件');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','elasticsearch','en-US','alias_name','Log component');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','minio','zh-HK','alias_name','備份存儲');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','minio','en-US','alias_name','Backup Storage');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','middlewareController','zh-HK','alias_name','中間件控制器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','middlewareController','en-US','alias_name','Middleware Operator');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','middlewareBackupController','zh-HK','alias_name','備份控制器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','middlewareBackupController','en-US','alias_name','Backup Controller');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','middlewareScheduler','zh-HK','alias_name','擴展調度器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','middlewareScheduler','en-US','alias_name','Extended Scheduler');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','lvmController','zh-HK','alias_name','LVM控制器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','lvmController','en-US','alias_name','LVM Controller');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','localPathController','zh-HK','alias_name','Local-Path控制器');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','localPathController','en-US','alias_name','Local-Path Controller');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','grafana','zh-HK','alias_name','監控面板');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','grafana','en-US','alias_name','Monitor Dashboard');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','prometheus','zh-HK','alias_name','數據監控');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','prometheus','en-US','alias_name','Monitoring');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','alertmanager','zh-HK','alias_name','監控告警');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','alertmanager','en-US','alias_name','Monitoring Alerts');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','network','zh-HK','alias_name','網絡監控');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','network','en-US','alias_name','Network Monitoring');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','cluster','zh-HK','alias_name','集羣監控');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('alert_target','cluster','en-US','alias_name','Cluster Monitoring');

update personal_config set platform_name='Zeus | CloudDB',platform_alias_name='CloudDB' where platform_alias_name='Middleware Management Integration Platform';

-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('license_info','正式版','zh-HK','type','正式版');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('license_info','正式版','en-US','type','Standard Edition');
-- 繁体中文
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('license_info','试用版','zh-HK','type','試用版');
-- 英语
insert into sys_resource_translate_config(`group_name`,`unique_value`,`language_code`,`property`,`translation`)
values('license_info','试用版','en-US','type','Trial Version');