-- ============================================================
-- 智慧养殖系统 - 测试数据
-- 在执行 init.sql 之后运行此脚本
-- ============================================================

SET NAMES utf8mb4;
USE smart_farm;

-- ============================================================
-- 1. 用户数据（3个角色各一个）
-- 密码统一为: 123456 (BCrypt加密)
-- ============================================================
INSERT INTO t_user (username, password, real_name, phone, status) VALUES
('manager', 'admin123', '王运营', '13800000001', 1),
('tech01', 'admin123', '李技术', '13800000002', 1),
('tech02', 'admin123', '张技术', '13800000003', 1);

INSERT INTO t_user_role (user_id, role_id) VALUES
(2, 2),  -- manager -> MANAGER
(3, 3),  -- tech01 -> TECHNICIAN
(4, 3);  -- tech02 -> TECHNICIAN

-- ============================================================
-- 2. IoT设备数据（每个养成池3个传感器+1个执行器）
-- ============================================================
INSERT INTO t_iot_device (device_id, device_name, mac_address, device_type, parameter_type, tank_id, mqtt_topic, online_status, last_heartbeat, status) VALUES
-- 1号养成池
('d001-do-sensor-tank1', '1号池DO传感器', 'AA:BB:CC:01:01:01', 'SENSOR', 'DO', 1, '/ras/farm_1/tank_1/sensor_do', 1, NOW(), 1),
('d002-ph-sensor-tank1', '1号池pH传感器', 'AA:BB:CC:01:01:02', 'SENSOR', 'PH', 1, '/ras/farm_1/tank_1/sensor_ph', 1, NOW(), 1),
('d003-temp-sensor-tank1', '1号池温度传感器', 'AA:BB:CC:01:01:03', 'SENSOR', 'TEMP', 1, '/ras/farm_1/tank_1/sensor_temp', 1, NOW(), 1),
('d004-aerator-tank1', '1号池曝气机', 'AA:BB:CC:01:02:01', 'ACTUATOR', NULL, 1, '/ras/farm_1/device_d004-aerator-tank1/cmd', 1, NOW(), 1),

-- 2号养成池
('d005-do-sensor-tank2', '2号池DO传感器', 'AA:BB:CC:02:01:01', 'SENSOR', 'DO', 2, '/ras/farm_1/tank_2/sensor_do', 1, NOW(), 1),
('d006-ph-sensor-tank2', '2号池pH传感器', 'AA:BB:CC:02:01:02', 'SENSOR', 'PH', 2, '/ras/farm_1/tank_2/sensor_ph', 1, NOW(), 1),
('d007-temp-sensor-tank2', '2号池温度传感器', 'AA:BB:CC:02:01:03', 'SENSOR', 'TEMP', 2, '/ras/farm_1/tank_2/sensor_temp', 1, NOW(), 1),
('d008-aerator-tank2', '2号池曝气机', 'AA:BB:CC:02:02:01', 'ACTUATOR', NULL, 2, '/ras/farm_1/device_d008-aerator-tank2/cmd', 1, NOW(), 1),

-- 3号养成池
('d009-do-sensor-tank3', '3号池DO传感器', 'AA:BB:CC:03:01:01', 'SENSOR', 'DO', 3, '/ras/farm_1/tank_3/sensor_do', 1, NOW(), 1),
('d010-ph-sensor-tank3', '3号池pH传感器', 'AA:BB:CC:03:01:02', 'SENSOR', 'PH', 3, '/ras/farm_1/tank_3/sensor_ph', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR), 1),
('d011-temp-sensor-tank3', '3号池温度传感器', 'AA:BB:CC:03:01:03', 'SENSOR', 'TEMP', 3, '/ras/farm_1/tank_3/sensor_temp', 1, NOW(), 1),
('d012-aerator-tank3', '3号池曝气机', 'AA:BB:CC:03:02:01', 'ACTUATOR', NULL, 3, '/ras/farm_1/device_d012-aerator-tank3/cmd', 1, NOW(), 1),

