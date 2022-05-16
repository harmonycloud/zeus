create table `active_area` (
    `id`         int auto_increment comment '自增id',
    `cluster_id` varchar(64) CHARACTER SET utf8mb4 null comment '集群id',
    `area_name`  varchar(64) CHARACTER SET utf8mb4 null comment '可用区名称',
    `alias_name` varchar(128) CHARACTER SET utf8mb4 null comment '可用区中文别名',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 comment '可用区'