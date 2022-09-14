-- 2022.8.31 liyinlong
-- 修改分区字段长度
ALTER TABLE `alert_rule_id` MODIFY `namespace` VARCHAR(256);
-- 添加ingress类型字段
ALTER TABLE `cluster_ingress_components` ADD COLUMN `type` VARCHAR(32) DEFAULT NULL COMMENT "ingress类型 nginx或traefik";
UPDATE `cluster_ingress_components` SET `type`="nginx" WHERE `type` != "traefik";

-- 2022.09.14 liyinlong
-- 添加可用区表
DROP TABLE IF EXISTS `active_area`;
CREATE TABLE `active_area` (
                               `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增id',
                               `cluster_id` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '集群id',
                               `area_name` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '可用区名称',
                               `alias_name` varchar(128) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '可用区中文别名',
                               `init` tinyint(1) DEFAULT NULL COMMENT '是否初始化',
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='可用区';