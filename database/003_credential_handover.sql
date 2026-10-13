-- 管理后台线下凭证交接状态
-- 说明：仅记录交接状态和经办信息，严禁保存网银密码、U 盾密码、交易密码或其文件。

USE wqst;

CREATE TABLE IF NOT EXISTS credential_handover (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  case_id BIGINT UNSIGNED NOT NULL,
  online_banking_status VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/HANDED_OVER/VERIFIED/RETURNED/NOT_REQUIRED',
  u_shield_status VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/HANDED_OVER/VERIFIED/RETURNED/NOT_REQUIRED',
  transaction_authorization_status VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/HANDED_OVER/VERIFIED/RETURNED/NOT_REQUIRED',
  handled_by BIGINT UNSIGNED NOT NULL,
  handled_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_credential_handover_case (case_id),
  KEY idx_credential_handover_operator_time (handled_by, handled_at)
) ENGINE=InnoDB COMMENT='线下凭证交接状态，不保存任何认证秘密';

INSERT INTO permission (permission_code, permission_name, permission_type)
VALUES ('case:credential-handover', '登记线下凭证交接状态', 'BUTTON')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.permission_code = 'case:credential-handover'
WHERE r.role_code IN ('SUPER_ADMIN', 'OPERATIONS', 'REVIEWER');
