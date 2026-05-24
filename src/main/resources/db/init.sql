-- ============================================================
-- 智慧养殖系统 - 全量数据库建表脚本
-- 数据库名: smart_farm
-- 编码: utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_connection = utf8mb4;

CREATE DATABASE IF NOT EXISTS smart_farm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smart_farm;

-- ============================================================
-- 一、用户与权限模块（陈闯）
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(32) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(32) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(64) COMMENT '邮箱',
    avatar VARCHAR(256) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS t_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_code VARCHAR(32) NOT NULL UNIQUE COMMENT '角色编码(ADMIN/MANAGER/TECHNICIAN)',
    role_name VARCHAR(32) NOT NULL COMMENT '角色名称',
    description VARCHAR(128) COMMENT '角色描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS t_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT='用户角色关联表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS t_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(32) COMMENT '操作用户名',
    operation VARCHAR(64) NOT NULL COMMENT '操作类型',
    module VARCHAR(32) COMMENT '所属模块',
    description VARCHAR(256) COMMENT '操作描述',
    request_method VARCHAR(10) COMMENT '请求方式',
    request_url VARCHAR(256) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    ip VARCHAR(64) COMMENT '操作IP',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
) COMMENT='审计日志表';

-- ============================================================
-- 二、养殖基地与水池模块（黄舰）
-- ============================================================

-- 养殖基地/车间表
CREATE TABLE IF NOT EXISTS t_farm_facility (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_name VARCHAR(64) NOT NULL COMMENT '基地/车间名称',
    location VARCHAR(128) COMMENT '地理位置',
    description VARCHAR(256) COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='养殖基地/车间表';

-- 水池表
CREATE TABLE IF NOT EXISTS t_tank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tank_code VARCHAR(32) NOT NULL UNIQUE COMMENT '水池编号(如: T-001)',
    tank_name VARCHAR(64) NOT NULL COMMENT '水池名称(如: 1号养成池)',
    tank_type ENUM('BREEDING','FILTER','NURSERY') DEFAULT 'BREEDING' COMMENT '水池类型: 养成池/过滤池/育苗池',
    facility_id BIGINT NOT NULL COMMENT '所属基地ID',
    volume_m3 DECIMAL(8,2) COMMENT '水体体积(立方米)',
    status ENUM('IDLE','OCCUPIED','MAINTENANCE') DEFAULT 'IDLE' COMMENT '状态: 空闲/占用/维护',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_facility (facility_id)
) COMMENT='水池表';

-- ============================================================
-- 三、IoT 设备模块（黄舰）
-- ============================================================

-- IoT 设备表
CREATE TABLE IF NOT EXISTS t_iot_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL UNIQUE COMMENT '设备唯一标识(UUID)',
    device_name VARCHAR(64) COMMENT '设备名称',
    mac_address VARCHAR(17) NOT NULL UNIQUE COMMENT 'MAC地址',
    device_type ENUM('SENSOR','ACTUATOR') NOT NULL COMMENT '设备类型: 传感器/执行器',
    parameter_type VARCHAR(16) COMMENT '监测参数类型(DO/PH/TEMP/NH4/WATER_LEVEL)',
    tank_id BIGINT COMMENT '绑定的水池ID',
    mqtt_topic VARCHAR(128) COMMENT 'MQTT主题',
    online_status TINYINT DEFAULT 0 COMMENT '在线状态: 0-离线 1-在线',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    status TINYINT DEFAULT 1 COMMENT '启用状态: 1-启用 0-停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tank (tank_id),
    INDEX idx_type (device_type)
) COMMENT='IoT设备表';

-- 报警规则表
CREATE TABLE IF NOT EXISTS t_alert_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(64) NOT NULL COMMENT '规则名称',
    sensor_device_id BIGINT NOT NULL COMMENT '触发传感器设备ID',
    operator VARCHAR(4) NOT NULL COMMENT '比较运算符(<, >, <=, >=, ==)',
    threshold_value DECIMAL(10,2) NOT NULL COMMENT '阈值',
    duration_seconds INT DEFAULT 0 COMMENT '持续时间(秒)，防抖',
    alert_level TINYINT NOT NULL COMMENT '报警级别: 1-轻微 2-严重 3-致命',
    actuator_device_id BIGINT COMMENT '联动执行器设备ID',
    actuator_action VARCHAR(8) COMMENT '执行器动作(ON/OFF)',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用: 1-启用 0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sensor (sensor_device_id)
) COMMENT='报警/联动规则表';

