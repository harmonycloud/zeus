-- 2021.12.22 xutianhong
-- 总定义配置模板优化
ALTER TABLE `middleware_platform`.`custom_config_template`
DROP COLUMN `alias_name`,
ADD COLUMN `uid` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板uid' AFTER `id`;

-- 2021.12.27 xutianhong
-- 自定义配置表相关字段长度优化
ALTER TABLE `custom_config`
MODIFY COLUMN `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段名称' AFTER `id`,
MODIFY COLUMN `default_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '默认值' AFTER `chart_name`,
MODIFY COLUMN `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '描述' AFTER `ranges`;

-- 配置目标表添加字段
ALTER TABLE `custom_config_template`
ADD COLUMN `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '模板描述' AFTER `type`;