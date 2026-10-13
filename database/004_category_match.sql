-- 营业执照经营范围与可做品类匹配规则
-- 规则只保存公开的行业关键词，不保存营业执照图片或识别结果。

USE wqst;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS business_category_rule (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  industry_code VARCHAR(64) NOT NULL,
  industry_name VARCHAR(64) NOT NULL,
  category_code VARCHAR(64) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  keywords VARCHAR(1000) NOT NULL COMMENT '英文逗号分隔的经营范围关键词',
  service_product_code VARCHAR(64) NULL COMMENT '可选关联服务产品编码',
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_business_category_code (category_code),
  KEY idx_business_category_industry (industry_code, status, sort_order)
) ENGINE=InnoDB COMMENT='经营范围品类匹配规则';

INSERT INTO business_category_rule
  (industry_code, industry_name, category_code, category_name, keywords, sort_order, status)
VALUES
  ('DIGITAL', '数字与技术服务', 'SOFTWARE_SERVICE', '软件与信息技术服务', '软件开发,软件服务,信息技术,技术开发,技术服务,技术咨询,计算机系统,网络技术', 10, 'ENABLED'),
  ('DIGITAL', '数字与技术服务', 'DATA_SERVICE', '数据与云服务', '数据处理,大数据,云计算,人工智能,数据服务,信息系统集成', 20, 'ENABLED'),
  ('DIGITAL', '数字与技术服务', 'NETWORK_SECURITY', '网络与信息安全', '网络安全,信息安全,安全技术,网络工程,安防系统', 30, 'ENABLED'),
  ('ELECTRONICS', '电子与办公设备', 'COMMUNICATION_EQUIPMENT', '通信与电子设备', '通信设备,通讯设备,电子产品,电子元器件,通信器材', 40, 'ENABLED'),
  ('ELECTRONICS', '电子与办公设备', 'OFFICE_EQUIPMENT', '计算机与办公设备', '办公设备,计算机,打印机,复印机,办公耗材,计算机软硬件', 50, 'ENABLED'),
  ('ELECTRONICS', '电子与办公设备', 'SECURITY_EQUIPMENT', '安防监控设备', '安防设备,监控设备,安全技术防范,门禁,视频监控', 60, 'ENABLED'),
  ('ENERGY', '能源与工业设备', 'NEW_ENERGY_EQUIPMENT', '新能源与储能设备', '新能源,光伏,风能,储能,充电桩,太阳能', 70, 'ENABLED'),
  ('ENERGY', '能源与工业设备', 'ELECTRICAL_EQUIPMENT', '电气与电力设备', '电气设备,电力设备,机电设备,配电设备,电线电缆', 80, 'ENABLED'),
  ('ENERGY', '能源与工业设备', 'MACHINERY_EQUIPMENT', '机械与自动化设备', '机械设备,自动化设备,机器人,工业设备,仪器仪表', 90, 'ENABLED'),
  ('CONSTRUCTION', '建筑与工程物资', 'CONSTRUCTION_ENGINEERING', '建筑与施工服务', '建筑工程,工程施工,装饰装修,市政工程,园林工程', 100, 'ENABLED'),
  ('CONSTRUCTION', '建筑与工程物资', 'BUILDING_MATERIALS', '建筑装饰材料', '建筑材料,装饰材料,五金产品,金属材料,防水材料', 110, 'ENABLED'),
  ('HOME_OFFICE', '家居与办公用品', 'FURNITURE', '家具与木制品', '家具,木制品,家居用品,办公家具,家具制造', 120, 'ENABLED'),
  ('HOME_OFFICE', '家居与办公用品', 'OFFICE_SUPPLIES', '办公文教用品', '办公用品,文具用品,教学设备,纸制品,文化用品', 130, 'ENABLED'),
  ('CONSUMER', '消费与生活物资', 'FOOD_BEVERAGE', '食品与农副产品', '食品销售,预包装食品,饮料,农产品,农副产品,粮油', 140, 'ENABLED'),
  ('CONSUMER', '消费与生活物资', 'TEXTILE_APPAREL', '纺织服装用品', '服装,服饰,纺织品,针织品,鞋帽,劳动保护用品', 150, 'ENABLED'),
  ('CONSUMER', '消费与生活物资', 'DAILY_NECESSITIES', '日用百货与家电', '日用百货,日用品,家用电器,化妆品,清洁用品', 160, 'ENABLED'),
  ('HEALTH_SPORT', '健康与文体用品', 'MEDICAL_DEVICE', '医疗器械与设备', '医疗器械,医疗设备,卫生用品,康复设备,消毒用品', 170, 'ENABLED'),
  ('HEALTH_SPORT', '健康与文体用品', 'SPORTS_EQUIPMENT', '体育与健身用品', '体育用品,健身器材,运动器材,户外用品,文体用品', 180, 'ENABLED'),
  ('AUTOMOTIVE', '汽车与交通用品', 'AUTOMOTIVE_SUPPLIES', '汽车配件与用品', '汽车零配件,汽车用品,机动车配件,轮胎,汽车装饰', 190, 'ENABLED')
ON DUPLICATE KEY UPDATE
  industry_code = VALUES(industry_code),
  industry_name = VALUES(industry_name),
  category_name = VALUES(category_name),
  keywords = VALUES(keywords),
  sort_order = VALUES(sort_order),
  status = VALUES(status);
