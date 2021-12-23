-- 2021.12.22 xutianhong
-- 总定义配置模板优化
ALTER TABLE `middleware_platform`.`custom_config_template`
DROP COLUMN `alias_name`,
ADD COLUMN `uid` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板uid' AFTER `id`;