-- 设备指令日志表
CREATE TABLE IF NOT EXISTS t_device_command_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL COMMENT '目标设备ID',
    command_type VARCHAR(16) NOT NULL COMMENT '指令类型(START/STOP/SET)',
    command_payload TEXT COMMENT '指令内容JSON',
    triggered_by VARCHAR(32) COMMENT '触发来源(RULE/MANUAL)',
    rule_id BIGINT COMMENT '关联规则ID',
    ack_status ENUM('PENDING','SUCCESS','TIMEOUT','FAILED') DEFAULT 'PENDING' COMMENT 'ACK状态',
    ack_time DATETIME COMMENT '收到ACK时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device (device_id),
    INDEX idx_time (create_time)
) COMMENT='设备指令下发日志表';

-- ============================================================
-- 四、养殖批次台账模块（陈闯）
-- ============================================================

-- 养殖批次主表
CREATE TABLE IF NOT EXISTS t_breeding_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(32) NOT NULL UNIQUE COMMENT '批次号(如: B-2026-04-01)',
    tank_id BIGINT NOT NULL COMMENT '绑定水池ID',
    species_name VARCHAR(64) NOT NULL COMMENT '养殖品种',
    initial_count INT NOT NULL COMMENT '初始入池尾数',
    current_count INT NOT NULL COMMENT '当前存活尾数',
    initial_avg_weight DECIMAL(8,2) COMMENT '入池均重(g)',
    total_feed_kg DECIMAL(10,2) DEFAULT 0.00 COMMENT '累计饲料总重(kg)',
    harvest_weight_kg DECIMAL(10,2) COMMENT '出栏总重(kg)',
    fcr DECIMAL(6,3) COMMENT '饲料转化率',
    supplier VARCHAR(64) COMMENT '供应商',
    quarantine_cert VARCHAR(64) COMMENT '检疫证号',
    status ENUM('ACTIVE','HARVESTED','CLOSED') DEFAULT 'ACTIVE' COMMENT '状态',
    start_date DATE NOT NULL COMMENT '入池日期',
    end_date DATE COMMENT '出栏日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tank (tank_id),
    INDEX idx_status (status)
) COMMENT='养殖批次主表';

-- 投喂记录表
CREATE TABLE IF NOT EXISTS t_feed_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    feed_weight_kg DECIMAL(8,2) NOT NULL COMMENT '本次投喂重量(kg)',
    feed_type VARCHAR(32) COMMENT '饲料类型',
    operator_id BIGINT COMMENT '操作人ID',
    feed_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投喂时间',
    remark VARCHAR(256) COMMENT '备注',
    INDEX idx_batch (batch_id),
    INDEX idx_time (feed_time)
) COMMENT='投喂记录表';

-- 死亡记录表
CREATE TABLE IF NOT EXISTS t_mortality_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    death_count INT NOT NULL COMMENT '死亡数量',
    death_cause ENUM('HYPOXIA','MECHANICAL','DISEASE','OTHER') COMMENT '死因分类: 缺氧/机械损伤/病害/其他',
    operator_id BIGINT COMMENT '操作人ID',
    record_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    remark VARCHAR(256) COMMENT '备注',
    INDEX idx_batch (batch_id)
) COMMENT='死亡记录表';

-- 用药记录表
CREATE TABLE IF NOT EXISTS t_medication_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    drug_name VARCHAR(64) NOT NULL COMMENT '药物名称',
    dosage VARCHAR(32) COMMENT '用量',
    withdrawal_days INT NOT NULL COMMENT '休药期(天)',
    withdrawal_end_date DATE NOT NULL COMMENT '休药期结束日期',
    operator_id BIGINT COMMENT '操作人ID',
    medication_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '用药时间',
    remark VARCHAR(256) COMMENT '备注',
    INDEX idx_batch (batch_id),
    INDEX idx_withdrawal (withdrawal_end_date)
) COMMENT='用药记录表';

-- ============================================================
-- 五、监控与报警模块（贾恩奇）
-- ============================================================

-- 水质阈值配置表
CREATE TABLE IF NOT EXISTS t_threshold_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parameter_type VARCHAR(16) NOT NULL COMMENT '参数类型(DO/PH/TEMP/NH4)',
    min_value DECIMAL(10,2) COMMENT '最小安全值',
    max_value DECIMAL(10,2) COMMENT '最大安全值',
    critical_min DECIMAL(10,2) COMMENT '致命最小值',
    critical_max DECIMAL(10,2) COMMENT '致命最大值',
    unit VARCHAR(16) COMMENT '单位(mg/L, ℃等)',
    description VARCHAR(128) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='水质阈值配置表';

