-- 2023.01.10 wangpenglei
-- 修改命名空间长度
ALTER TABLE alert_record modify namespace VARCHAR(128) null comment '分区';
ALTER TABLE alert_rule_id modify namespace VARCHAR(64) null comment '命名空间';

-- 2023.01.31 liyinlong
-- 删除备份服务器地址表
DROP TABLE IF EXISTS `middleware_backup_address`;
-- 修改菜单
update middleware_platform.resource_menu
set alias_name = "备份服务器",
    `name`="backupServer",
    `url`      = "backupService/backupServer"
where id = 13;
-- 新增表
DROP TABLE IF EXISTS `backup_server_detail`;
CREATE TABLE `backup_server_detail` (
    `id`               int(11) NOT NULL AUTO_INCREMENT,
    `backup_server_id` int(11) NOT NULL COMMENT '备份服务器id',
    `server_usage`     varchar(32) COLLATE utf8_bin  DEFAULT NULL COMMENT '用途：A可用区A,B:可用区B',
    `type`             int(11)                       DEFAULT NULL COMMENT '类型：1: S3. 2: ftp: 3: server',
    `protocol`         varchar(45) COLLATE utf8_bin  DEFAULT NULL COMMENT '协议',
    `host`             varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT '主机',
    `port`             varchar(45) COLLATE utf8_bin  DEFAULT NULL COMMENT '端口',
    `username`         varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT '用户名',
    `password`         varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT '密码',
    `create_time`      varchar(45) COLLATE utf8_bin  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 17 DEFAULT CHARSET = utf8 COLLATE = utf8_bin COMMENT ='备份服务器详情';
-- 新增表
DROP TABLE IF EXISTS `backup_server`;
CREATE TABLE `backup_server`(
    `id`          int(11)                       NOT NULL AUTO_INCREMENT,
    `name`        varchar(512) COLLATE utf8_bin NOT NULL COMMENT '备份服务器名称',
    `cluster_id`  varchar(256) COLLATE utf8_bin      DEFAULT NULL COMMENT '所属集群',
    `type`        int(11)                       NOT NULL COMMENT '备份服务器类型（1:普通，2:双活）',
    `create_time` timestamp                     NULL DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 13 DEFAULT CHARSET = utf8 COLLATE = utf8_bin COMMENT ='备份服务器';
-- 新增表
DROP TABLE IF EXISTS `project_backup_server`;
CREATE TABLE `project_backup_server`(
    `id`               int(11)                       NOT NULL AUTO_INCREMENT,
    `project_id`       varchar(128) COLLATE utf8_bin NOT NULL COMMENT '项目id',
    `backup_server_id` int(11)                       NOT NULL COMMENT '备份服务器id',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 9 DEFAULT CHARSET = utf8 COLLATE = utf8_bin COMMENT ='项目备份服务器关联表';
-- 新增表
DROP TABLE IF EXISTS `backup_position`;
CREATE TABLE `backup_position`(
    `id`               int(11)                       NOT NULL AUTO_INCREMENT,
    `name`             varchar(512) COLLATE utf8_bin NOT NULL COMMENT '备份位置名称',
    `project_id`       varchar(128) COLLATE utf8_bin NOT NULL COMMENT '项目id',
    `backup_server_id` int(11)                       NOT NULL COMMENT '备份服务器id',
    `backup_position`  varchar(512) COLLATE utf8_bin NOT NULL COMMENT '备份路径（对于minio则是bucket）',
    `create_time`      timestamp                     NULL DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 5 DEFAULT CHARSET = utf8 COLLATE = utf8_bin COMMENT ='备份位置表';
