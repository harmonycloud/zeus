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
