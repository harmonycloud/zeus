-- 2021.11.12 xutianhong
-- 新增集群组件表
DROP TABLE IF EXISTS `cluster_components`;
CREATE TABLE `cluster_components` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `cluster_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '集群Id',
  `component` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '组件名称',
  `status` int(11) NULL COMMENT '0-未安装接入 1-已接入 2-安装中 3-运行正常 4-运行异常 5-卸载中',
	PRIMARY KEY (`id`)
) COMMENT = '集群组件表';