-- xutianhong
-- 2022.10.21 添加sql执行记录表
DROP TABLE IF EXISTS `sql_execute_record`;
CREATE TABLE `sql_execute_record` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '自增Id',
    `database` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目标database',
    `sql` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行sql',
    `line` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行数',
    `time` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '耗时',
    `message` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '信息',
    `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
    `date` timestamp NULL DEFAULT NULL COMMENT '执行时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='sql执行表';

-- wangpenglei
-- 2022.10.27  修改任务名称长度
alter table backup_name modify backup_name varchar(128) null comment '备份任务名称';