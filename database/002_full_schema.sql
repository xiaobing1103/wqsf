-- 万企商服通完整业务模型增量脚本
-- 适用：MySQL 8.4 / utf8mb4
-- 说明：本脚本面向开发骨架库。生产环境执行前请先备份并核对迁移计划。
-- 执行策略：在 001_init.sql 之后执行一次；本脚本包含 ALTER TABLE，不是无条件可重复执行脚本。

CREATE DATABASE IF NOT EXISTS wqst DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE wqst;

SET NAMES utf8mb4;
SET time_zone = '+08:00';

-- 1. 账号与权限
CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  account_type VARCHAR(16) NOT NULL COMMENT 'ADMIN/CLIENT',
  username VARCHAR(64) NULL,
  password_hash VARCHAR(255) NULL,
  wechat_openid VARCHAR(128) NULL,
  wechat_unionid VARCHAR(128) NULL,
  display_name VARCHAR(64) NOT NULL,
  phone_cipher VARCHAR(512) NULL,
  phone_hash CHAR(64) NULL,
  phone_mask VARCHAR(32) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED/LOCKED',
  last_login_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT UNSIGNED NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_openid (wechat_openid),
  UNIQUE KEY uk_user_phone_hash (phone_hash),
  KEY idx_user_type_status (account_type, status)
) ENGINE=InnoDB COMMENT='统一用户账号';

CREATE TABLE IF NOT EXISTS role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色';

CREATE TABLE IF NOT EXISTS permission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  permission_code VARCHAR(128) NOT NULL,
  permission_name VARCHAR(128) NOT NULL,
  permission_type VARCHAR(16) NOT NULL DEFAULT 'BUTTON' COMMENT 'MENU/BUTTON/API',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB COMMENT='权限';

CREATE TABLE IF NOT EXISTS user_role (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, role_id),
  KEY idx_user_role_role (role_id)
) ENGINE=InnoDB COMMENT='用户角色';

CREATE TABLE IF NOT EXISTS role_permission (
  role_id BIGINT UNSIGNED NOT NULL,
  permission_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (role_id, permission_id),
  KEY idx_role_permission_permission (permission_id)
) ENGINE=InnoDB COMMENT='角色权限';

-- 2. 兼容旧 company 表并补充字段
ALTER TABLE company
  ADD COLUMN legal_person VARCHAR(64) NULL AFTER contact_phone,
  ADD COLUMN contact_phone_cipher VARCHAR(512) NULL AFTER legal_person,
  ADD COLUMN contact_phone_hash CHAR(64) NULL AFTER contact_phone_cipher,
  ADD COLUMN contact_phone_mask VARCHAR(32) NULL AFTER contact_phone_hash,
  ADD COLUMN company_email VARCHAR(128) NULL AFTER city,
  ADD COLUMN detail_address VARCHAR(255) NULL AFTER company_email,
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' AFTER detail_address,
  ADD COLUMN deleted_at DATETIME(3) NULL AFTER updated_at,
  ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 AFTER deleted_at;

CREATE TABLE IF NOT EXISTS company_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  company_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  member_role VARCHAR(32) NOT NULL DEFAULT 'CONTACT' COMMENT 'OWNER/CONTACT/VIEWER',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_company_user (company_id, user_id),
  KEY idx_company_user_user (user_id, status)
) ENGINE=InnoDB COMMENT='企业成员关系';

-- 3. 产品与资料模板
CREATE TABLE IF NOT EXISTS service_module (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  module_code VARCHAR(32) NOT NULL,
  module_name VARCHAR(64) NOT NULL,
  description VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_module_code (module_code)
) ENGINE=InnoDB COMMENT='服务模块';

CREATE TABLE IF NOT EXISTS material_template (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(128) NOT NULL,
  version_no INT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/ARCHIVED',
  created_by BIGINT UNSIGNED NULL,
  published_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_version (template_code, version_no),
  KEY idx_template_status (template_code, status)
) ENGINE=InnoDB COMMENT='资料模板';

