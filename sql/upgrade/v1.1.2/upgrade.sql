-- 2022.7.12 liyinlong
-- 添加告警设置表
CREATE TABLE `alert_setting`
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `cluster_id`        varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '集群id',
    `namespace`         varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '分区名称',
    `middleware_name`   varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '服务名称',
    `lay`               varchar(32) COLLATE utf8_bin NOT NULL COMMENT '告警等级 service:服务告警 system:系统告警',
    `enable_ding_alert` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '是否开启钉钉告警',
    `enable_mail_alert` varchar(45) COLLATE utf8_bin NOT NULL COMMENT '是否开启邮件告警',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='告警设置';
-- 修改ldap表名为系统表，此表将存储key-value类型的数据
ALTER TABLE `ldap_config` RENAME `system_config`;
-- 添加告警设置id字段
ALTER TABLE `mail_to_user` ADD COLUMN `alert_setting_id` INT(11) COMMENT '告警设置id';

