-- 依据数据库生成的状态/审核日志校正历史 UTC 混用时间。
-- 执行前备份数据库。数据库日志采用 Asia/Shanghai，本脚本不按固定八小时盲目平移。
START TRANSACTION;
UPDATE service_case c JOIN (
  SELECT case_id, MAX(created_at) AS event_time FROM case_status_log
  WHERE after_status = 'PENDING_REVIEW' GROUP BY case_id
) l ON l.case_id = c.id SET c.submitted_at = l.event_time, c.updated_at = GREATEST(c.updated_at, l.event_time)
WHERE c.submitted_at IS NOT NULL AND ABS(TIMESTAMPDIFF(SECOND,c.submitted_at,l.event_time)) BETWEEN 28790 AND 28810;
UPDATE service_case c JOIN (
  SELECT case_id, MAX(created_at) AS event_time FROM case_status_log
  WHERE after_status = 'COMPLETED' GROUP BY case_id
) l ON l.case_id = c.id SET c.completed_at = l.event_time, c.updated_at = GREATEST(c.updated_at,l.event_time)
WHERE c.completed_at IS NOT NULL AND ABS(TIMESTAMPDIFF(SECOND,c.completed_at,l.event_time)) BETWEEN 28790 AND 28810;
UPDATE material_submission m JOIN (
  SELECT material_id, MAX(reviewed_at) AS event_time FROM material_review_log GROUP BY material_id
) l ON l.material_id = m.id SET m.reviewed_at = l.event_time, m.updated_at = m.updated_at
WHERE m.reviewed_at IS NOT NULL AND ABS(TIMESTAMPDIFF(SECOND,m.reviewed_at,l.event_time)) BETWEEN 28790 AND 28810;
UPDATE supplement_request r JOIN (
  SELECT case_id, MAX(created_at) AS event_time FROM case_status_log
  WHERE after_status = 'PENDING_REVIEW' GROUP BY case_id
) l ON l.case_id = r.case_id SET r.resolved_at = l.event_time
WHERE r.resolved_at IS NOT NULL AND ABS(TIMESTAMPDIFF(SECOND,r.resolved_at,l.event_time)) BETWEEN 28790 AND 28810;
UPDATE credential_handover h JOIN (
  SELECT business_id, MAX(created_at) AS event_time FROM operation_log
  WHERE operation_code = 'CREDENTIAL_HANDOVER_UPDATE' AND business_type = 'CASE' GROUP BY business_id
) l ON l.business_id = h.case_id SET h.handled_at = l.event_time, h.updated_at = h.updated_at
WHERE ABS(TIMESTAMPDIFF(SECOND,h.handled_at,l.event_time)) BETWEEN 28790 AND 28810;
-- 任务开始时间由数据库产生，结束时间不可能早于开始时间；仅修复接近八小时的反向偏差。
UPDATE export_job SET expires_at = DATE_ADD(expires_at, INTERVAL 8 HOUR), completed_at = DATE_ADD(completed_at, INTERVAL 8 HOUR)
WHERE TIMESTAMPDIFF(SECOND,completed_at,started_at) BETWEEN 28500 AND 28800;
UPDATE import_job SET completed_at = DATE_ADD(completed_at, INTERVAL 8 HOUR)
WHERE TIMESTAMPDIFF(SECOND,completed_at,started_at) BETWEEN 28500 AND 28800;
COMMIT;