CREATE TABLE IF NOT EXISTS material_template_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  input_type VARCHAR(20) NOT NULL COMMENT 'FILE/TEXT/URL/FORM_GROUP',
  is_required TINYINT(1) NOT NULL DEFAULT 1,
  is_sensitive TINYINT(1) NOT NULL DEFAULT 0,
  allowed_extensions VARCHAR(255) NULL,
  max_file_size_mb INT UNSIGNED NULL,
  max_file_count INT UNSIGNED NOT NULL DEFAULT 1,
  help_text VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_item_code (template_id, item_code),
  KEY idx_template_item_order (template_id, sort_order)
) ENGINE=InnoDB COMMENT='资料模板明细';

CREATE TABLE IF NOT EXISTS service_product (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  module_id BIGINT UNSIGNED NOT NULL,
  product_code VARCHAR(64) NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  description VARCHAR(500) NULL,
  template_id BIGINT UNSIGNED NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/UNPUBLISHED',
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT UNSIGNED NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_code (product_code),
  KEY idx_product_module_status (module_id, status, sort_order)
) ENGINE=InnoDB COMMENT='服务产品';

-- 4. 兼容旧报单、开票和资料表
ALTER TABLE service_case
  MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  ADD COLUMN product_id BIGINT UNSIGNED NULL AFTER company_id,
  ADD COLUMN applicant_user_id BIGINT UNSIGNED NULL AFTER assignee_id,
  ADD COLUMN company_name_snapshot VARCHAR(128) NULL AFTER applicant_user_id,
  ADD COLUMN legal_person_name VARCHAR(64) NULL AFTER company_name_snapshot,
  ADD COLUMN contact_phone_cipher VARCHAR(512) NULL AFTER legal_person_name,
  ADD COLUMN contact_phone_hash CHAR(64) NULL AFTER contact_phone_cipher,
  ADD COLUMN contact_phone_mask VARCHAR(32) NULL AFTER contact_phone_hash,
  ADD COLUMN case_month CHAR(7) NULL AFTER contact_phone_mask,
  ADD COLUMN completed_at DATETIME(3) NULL AFTER submitted_at,
  ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 AFTER updated_at,
  ADD COLUMN deleted_at DATETIME(3) NULL AFTER version;

ALTER TABLE invoice_info
  ADD COLUMN tax_no_cipher VARCHAR(512) NULL AFTER company_name,
  ADD COLUMN tax_no_mask VARCHAR(64) NULL AFTER tax_no_cipher,
  ADD COLUMN contact_phone_cipher VARCHAR(512) NULL AFTER contact_phone,
  ADD COLUMN contact_phone_hash CHAR(64) NULL AFTER contact_phone_cipher,
  ADD COLUMN contact_phone_mask VARCHAR(32) NULL AFTER contact_phone_hash,
  ADD COLUMN basic_account_cipher VARCHAR(512) NULL AFTER bank_branch,
  ADD COLUMN basic_account_mask VARCHAR(64) NULL AFTER basic_account_cipher;

ALTER TABLE material_submission
  ADD COLUMN template_item_id BIGINT UNSIGNED NULL AFTER case_id,
  ADD COLUMN input_type VARCHAR(20) NULL AFTER material_name,
  ADD COLUMN is_sensitive TINYINT(1) NOT NULL DEFAULT 0 AFTER required,
  ADD COLUMN text_value TEXT NULL AFTER is_sensitive,
  ADD COLUMN submit_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED' AFTER text_value,
  ADD COLUMN client_visible_note VARCHAR(500) NULL AFTER review_note,
  ADD COLUMN internal_note VARCHAR(500) NULL AFTER client_visible_note,
  ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 AFTER updated_at;