-- 报警历史表
CREATE TABLE IF NOT EXISTS t_alert_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT COMMENT '触发的规则ID',
    tank_id BIGINT COMMENT '相关水池ID',
    device_id BIGINT COMMENT '触发传感器ID',
    alert_level TINYINT NOT NULL COMMENT '报警级别: 1/2/3',
    parameter_type VARCHAR(16) COMMENT '参数类型',
    current_value DECIMAL(10,2) COMMENT '触发时的当前值',
    threshold_value DECIMAL(10,2) COMMENT '阈值',
    message VARCHAR(256) COMMENT '报警消息',
    status ENUM('ACTIVE','ACKNOWLEDGED','ESCALATED','RESOLVED') DEFAULT 'ACTIVE' COMMENT '报警状态',
    acknowledged_by BIGINT COMMENT '确认人ID',
    acknowledged_time DATETIME COMMENT '确认时间',
    resolved_time DATETIME COMMENT '解除时间',
    escalated_to BIGINT COMMENT '升级推送给谁',
    escalated_time DATETIME COMMENT '升级时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_tank (tank_id),
    INDEX idx_time (create_time)
) COMMENT='报警历史表';

-- 水质日志表（MySQL备份，主要数据存ES）
CREATE TABLE IF NOT EXISTS t_water_quality_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tank_id BIGINT NOT NULL COMMENT '水池ID',
    device_id BIGINT NOT NULL COMMENT '传感器设备ID',
    parameter_type VARCHAR(16) NOT NULL COMMENT '参数类型',
    value DECIMAL(10,3) NOT NULL COMMENT '数值',
    recorded_at DATETIME NOT NULL COMMENT '采集时间(传感器原始时间戳)',
    received_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '服务器接收时间',
    INDEX idx_tank_param (tank_id, parameter_type),
    INDEX idx_time (recorded_at)
) COMMENT='水质数据日志表(MySQL备份)';

-- ============================================================
-- 六、初始化数据
-- ============================================================

-- 初始化角色
INSERT INTO t_role (role_code, role_name, description) VALUES
('ADMIN', '系统超级管理员', '拥有全部权限，负责系统配置与设备管理'),
('MANAGER', '农场运营经理', '拥有监控、报表、台账只读等权限'),
('TECHNICIAN', '现场作业技术员', '负责台账录入、初级预警处理');

-- 初始化管理员账号 (密码: admin123，BCrypt加密)
INSERT INTO t_user (username, password, real_name, phone, status) VALUES
('admin', 'admin123', '系统管理员', '13800000000', 1);

INSERT INTO t_user_role (user_id, role_id) VALUES (1, 1);

-- 初始化水质阈值配置
INSERT INTO t_threshold_config (parameter_type, min_value, max_value, critical_min, critical_max, unit, description) VALUES
('DO', 6.0, 12.0, 5.0, 15.0, 'mg/L', '溶解氧: 正常≥6.0, 致命<5.0'),
('PH', 7.0, 8.5, 6.5, 9.0, '', 'pH值: 最佳7.0-8.5'),
('TEMP', 18.0, 22.0, 15.0, 28.0, '℃', '水温: 黄条鰤最佳18-22℃'),
('NH4', 0.0, 0.5, 0.0, 0.8, 'mg/L', '氨氮: 必须<0.5, 致命>0.8');

-- 初始化演示基地和水池
INSERT INTO t_farm_facility (facility_name, location, description) VALUES
('1号示范养殖车间', '辽宁省大连市', '黄条鰤工厂化养殖示范基地');

INSERT INTO t_tank (tank_code, tank_name, tank_type, facility_id, volume_m3) VALUES
('T-001', '1号养成池', 'BREEDING', 1, 50.00),
('T-002', '2号养成池', 'BREEDING', 1, 50.00),
('T-003', '3号养成池', 'BREEDING', 1, 50.00),
('T-004', '4号养成池', 'BREEDING', 1, 45.00),
('T-005', '5号养成池', 'BREEDING', 1, 45.00),
('T-006', '6号养成池', 'BREEDING', 1, 45.00),
('T-007', '1号育苗池', 'NURSERY', 1, 20.00),
('T-008', '1号过滤池', 'FILTER', 1, 30.00),
('T-009', '2号过滤池', 'FILTER', 1, 30.00);
