ALTER TABLE `cluster_ingress_components` ADD COLUMN `type` VARCHAR(32) DEFAULT NULL COMMENT "ingress类型 nginx或traefik";