-- 5. 文件、审核、补件、状态和导出
ALTER TABLE file_object
  MODIFY COLUMN case_id BIGINT NULL,
  ADD COLUMN bucket_name VARCHAR(128) NULL AFTER case_id,
  ADD COLUMN stored_name VARCHAR(255) NULL AFTER original_name,
  ADD COLUMN extension VARCHAR(32) NULL AFTER content_type,
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'READY' AFTER sha256,
  ADD COLUMN uploaded_by BIGINT UNSIGNED NULL AFTER created_by;

CREATE TABLE IF NOT EXISTS material_review_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  material_id BIGINT UNSIGNED NOT NULL,
  case_id BIGINT UNSIGNED NOT NULL,
  before_status VARCHAR(32) NULL,
  after_status VARCHAR(32) NOT NULL,
  client_visible_note VARCHAR(500) NULL,
  internal_note VARCHAR(500) NULL,
  reviewed_by BIGINT UNSIGNED NOT NULL,
  reviewed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_review_material_time (material_id, reviewed_at),
  KEY idx_review_case_time (case_id, reviewed_at)
) ENGINE=InnoDB COMMENT='资料审核历史';

CREATE TABLE IF NOT EXISTS supplement_request (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  case_id BIGINT UNSIGNED NOT NULL,
  request_no VARCHAR(40) NOT NULL,
  client_message VARCHAR(1000) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/RESOLVED/CANCELED',
  requested_by BIGINT UNSIGNED NOT NULL,
  requested_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  resolved_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_supplement_request_no (request_no),
  KEY idx_supplement_case_status (case_id, status)
) ENGINE=InnoDB COMMENT='补件要求';

CREATE TABLE IF NOT EXISTS supplement_request_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  request_id BIGINT UNSIGNED NOT NULL,
  material_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_supplement_item (request_id, material_id),
  KEY idx_supplement_item_material (material_id)
) ENGINE=InnoDB COMMENT='补件资料项';

CREATE TABLE IF NOT EXISTS case_status_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  case_id BIGINT UNSIGNED NOT NULL,
  before_status VARCHAR(32) NULL,
  after_status VARCHAR(32) NOT NULL,
  note VARCHAR(1000) NULL,
  changed_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_case_status_log_case_time (case_id, created_at)
) ENGINE=InnoDB COMMENT='报单状态流转历史';

ALTER TABLE export_job
  MODIFY COLUMN export_type VARCHAR(32) NOT NULL COMMENT 'REVIEW_EXCEL/MATERIAL_ZIP',
  ADD COLUMN request_id VARCHAR(64) NULL AFTER case_id,
  ADD COLUMN request_payload JSON NULL AFTER request_id,
  ADD COLUMN error_code VARCHAR(64) NULL AFTER object_key,
  ADD COLUMN error_message VARCHAR(500) NULL AFTER error_code,
  ADD COLUMN started_at DATETIME(3) NULL AFTER expires_at,
  ADD COLUMN completed_at DATETIME(3) NULL AFTER started_at,
  ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 AFTER created_at,
  ADD UNIQUE KEY uk_export_request_id (request_id),
  ADD KEY idx_export_operator_status (operator_id, status, created_at);

CREATE TABLE IF NOT EXISTS file_access_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  file_id BIGINT UNSIGNED NOT NULL,
  case_id BIGINT UNSIGNED NULL,
  access_type VARCHAR(16) NOT NULL COMMENT 'PREVIEW/DOWNLOAD',
  operator_id BIGINT UNSIGNED NOT NULL,
  result VARCHAR(16) NOT NULL COMMENT 'SUCCESS/DENIED/EXPIRED',
  request_id VARCHAR(64) NULL,
  ip_address VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_file_access_file_time (file_id, created_at),
  KEY idx_file_access_operator_time (operator_id, created_at)
) ENGINE=InnoDB COMMENT='敏感文件访问日志';

