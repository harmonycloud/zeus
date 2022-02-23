-- 2022.2.23 liyinlong
-- 请求ip字段长度改为128
ALTER TABLE `operation_audit`
    MODIFY COLUMN `remote_ip` char(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求ip' AFTER `response`;