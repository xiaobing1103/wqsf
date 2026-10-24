-- 功能可用性配置，不代表平台审核结论；禁止用于隐藏未审核业务。
CREATE TABLE IF NOT EXISTS client_capability (
  capability_key VARCHAR(64) NOT NULL PRIMARY KEY,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO client_capability (capability_key, enabled) VALUES
('tradeApplicationEnabled', 0), ('trademarkConsultationEnabled', 0),
('qualificationConsultationEnabled', 0), ('categoryMatchEnabled', 1),
('licenseOcrEnabled', 0), ('sensitiveMaterialUploadEnabled', 0),
('customerServiceEnabled', 0);
