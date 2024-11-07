ALTER TABLE backup_position ADD backup_server_detail_id int NOT NULL comment '备份服务器详情id';

CREATE TEMPORARY TABLE temp_ids AS
SELECT  t1.name, t1.organ_id, t1.project_id, t1.backup_server_id, t1.backup_position, t1.create_time, t2.id AS backup_server_detail_id
FROM backup_position t1
         JOIN backup_server_detail t2 ON t1.backup_server_id = t2.backup_server_id;

SELECT * FROM temp_ids;

DELETE FROM backup_position WHERE TRUE;

INSERT INTO backup_position(name, organ_id, project_id, backup_server_id, backup_position, create_time, backup_server_detail_id)
SELECT * FROM temp_ids;

DROP TABLE temp_ids;