CREATE TABLE IF NOT EXISTS download_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  export_job_id BIGINT UNSIGNED NOT NULL,
  operator_id BIGINT UNSIGNED NOT NULL,
  result VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_download_job_time (export_job_id, created_at)
) ENGINE=InnoDB COMMENT='导出文件下载日志';

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  operator_id BIGINT UNSIGNED NULL,
  operation_code VARCHAR(64) NOT NULL,
  business_type VARCHAR(32) NOT NULL,
  business_id BIGINT UNSIGNED NULL,
  summary VARCHAR(500) NULL,
  request_id VARCHAR(64) NULL,
  ip_address VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_operation_business_time (business_type, business_id, created_at),
  KEY idx_operation_operator_time (operator_id, created_at)
) ENGINE=InnoDB COMMENT='业务操作审计';

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  company_id BIGINT UNSIGNED NULL,
  case_id BIGINT UNSIGNED NULL,
  notification_type VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  read_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_notification_user_read (user_id, read_at, created_at)
) ENGINE=InnoDB COMMENT='站内通知';

CREATE TABLE IF NOT EXISTS import_job (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  job_no VARCHAR(40) NOT NULL,
  source_file_id BIGINT UNSIGNED NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCEEDED/FAILED/CANCELED',
  dry_run TINYINT(1) NOT NULL DEFAULT 0,
  total_rows INT UNSIGNED NOT NULL DEFAULT 0,
  success_rows INT UNSIGNED NOT NULL DEFAULT 0,
  skipped_rows INT UNSIGNED NOT NULL DEFAULT 0,
  failed_rows INT UNSIGNED NOT NULL DEFAULT 0,
  error_report_file_id BIGINT UNSIGNED NULL,
  operator_id BIGINT UNSIGNED NOT NULL,
  started_at DATETIME(3) NULL,
  completed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_import_job_no (job_no),
  KEY idx_import_operator_status (operator_id, status, created_at)
) ENGINE=InnoDB COMMENT='Excel 导入任务';

CREATE TABLE IF NOT EXISTS import_job_error (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  job_id BIGINT UNSIGNED NOT NULL,
  row_no INT UNSIGNED NOT NULL,
  error_code VARCHAR(64) NOT NULL,
  error_message VARCHAR(500) NOT NULL,
  row_snapshot JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_import_error_job_row (job_id, row_no)
) ENGINE=InnoDB COMMENT='Excel 导入错误行';

-- 6. 初始化服务模块、权限和模板
INSERT INTO service_module (module_code, module_name, description, sort_order, status)
VALUES
  ('TRADE_INCREMENT', '贸易增量', '月度企业业务材料提交', 1, 'ENABLED'),
  ('IP', '知识产权', '知识产权基础需求报单', 2, 'ENABLED'),
  ('QUALIFICATION', '资质申报', '企业资质申报需求报单', 3, 'ENABLED')
ON DUPLICATE KEY UPDATE module_name = VALUES(module_name), description = VALUES(description), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO role (role_code, role_name, status)
VALUES
  ('SUPER_ADMIN', '超级管理员', 'ENABLED'),
  ('OPERATIONS', '运营人员', 'ENABLED'),
  ('REVIEWER', '审核人员', 'ENABLED'),
  ('CLIENT', '客户用户', 'ENABLED')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = VALUES(status);

INSERT INTO permission (permission_code, permission_name, permission_type)
VALUES
  ('dashboard:view', '查看工作台', 'MENU'),
  ('case:view', '查看报单', 'API'),
  ('case:create', '创建报单', 'API'),
  ('case:assign', '分配负责人', 'BUTTON'),
  ('case:review', '审核资料', 'BUTTON'),
  ('product:view', '查看产品', 'MENU'),
  ('product:edit', '编辑产品', 'BUTTON'),
  ('company:view', '查看客户资料', 'MENU'),
  ('company:import', '导入客户 Excel', 'BUTTON'),
  ('file:sensitive:view', '受控查看敏感资料', 'BUTTON'),
  ('export:excel', '导出审核 Excel', 'BUTTON'),
  ('export:zip', '导出原始资料 ZIP', 'BUTTON'),
  ('audit:view', '查看操作日志', 'MENU')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p
  ON (r.role_code = 'OPERATIONS' AND p.permission_code IN ('dashboard:view','case:view','case:assign','product:view','company:view','company:import','export:excel','export:zip'))
  OR (r.role_code = 'REVIEWER' AND p.permission_code IN ('dashboard:view','case:view','case:review','product:view','company:view','file:sensitive:view','export:excel'))
  OR (r.role_code = 'CLIENT' AND p.permission_code IN ('case:view','case:create','product:view'));

