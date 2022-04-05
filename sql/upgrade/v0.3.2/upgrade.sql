-- 2022.3.23 yushuaikang
-- 个性化表新增tab栏图标自定义
ALTER TABLE `personal_config`
ADD COLUMN `tab_logo_path` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT 'tab栏logo地址' AFTER `home_logo_path`,
ADD COLUMN `tab_logo` mediumblob COMMENT 'tab栏logo' AFTER `home_logo_path`;

-- 2022.3.25 yushuaikang
-- 新增集群表
CREATE TABLE `middleware_cluster` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `clusterId` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT '集群ID',
  `middleware_cluster` text COLLATE utf8_bin COMMENT '集群对象',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- 2022.4.5 liyinlong
-- 新增mysql数据库表
CREATE TABLE `mysql_db` (
                            `id` int(11) NOT NULL AUTO_INCREMENT,
                            `mysql_qualified_name` varchar(512) NOT NULL COMMENT 'mysql服务限定名',
                            `db` char(64) NOT NULL COMMENT '数据库名',
                            `createtime` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
                            `description` varchar(512) DEFAULT NULL COMMENT '备注',
                            `charset` varchar(32) NOT NULL COMMENT '字符集',
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='mysql数据库';

-- 2022.4.5 liyinlong
-- 新增mysql用户表
CREATE TABLE `mysql_user` (
                              `id` int(11) NOT NULL AUTO_INCREMENT,
                              `mysql_qualified_name` varchar(512) NOT NULL COMMENT 'mysql服务限定名',
                              `user` char(32) NOT NULL COMMENT '用户名',
                              `password` varchar(255) NOT NULL COMMENT '密码',
                              `createtime` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
                              `description` varchar(512) DEFAULT NULL COMMENT '备注',
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='mysql用户';

-- 2022.4.5 liyinlong
-- 新增mysql数据库授权表
CREATE TABLE `mysql_db_priv` (
                                 `id` int(11) NOT NULL AUTO_INCREMENT,
                                 `mysql_qualified_name` varchar(512) NOT NULL COMMENT 'mysql服务限定名',
                                 `db` char(64) NOT NULL COMMENT '数据库名',
                                 `user` char(32) NOT NULL COMMENT '用户名',
                                 `authority` int(11) NOT NULL COMMENT '权限：1：只读，2：读写，3：仅DDL，4：仅DML',
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Mysql数据库授权';