-- 5号养成池（模拟异常池）
('d013-do-sensor-tank5', '5号池DO传感器', 'AA:BB:CC:05:01:01', 'SENSOR', 'DO', 5, '/ras/farm_1/tank_5/sensor_do', 1, NOW(), 1),
('d014-nh4-sensor-tank5', '5号池氨氮传感器', 'AA:BB:CC:05:01:04', 'SENSOR', 'NH4', 5, '/ras/farm_1/tank_5/sensor_nh4', 1, NOW(), 1),
('d015-aerator-tank5', '5号池曝气机', 'AA:BB:CC:05:02:01', 'ACTUATOR', NULL, 5, '/ras/farm_1/device_d015-aerator-tank5/cmd', 0, DATE_SUB(NOW(), INTERVAL 5 MINUTE), 1),

-- 过滤池
('d016-waterlevel-tank8', '1号过滤池水位传感器', 'AA:BB:CC:08:01:01', 'SENSOR', 'WATER_LEVEL', 8, '/ras/farm_1/tank_8/sensor_water_level', 1, NOW(), 1);

-- ============================================================
-- 3. 养殖批次数据
-- ============================================================
INSERT INTO t_breeding_batch (batch_id, tank_id, species_name, initial_count, current_count, initial_avg_weight, total_feed_kg, harvest_weight_kg, fcr, supplier, quarantine_cert, status, start_date, end_date) VALUES
('B-2026-03-01', 1, '黄条鰤', 5000, 4832, 50.00, 1250.50, NULL, NULL, '大连海洋种苗有限公司', 'QC-2026-001', 'ACTIVE', '2026-03-01', NULL),
('B-2026-03-05', 2, '黄条鰤', 4800, 4650, 48.00, 980.00, NULL, NULL, '大连海洋种苗有限公司', 'QC-2026-002', 'ACTIVE', '2026-03-05', NULL),
('B-2026-03-10', 3, '黄条鰤', 4500, 4380, 52.00, 850.00, NULL, NULL, '青岛水产育苗中心', 'QC-2026-003', 'ACTIVE', '2026-03-10', NULL),
('B-2026-02-01', 5, '大黄鱼', 6000, 5200, 30.00, 2100.00, NULL, NULL, '福建闽东水产', 'QC-2026-004', 'ACTIVE', '2026-02-01', NULL),
('B-2026-01-15', 4, '黄条鰤', 3000, 0, 45.00, 1800.00, 1520.00, 1.28, '大连海洋种苗有限公司', 'QC-2025-088', 'HARVESTED', '2026-01-15', '2026-04-01'),
('B-2025-12-01', 6, '大黄鱼', 5500, 0, 25.00, 2350.00, 1980.00, 1.32, '福建闽东水产', 'QC-2025-076', 'HARVESTED', '2025-12-01', '2026-03-20');

-- 更新水池状态
UPDATE t_tank SET status = 'OCCUPIED' WHERE id IN (1, 2, 3, 5);