INSERT INTO material_template (template_code, template_name, version_no, status)
VALUES
  ('TRADE_INCREMENT', '贸易增量资料模板', 1, 'PUBLISHED'),
  ('IP_SIMPLE', '知识产权简表单模板', 1, 'PUBLISHED'),
  ('QUALIFICATION_SIMPLE', '资质申报简表单模板', 1, 'PUBLISHED')
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name), status = VALUES(status);

INSERT INTO material_template_item
  (template_id, item_code, item_name, input_type, is_required, is_sensitive, allowed_extensions, max_file_size_mb, max_file_count, help_text, sort_order)
SELECT t.id, x.item_code, x.item_name, x.input_type, x.is_required, x.is_sensitive, x.allowed_extensions, x.max_file_size_mb, x.max_file_count, x.help_text, x.sort_order
FROM material_template t
JOIN (
  SELECT 'TRADE_INCREMENT' template_code, 'BUSINESS_LICENSE' item_code, '营业执照图片' item_name, 'FILE' input_type, 1 is_required, 0 is_sensitive, 'jpg,jpeg,png,pdf' allowed_extensions, 10 max_file_size_mb, 1 max_file_count, '请上传清晰完整的营业执照' help_text, 1 sort_order
  UNION ALL SELECT 'TRADE_INCREMENT', 'INVOICE_INFO', '开票信息（8 项文字）', 'FORM_GROUP', 1, 1, NULL, NULL, 1, '请完整填写 8 项开票信息', 2
  UNION ALL SELECT 'TRADE_INCREMENT', 'LEGAL_ID_CARD', '法人身份证扫描件（盖公章）', 'FILE', 1, 1, 'jpg,jpeg,png,pdf', 20, 2, '请上传扫描件并盖公章', 3
  UNION ALL SELECT 'TRADE_INCREMENT', 'NO_TAX_DEBT_CERT', '无欠税证明', 'FILE', 1, 1, 'jpg,jpeg,png,pdf', 20, 1, '请上传有效证明', 4
  UNION ALL SELECT 'TRADE_INCREMENT', 'TAX_QUOTA_SCREENSHOT', '税务局开票额度截图', 'FILE', 1, 1, 'jpg,jpeg,png', 10, 3, '需包含右上角公司名称及右下角实时日期', 5
  UNION ALL SELECT 'TRADE_INCREMENT', 'ONLINE_BANK_LIMIT', '网银限额截图', 'FILE', 1, 1, 'jpg,jpeg,png,pdf', 10, 3, '请上传最新限额截图', 6
  UNION ALL SELECT 'TRADE_INCREMENT', 'ONLINE_BANK_BALANCE', '网银余额大于 1000 元截图', 'FILE', 1, 1, 'jpg,jpeg,png,pdf', 10, 3, '余额需大于 1000 元', 7
  UNION ALL SELECT 'TRADE_INCREMENT', 'LEGAL_CREDIT_REPORT', '法人征信报告', 'FILE', 1, 1, 'pdf,jpg,jpeg,png', 30, 1, '敏感资料受控查看', 8
  UNION ALL SELECT 'TRADE_INCREMENT', 'COMPANY_CREDIT_REPORT', '企业征信报告', 'FILE', 1, 1, 'pdf,jpg,jpeg,png', 30, 1, '敏感资料受控查看', 9
  UNION ALL SELECT 'TRADE_INCREMENT', 'OFFICE_MEDIA', '企业办公室照片及视频', 'FILE', 1, 0, 'jpg,jpeg,png,mp4', 100, 20, '支持办公室照片和视频', 10
  UNION ALL SELECT 'TRADE_INCREMENT', 'JELLYFISH_REPORT_URL', '水母报告链接', 'URL', 1, 0, NULL, NULL, 1, '请输入水母报告链接', 11
  UNION ALL SELECT 'IP_SIMPLE', 'LEGAL_PERSON_NAME', '法人姓名', 'TEXT', 1, 0, NULL, NULL, 1, '请输入法人姓名', 1
  UNION ALL SELECT 'IP_SIMPLE', 'COMPANY_NAME', '公司名称', 'TEXT', 1, 0, NULL, NULL, 1, '请输入公司全称', 2
  UNION ALL SELECT 'IP_SIMPLE', 'CONTACT_PHONE', '联系电话', 'TEXT', 1, 1, NULL, NULL, 1, '请输入联系电话', 3
  UNION ALL SELECT 'QUALIFICATION_SIMPLE', 'LEGAL_PERSON_NAME', '法人姓名', 'TEXT', 1, 0, NULL, NULL, 1, '请输入法人姓名', 1
  UNION ALL SELECT 'QUALIFICATION_SIMPLE', 'COMPANY_NAME', '公司名称', 'TEXT', 1, 0, NULL, NULL, 1, '请输入公司全称', 2
  UNION ALL SELECT 'QUALIFICATION_SIMPLE', 'CONTACT_PHONE', '联系电话', 'TEXT', 1, 1, NULL, NULL, 1, '请输入联系电话', 3
) x ON x.template_code = t.template_code AND t.version_no = 1
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name), help_text = VALUES(help_text), sort_order = VALUES(sort_order);

