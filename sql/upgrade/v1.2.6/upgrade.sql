-- 2023.01.10 wangpenglei
-- 修改命名空间长度
ALTER TABLE alert_record modify namespace VARCHAR(128) null comment '分区';