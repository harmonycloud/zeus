-- xutianhong
-- 2022.10.21 添加sql执行记录表
DROP TABLE IF EXISTS `sql_execute_record`;
CREATE TABLE `sql_execute_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cluster_id` varchar(256) COLLATE utf8_bin DEFAULT NULL,
  `namespace` varchar(256) COLLATE utf8_bin DEFAULT NULL,
  `middleware_name` varchar(256) COLLATE utf8_bin DEFAULT NULL,
  `target_database` varchar(256) COLLATE utf8_bin DEFAULT NULL,
  `sqlstr` varchar(1024) COLLATE utf8_bin DEFAULT NULL COMMENT '执行sql',
  `exec_status` varchar(45) COLLATE utf8_bin DEFAULT NULL COMMENT '执行状态',
  `exec_date` timestamp NULL DEFAULT NULL COMMENT '执行时间',
  `exec_time` varchar(45) COLLATE utf8_bin DEFAULT NULL COMMENT '耗时',
  `message` varchar(45) COLLATE utf8_bin DEFAULT NULL COMMENT '信息',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='sql执行记录表';

-- wangpenglei
-- 2022.10.27  修改任务名称长度
alter table backup_name modify backup_name varchar(128) null comment '备份任务名称';

-- xutianhong
-- 2022.11.21 修改集群组件表
alter table cluster_components add protocol varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '协议' after component;
alter table cluster_components add host varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址' after protocol;
alter table cluster_components add port int null COMMENT '端口' after host;
alter table cluster_components add username varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名' after port;
alter table cluster_components add password varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码' after username;


