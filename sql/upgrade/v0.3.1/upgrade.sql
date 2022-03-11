-- 2022.3.10
-- 新增镜像仓库表
DROP TABLE IF EXISTS `mirror_image`;
CREATE TABLE `mirror_image` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cluster_id` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '集群',
  `namespace` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '命名空间',
  `protocol` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '协议',
  `address` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'harbor地址',
  `host_address` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT 'harbor主机地址',
  `port` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '端口',
  `project` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'harbor项目',
  `username` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '用户名',
  `password` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '密码',
  `description` varchar(512) COLLATE utf8_bin DEFAULT NULL COMMENT '描述',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='镜像仓库表';