-- ============================================================
-- 4. 投喂记录（最近7天）
-- ============================================================
INSERT INTO t_feed_record (batch_id, feed_weight_kg, feed_type, operator_id, feed_time, remark) VALUES
(1, 25.50, '膨化颗粒料', 3, DATE_SUB(NOW(), INTERVAL 7 DAY), '早间投喂'),
(1, 28.00, '膨化颗粒料', 3, DATE_SUB(NOW(), INTERVAL 7 DAY), '晚间投喂'),
(1, 26.00, '膨化颗粒料', 3, DATE_SUB(NOW(), INTERVAL 6 DAY), '早间投喂'),
(1, 27.50, '膨化颗粒料', 3, DATE_SUB(NOW(), INTERVAL 6 DAY), '晚间投喂'),
(1, 25.00, '膨化颗粒料', 3, DATE_SUB(NOW(), INTERVAL 5 DAY), '早间投喂'),
(1, 26.50, '膨化颗粒料', 4, DATE_SUB(NOW(), INTERVAL 5 DAY), '晚间投喂'),
(1, 24.00, '膨化颗粒料', 4, DATE_SUB(NOW(), INTERVAL 4 DAY), '早间投喂'),
(1, 27.00, '膨化颗粒料', 4, DATE_SUB(NOW(), INTERVAL 4 DAY), '晚间投喂'),
(1, 26.00, '膨化颗粒料', 3, DATE_SUB(NOW(), INTERVAL 3 DAY), '早间投喂'),
(1, 25.50, '膨化颗粒料', 3, DATE_SUB(NOW(), INTERVAL 3 DAY), '晚间投喂'),
(1, 28.00, '膨化颗粒料', 3, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
(1, 26.00, '膨化颗粒料', 3, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(1, 27.00, '膨化颗粒料', 3, NOW(), '今日早间'),
(2, 22.00, '膨化颗粒料', 4, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(2, 23.50, '膨化颗粒料', 4, NOW(), NULL),
(3, 20.00, '沉性颗粒料', 3, NOW(), NULL),
(4, 18.00, '粉状饲料', 4, NOW(), NULL);

-- ============================================================
-- 5. 死亡记录
-- ============================================================
INSERT INTO t_mortality_record (batch_id, death_count, death_cause, operator_id, record_time, remark) VALUES
(1, 12, 'HYPOXIA', 3, DATE_SUB(NOW(), INTERVAL 10 DAY), '凌晨DO骤降导致'),
(1, 5, 'MECHANICAL', 3, DATE_SUB(NOW(), INTERVAL 7 DAY), '换水时网兜刮伤'),
(1, 3, 'DISEASE', 4, DATE_SUB(NOW(), INTERVAL 3 DAY), '疑似白点病'),
(2, 8, 'HYPOXIA', 4, DATE_SUB(NOW(), INTERVAL 5 DAY), '曝气机故障2小时'),
(2, 4, 'OTHER', 3, DATE_SUB(NOW(), INTERVAL 2 DAY), '原因不明'),
(3, 6, 'DISEASE', 3, DATE_SUB(NOW(), INTERVAL 4 DAY), '肠炎'),
(4, 35, 'HYPOXIA', 4, DATE_SUB(NOW(), INTERVAL 8 DAY), 'DO传感器离线未及时发现');

-- ============================================================
-- 6. 用药记录（2号池在休药期内）
-- ============================================================
INSERT INTO t_medication_record (batch_id, drug_name, dosage, withdrawal_days, withdrawal_end_date, operator_id, medication_time, remark) VALUES
(2, '恩诺沙星', '5g/吨水', 28, DATE_ADD(CURDATE(), INTERVAL 14 DAY), 3, DATE_SUB(NOW(), INTERVAL 14 DAY), '治疗肠炎，休药期28天'),
(3, '聚维酮碘', '0.3mg/L', 7, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 4, DATE_SUB(NOW(), INTERVAL 10 DAY), '日常消毒，已过休药期'),
(4, '氟苯尼考', '10mg/kg鱼体重', 21, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 3, DATE_SUB(NOW(), INTERVAL 16 DAY), '治疗细菌性败血症');

-- ============================================================
-- 7. 报警规则
-- ============================================================
INSERT INTO t_alert_rule (rule_name, sensor_device_id, operator, threshold_value, duration_seconds, alert_level, actuator_device_id, actuator_action, enabled) VALUES
('1号池DO低氧联动增氧', 1, '<', 6.00, 180, 3, 4, 'ON', 1),
('2号池DO低氧联动增氧', 5, '<', 6.00, 180, 3, 8, 'ON', 1),
('3号池DO低氧联动增氧', 9, '<', 6.00, 180, 3, 12, 'ON', 1),
('5号池DO低氧联动增氧', 13, '<', 6.00, 180, 3, 15, 'ON', 1),
('5号池氨氮超标预警', 14, '>', 0.50, 120, 2, NULL, NULL, 1),
('1号池高温报警', 3, '>', 22.00, 300, 2, NULL, NULL, 1),
('1号池pH偏低提示', 2, '<', 7.00, 600, 1, NULL, NULL, 0);

-- ============================================================
-- 8. 报警历史记录
-- ============================================================
INSERT INTO t_alert_history (rule_id, tank_id, device_id, alert_level, parameter_type, current_value, threshold_value, message, status, acknowledged_by, acknowledged_time, resolved_time, escalated_to, escalated_time, create_time) VALUES
-- 活跃报警
(4, 5, 13, 3, 'DO', 4.80, 6.00, '5号养成池溶解氧严重不足(4.8mg/L)，已低于致命阈值6.0mg/L', 'ACTIVE', NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(5, 5, 14, 2, 'NH4', 0.62, 0.50, '5号养成池氨氮超标(0.62mg/L)，超过安全上限0.5mg/L', 'ACTIVE', NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),

-- 已确认报警
(6, 1, 3, 2, 'TEMP', 22.80, 22.00, '1号养成池水温偏高(22.8℃)，超过22℃上限', 'ACKNOWLEDGED', 3, DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1, 1, 1, 3, 'DO', 5.50, 6.00, '1号养成池DO降至5.5mg/L，曝气机已自动启动', 'ACKNOWLEDGED', 4, DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR)),

-- 已升级报警
(4, 5, 13, 3, 'DO', 5.10, 6.00, '5号养成池DO持续偏低(5.1mg/L)，技术员5分钟未响应', 'ESCALATED', NULL, NULL, NULL, 2, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 7 HOUR)),

-- 已解除报警
(1, 1, 1, 3, 'DO', 5.80, 6.00, '1号养成池DO恢复至6.5mg/L，报警解除', 'RESOLVED', 3, DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 9 HOUR), NULL, NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(2, 2, 5, 3, 'DO', 5.90, 6.00, '2号养成池DO短暂低于阈值后恢复', 'RESOLVED', 4, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 23 HOUR), NULL, NULL, DATE_SUB(NOW(), INTERVAL 25 HOUR)),
(5, 5, 14, 2, 'NH4', 0.55, 0.50, '5号养成池氨氮轻微超标后恢复', 'RESOLVED', 3, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 46 HOUR), NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 1, 3, 2, 'TEMP', 22.30, 22.00, '1号养成池午间水温偏高后夜间自然回落', 'RESOLVED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, 1, 2, 1, 'PH', 6.95, 7.00, '1号养成池pH略低于7.0', 'RESOLVED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ============================================================
-- 9. 设备指令日志
-- ============================================================
INSERT INTO t_device_command_log (device_id, command_type, command_payload, triggered_by, rule_id, ack_status, ack_time, create_time) VALUES
(4, 'START', '{"action":"ON","speed":"HIGH"}', 'RULE', 1, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(4, 'STOP', '{"action":"OFF"}', 'RULE', 1, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(15, 'START', '{"action":"ON","speed":"HIGH"}', 'RULE', 4, 'TIMEOUT', NULL, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(8, 'START', '{"action":"ON","speed":"NORMAL"}', 'MANUAL', NULL, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(12, 'START', '{"action":"ON","speed":"HIGH"}', 'RULE', 3, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(12, 'STOP', '{"action":"OFF"}', 'RULE', 3, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 47 HOUR));

-- ============================================================
-- 10. 水质历史日志（模拟最近3小时每10分钟一条）
-- ============================================================
INSERT INTO t_water_quality_log (tank_id, device_id, parameter_type, value, recorded_at) VALUES
-- 1号池DO数据
(1, 1, 'DO', 7.20, DATE_SUB(NOW(), INTERVAL 180 MINUTE)),
(1, 1, 'DO', 7.10, DATE_SUB(NOW(), INTERVAL 170 MINUTE)),
(1, 1, 'DO', 6.90, DATE_SUB(NOW(), INTERVAL 160 MINUTE)),
(1, 1, 'DO', 6.80, DATE_SUB(NOW(), INTERVAL 150 MINUTE)),
(1, 1, 'DO', 6.50, DATE_SUB(NOW(), INTERVAL 140 MINUTE)),
(1, 1, 'DO', 6.20, DATE_SUB(NOW(), INTERVAL 130 MINUTE)),
(1, 1, 'DO', 5.80, DATE_SUB(NOW(), INTERVAL 120 MINUTE)),
(1, 1, 'DO', 5.50, DATE_SUB(NOW(), INTERVAL 110 MINUTE)),
(1, 1, 'DO', 5.80, DATE_SUB(NOW(), INTERVAL 100 MINUTE)),
(1, 1, 'DO', 6.20, DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(1, 1, 'DO', 6.50, DATE_SUB(NOW(), INTERVAL 80 MINUTE)),
(1, 1, 'DO', 6.80, DATE_SUB(NOW(), INTERVAL 70 MINUTE)),
(1, 1, 'DO', 7.00, DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
(1, 1, 'DO', 7.10, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(1, 1, 'DO', 7.20, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(1, 1, 'DO', 7.15, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 1, 'DO', 7.10, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(1, 1, 'DO', 7.05, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),

-- 5号池DO数据（异常池，持续低位）
(5, 13, 'DO', 6.10, DATE_SUB(NOW(), INTERVAL 180 MINUTE)),
(5, 13, 'DO', 5.90, DATE_SUB(NOW(), INTERVAL 170 MINUTE)),
(5, 13, 'DO', 5.70, DATE_SUB(NOW(), INTERVAL 160 MINUTE)),
(5, 13, 'DO', 5.50, DATE_SUB(NOW(), INTERVAL 150 MINUTE)),
(5, 13, 'DO', 5.30, DATE_SUB(NOW(), INTERVAL 140 MINUTE)),
(5, 13, 'DO', 5.10, DATE_SUB(NOW(), INTERVAL 130 MINUTE)),
(5, 13, 'DO', 4.90, DATE_SUB(NOW(), INTERVAL 120 MINUTE)),
(5, 13, 'DO', 4.80, DATE_SUB(NOW(), INTERVAL 110 MINUTE)),
(5, 13, 'DO', 4.70, DATE_SUB(NOW(), INTERVAL 100 MINUTE)),
(5, 13, 'DO', 4.80, DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(5, 13, 'DO', 4.90, DATE_SUB(NOW(), INTERVAL 80 MINUTE)),
(5, 13, 'DO', 4.80, DATE_SUB(NOW(), INTERVAL 70 MINUTE)),
(5, 13, 'DO', 4.85, DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
(5, 13, 'DO', 4.80, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(5, 13, 'DO', 4.75, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(5, 13, 'DO', 4.80, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(5, 13, 'DO', 4.85, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(5, 13, 'DO', 4.80, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),

-- 1号池pH
(1, 2, 'PH', 7.80, DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
(1, 2, 'PH', 7.75, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(1, 2, 'PH', 7.70, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(1, 2, 'PH', 7.72, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 2, 'PH', 7.68, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(1, 2, 'PH', 7.70, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),

-- 1号池温度
(1, 3, 'TEMP', 20.10, DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
(1, 3, 'TEMP', 20.20, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(1, 3, 'TEMP', 20.30, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(1, 3, 'TEMP', 20.25, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 3, 'TEMP', 20.20, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(1, 3, 'TEMP', 20.15, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),

-- 5号池氨氮
(5, 14, 'NH4', 0.42, DATE_SUB(NOW(), INTERVAL 120 MINUTE)),
(5, 14, 'NH4', 0.45, DATE_SUB(NOW(), INTERVAL 100 MINUTE)),
(5, 14, 'NH4', 0.48, DATE_SUB(NOW(), INTERVAL 80 MINUTE)),
(5, 14, 'NH4', 0.52, DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
(5, 14, 'NH4', 0.55, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(5, 14, 'NH4', 0.60, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(5, 14, 'NH4', 0.62, NOW());

-- ============================================================
-- 11. 审计日志
-- ============================================================
INSERT INTO t_audit_log (user_id, username, operation, module, description, request_method, request_url, ip, create_time) VALUES
(1, 'admin', 'LOGIN', 'AUTH', '管理员登录系统', 'POST', '/auth/login', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(3, 'tech01', 'LOGIN', 'AUTH', '技术员登录系统', 'POST', '/auth/login', '192.168.1.51', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(3, 'tech01', 'CREATE', 'BATCH', '录入投喂记录: 批次B-2026-03-01, 饲料27kg', 'POST', '/batch/feed', '192.168.1.51', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(3, 'tech01', 'CREATE', 'BATCH', '录入死亡记录: 批次B-2026-03-01, 死亡3尾(病害)', 'POST', '/batch/mortality', '192.168.1.51', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 'admin', 'UPDATE', 'RULE', '修改规则: 1号池DO低氧联动增氧, 阈值调整为6.0', 'PUT', '/rule/1', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(1, 'admin', 'CREATE', 'DEVICE', '注册设备: 5号池氨氮传感器 MAC=AA:BB:CC:05:01:04', 'POST', '/device/register', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 'tech02', 'COMMAND', 'DEVICE', '手动启动2号池曝气机', 'POST', '/device/command', '192.168.1.52', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'manager', 'EXPORT', 'BATCH', '导出批次B-2025-12-01全生命周期报表(PDF)', 'GET', '/batch/6/export', '192.168.1.200', DATE_SUB(NOW(), INTERVAL 2 DAY));
