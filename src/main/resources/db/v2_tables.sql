-- ============================================================
-- V2 新增表：经营管理模块
-- ============================================================

SET NAMES utf8mb4;
USE smart_farm;

-- 饲料库存表
CREATE TABLE IF NOT EXISTS t_feed_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feed_name VARCHAR(64) NOT NULL COMMENT '饲料名称',
    feed_type VARCHAR(32) COMMENT '饲料类型(膨化颗粒/沉性颗粒/粉状)',
    specification VARCHAR(32) COMMENT '规格(如25kg/袋)',
    stock_kg DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当前库存(kg)',
    unit_price DECIMAL(8,2) COMMENT '单价(元/kg)',
    supplier VARCHAR(64) COMMENT '供应商',
    warning_threshold DECIMAL(10,2) DEFAULT 100 COMMENT '库存预警阈值(kg)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='饲料库存表';

-- 饲料出入库记录
CREATE TABLE IF NOT EXISTS t_feed_inventory_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id BIGINT NOT NULL COMMENT '饲料ID',
    type ENUM('IN','OUT') NOT NULL COMMENT '出入库类型',
    quantity_kg DECIMAL(10,2) NOT NULL COMMENT '数量(kg)',
    batch_id BIGINT COMMENT '关联批次(出库时)',
    operator_id BIGINT COMMENT '操作人',
    remark VARCHAR(128) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='饲料出入库记录';

-- 成本记录表
CREATE TABLE IF NOT EXISTS t_cost_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT COMMENT '关联批次',
    cost_type ENUM('FEED','ELECTRICITY','MEDICINE','LABOR','OTHER') NOT NULL COMMENT '成本类型',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额(元)',
    description VARCHAR(128) COMMENT '描述',
    record_date DATE NOT NULL COMMENT '记录日期',
    operator_id BIGINT COMMENT '操作人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='成本记录表';

-- 检疫证管理表
CREATE TABLE IF NOT EXISTS t_quarantine_cert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT '关联批次',
    cert_no VARCHAR(64) NOT NULL COMMENT '检疫证号',
    issuer VARCHAR(64) COMMENT '签发机构',
    issue_date DATE COMMENT '签发日期',
    valid_until DATE COMMENT '有效期至',
    cert_type ENUM('IMPORT','EXPORT') DEFAULT 'IMPORT' COMMENT '类型:入场/出场',
    file_url VARCHAR(256) COMMENT '证书附件URL',
    status ENUM('VALID','EXPIRED','REVOKED') DEFAULT 'VALID' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='检疫证管理表';

-- 市场行情表（缓存）
CREATE TABLE IF NOT EXISTS t_market_price (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    species_name VARCHAR(64) NOT NULL COMMENT '品种',
    price_per_kg DECIMAL(8,2) NOT NULL COMMENT '市场价(元/kg)',
    market_name VARCHAR(64) COMMENT '市场名称',
    price_date DATE NOT NULL COMMENT '行情日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='市场行情价格表';

-- 初始化饲料库存数据
INSERT INTO t_feed_inventory (feed_name, feed_type, specification, stock_kg, unit_price, supplier, warning_threshold) VALUES
('黄条鰤专用膨化料', '膨化颗粒', '25kg/袋', 2500.00, 12.50, '大连海大饲料', 500),
('幼鱼开口料', '粉状', '10kg/袋', 800.00, 18.00, '通威股份', 200),
('育成期沉性料', '沉性颗粒', '25kg/袋', 1800.00, 10.80, '大连海大饲料', 400);

-- 初始化成本数据
INSERT INTO t_cost_record (batch_id, cost_type, amount, description, record_date) VALUES
(1, 'FEED', 15631.25, '1号池3月饲料支出', '2026-03-31'),
(1, 'ELECTRICITY', 3200.00, '1号池3月电费', '2026-03-31'),
(1, 'MEDICINE', 450.00, '消毒剂采购', '2026-03-15'),
(2, 'FEED', 10584.00, '2号池3月饲料支出', '2026-03-31'),
(2, 'ELECTRICITY', 2800.00, '2号池3月电费', '2026-03-31'),
(1, 'LABOR', 8000.00, '4月工人工资分摊', '2026-04-01');

-- 初始化市场行情
INSERT INTO t_market_price (species_name, price_per_kg, market_name, price_date) VALUES
('黄条鰤', 68.50, '大连水产品批发市场', '2026-04-12'),
('黄条鰤', 67.80, '大连水产品批发市场', '2026-04-11'),
('黄条鰤', 69.20, '大连水产品批发市场', '2026-04-10'),
('黄条鰤', 66.50, '大连水产品批发市场', '2026-04-09'),
('黄条鰤', 68.00, '大连水产品批发市场', '2026-04-08'),
('大黄鱼', 45.00, '福州海峡水产品市场', '2026-04-12'),
('大黄鱼', 44.50, '福州海峡水产品市场', '2026-04-11'),
('大黄鱼', 46.00, '福州海峡水产品市场', '2026-04-10');

-- 初始化检疫证
INSERT INTO t_quarantine_cert (batch_id, cert_no, issuer, issue_date, valid_until, cert_type, status) VALUES
(1, 'QC-2026-001', '辽宁省海洋渔业厅', '2026-03-01', '2026-09-01', 'IMPORT', 'VALID'),
(2, 'QC-2026-002', '辽宁省海洋渔业厅', '2026-03-05', '2026-09-05', 'IMPORT', 'VALID'),
(5, 'QC-EXP-001', '大连市动物卫生监督所', '2026-04-01', '2026-04-15', 'EXPORT', 'VALID');