-- 使用显式初始化语句，兼容已有骨架库并避免覆盖历史产品。
INSERT INTO service_product (module_id, product_code, product_name, description, template_id, sort_order, status)
SELECT m.id, 'TRADE_INCREMENT_SERVICE', '贸易增量服务', '月度企业材料提交', t.id, 1, 'PUBLISHED'
FROM service_module m JOIN material_template t ON t.template_code = 'TRADE_INCREMENT' AND t.version_no = 1
WHERE m.module_code = 'TRADE_INCREMENT'
  AND NOT EXISTS (SELECT 1 FROM service_product p WHERE p.product_code = 'TRADE_INCREMENT_SERVICE');

INSERT INTO service_product (module_id, product_code, product_name, description, template_id, sort_order, status)
SELECT m.id, 'IP_CONSULTING', '商标注册咨询', '知识产权基础报单', t.id, 2, 'PUBLISHED'
FROM service_module m JOIN material_template t ON t.template_code = 'IP_SIMPLE' AND t.version_no = 1
WHERE m.module_code = 'IP'
  AND NOT EXISTS (SELECT 1 FROM service_product p WHERE p.product_code = 'IP_CONSULTING');

INSERT INTO service_product (module_id, product_code, product_name, description, template_id, sort_order, status)
SELECT m.id, 'QUALIFICATION_APPLICATION', '资质申报服务', '科技资质申报需求', t.id, 3, 'PUBLISHED'
FROM service_module m JOIN material_template t ON t.template_code = 'QUALIFICATION_SIMPLE' AND t.version_no = 1
WHERE m.module_code = 'QUALIFICATION'
  AND NOT EXISTS (SELECT 1 FROM service_product p WHERE p.product_code = 'QUALIFICATION_APPLICATION');

-- 注意：本脚本不创建物理外键，关联完整性由后端事务和权限层维护。
-- 兼容说明：001_init.sql 中的 invoice_info.tax_no、invoice_info.basic_account 和
-- invoice_info.contact_phone 为早期明文字段。新代码必须只写入 *_cipher、*_hash、*_mask
-- 字段；已有真实数据迁移前应先完成加密回填，不得直接继续使用早期明文字段。
