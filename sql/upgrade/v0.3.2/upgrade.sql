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
