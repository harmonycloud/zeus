-- 2022.6.9 xutianhong
-- ingress组件表修改
alter table cluster_ingress_components add namespace varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci null comment '分区' after cluster_id;
alter table cluster_ingress_components add address varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci null comment '访问地址' after namespace;
alter table cluster_ingress_components add config_map_name varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci null comment 'tcp配置文件名称' after address;