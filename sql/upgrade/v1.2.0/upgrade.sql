-- 2022.8.31 liyinlong
-- 修改分区字段长度
ALTER TABLE `alert_rule_id` MODIFY `namespace` VARCHAR(256);
-- 添加ingress类型字段
ALTER TABLE `cluster_ingress_components` ADD COLUMN `type` VARCHAR(32) DEFAULT NULL COMMENT "ingress类型 nginx或traefik";
UPDATE `cluster_ingress_components` SET `type`="nginx" WHERE `type` != "traefik";