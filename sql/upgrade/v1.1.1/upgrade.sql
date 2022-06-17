-- 2022.6.9 xutianhong
-- 添加中间件类型映照表
DROP TABLE IF EXISTS `middleware_cr_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `middleware_cr_type` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `chart_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'chart包名称',
  `cr_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'cr类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='中间件类型映照表';

-- 2022.6.15 yushuaikang
-- 备份位置表
DROP TABLE IF EXISTS `middleware_backup_address`;
CREATE TABLE `middleware_backup_address` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '中文名称',
  `type` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '类型',
  `relevance_num` int(11) DEFAULT NULL COMMENT '关联数',
  `status` int(11) DEFAULT NULL COMMENT '状态',
  `bucket_name` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'bucket名称',
  `access_key_id` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '用户ID',
  `secret_access_key` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '密码',
  `capacity` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '容量',
  `endpoint` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '地址',
  `ftp_host` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'FTP主机服务器',
  `ftp_user` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'FTP登录用户名',
  `ftp_password` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'FTP登录密码',
  `ftp_port` int(11) DEFAULT NULL COMMENT 'FTP端口',
  `server_host` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '服务器地址',
  `server_user` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '服务器用户名',
  `server_password` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '服务器密码',
  `server_port` int(11) DEFAULT NULL COMMENT '服务器端口',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8 COLLATE=utf8_bin

-- 2022.6.15 yushuaikang
-- 备份位置集群映射表
DROP TABLE IF EXISTS `backup_address_cluster`;
CREATE TABLE `backup_address_cluster` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `backup_address_id` int(11) DEFAULT NULL COMMENT '备份地址ID',
  `cluster_id` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '集群ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8 COLLATE=utf8_bin