-- =============================================
-- 定时提醒系统 - 数据库初始化脚本
-- =============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_connection = utf8mb4;

CREATE DATABASE IF NOT EXISTS reminder_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE reminder_db;
SET NAMES utf8mb4;

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 定时提醒任务表
CREATE TABLE IF NOT EXISTS reminder_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    target_table VARCHAR(100) NOT NULL COMMENT '目标数据表',
    frequency_type VARCHAR(20) NOT NULL COMMENT '频率类型: DAILY/WEEKLY/MONTHLY',
    frequency_day INT COMMENT '星期几(1-7)或日期(1-31)',
    frequency_hour INT NOT NULL COMMENT '小时(0-23)',
    frequency_minute INT NOT NULL COMMENT '分钟(0-59)',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式',
    condition_mode VARCHAR(20) NOT NULL DEFAULT 'VISUAL' COMMENT '条件模式: VISUAL/EDITOR',
    condition_json TEXT COMMENT '可视化条件JSON',
    condition_sql TEXT COMMENT '自定义WHERE语句',
    message_template TEXT NOT NULL COMMENT '消息模板',
    receiver_field VARCHAR(100) NOT NULL COMMENT '接收方字段',
    dingtalk_webhook VARCHAR(500) COMMENT '钉钉Webhook地址',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-停用 1-启用',
    create_user VARCHAR(50) NOT NULL COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时提醒任务表';

-- 提醒条件明细表
CREATE TABLE IF NOT EXISTS reminder_condition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    field_name VARCHAR(100) NOT NULL COMMENT '字段名',
    operator VARCHAR(20) NOT NULL COMMENT '操作符',
    compare_type VARCHAR(20) NOT NULL DEFAULT 'VALUE' COMMENT '比较类型: FIELD/VALUE',
    compare_value VARCHAR(500) COMMENT '比较值',
    logic_operator VARCHAR(10) DEFAULT 'AND' COMMENT '与下一条件关系: AND/OR',
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒条件明细表';

-- 提醒发送日志表
CREATE TABLE IF NOT EXISTS reminder_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    task_name VARCHAR(200) COMMENT '任务名称',
    send_time DATETIME NOT NULL COMMENT '发送时间',
    receiver VARCHAR(200) COMMENT '接收方',
    send_content TEXT COMMENT '发送内容',
    send_status TINYINT DEFAULT 1 COMMENT '发送状态: 0-失败 1-成功',
    error_msg TEXT COMMENT '错误信息',
    KEY idx_task_id (task_id),
    KEY idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒发送日志表';

-- =============================================
-- 示例业务表（用于演示定时提醒功能）
-- =============================================

-- 示例-员工表
CREATE TABLE IF NOT EXISTS demo_employee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) COMMENT '姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    department VARCHAR(50) COMMENT '部门',
    position VARCHAR(50) COMMENT '职位',
    salary DECIMAL(10,2) COMMENT '薪资',
    hire_date DATE COMMENT '入职日期',
    contract_end DATE COMMENT '合同到期日',
    status VARCHAR(20) DEFAULT '在职' COMMENT '状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='示例-员工表';

-- 示例-订单表
CREATE TABLE IF NOT EXISTS demo_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) COMMENT '订单号',
    customer_name VARCHAR(50) COMMENT '客户名称',
    customer_phone VARCHAR(20) COMMENT '客户手机',
    amount DECIMAL(10,2) COMMENT '订单金额',
    status VARCHAR(20) DEFAULT '待处理' COMMENT '订单状态',
    due_date DATE COMMENT '截止日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='示例-订单表';

-- 示例-项目表
CREATE TABLE IF NOT EXISTS demo_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(100) COMMENT '项目名称',
    manager VARCHAR(50) COMMENT '负责人',
    manager_phone VARCHAR(20) COMMENT '负责人手机',
    budget DECIMAL(12,2) COMMENT '预算',
    spent DECIMAL(12,2) COMMENT '已花费',
    deadline DATE COMMENT '截止日期',
    progress INT DEFAULT 0 COMMENT '进度百分比',
    status VARCHAR(20) DEFAULT '进行中' COMMENT '状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='示例-项目表';

-- =============================================
-- 初始数据
-- =============================================

INSERT INTO sys_user (username, password, nickname, role) VALUES
('admin', 'admin123', '系统管理员', 'ADMIN'),
('zhangsan', 'user123', '张三', 'USER'),
('lisi', 'user123', '李四', 'USER');

INSERT INTO demo_employee (name, phone, email, department, position, salary, hire_date, contract_end, status) VALUES
('张三', '13800138001', 'zhangsan@example.com', '技术部', '高级开发工程师', 25000.00, '2022-01-15', '2025-01-14', '在职'),
('李四', '13800138002', 'lisi@example.com', '技术部', '测试工程师', 15000.00, '2023-03-20', '2026-03-19', '在职'),
('王五', '13800138003', 'wangwu@example.com', '销售部', '销售经理', 20000.00, '2021-06-01', '2024-05-31', '在职'),
('赵六', '13800138004', 'zhaoliu@example.com', '人事部', 'HR经理', 18000.00, '2022-07-10', '2025-07-09', '在职'),
('陈七', '13800138005', 'chenqi@example.com', '财务部', '财务主管', 22000.00, '2021-12-01', '2024-11-30', '在职'),
('钱八', '13800138006', 'qianba@example.com', '技术部', '前端开发', 16000.00, '2023-09-01', '2026-08-31', '在职'),
('孙九', '13800138007', 'sunji@example.com', '销售部', '销售代表', 12000.00, '2024-01-15', '2027-01-14', '在职'),
('周十', '13800138008', 'zhoushi@example.com', '技术部', '架构师', 35000.00, '2020-03-01', '2025-02-28', '离职');

INSERT INTO demo_order (order_no, customer_name, customer_phone, amount, status, due_date, create_time) VALUES
('ORD20240001', '甲公司', '13900139001', 50000.00, '待处理', '2025-03-30', '2025-01-01 10:00:00'),
('ORD20240002', '乙企业', '13900139002', 32000.00, '处理中', '2025-02-25', '2025-01-05 14:30:00'),
('ORD20240003', '丙集团', '13900139003', 85000.00, '已完成', '2025-01-20', '2025-01-10 09:15:00'),
('ORD20240004', '丁公司', '13900139004', 15000.00, '待处理', '2025-04-05', '2025-01-15 16:45:00'),
('ORD20240005', '戊科技', '13900139005', 120000.00, '已逾期', '2025-01-10', '2024-11-28 11:20:00'),
('ORD20240006', '己有限公司', '13900139006', 28000.00, '处理中', '2025-03-15', '2025-02-01 08:00:00');

INSERT INTO demo_project (project_name, manager, manager_phone, budget, spent, deadline, progress, status) VALUES
('智慧城市一期', '王五', '13800138003', 500000.00, 350000.00, '2025-06-30', 70, '进行中'),
('ERP系统升级', '张三', '13800138001', 200000.00, 180000.00, '2025-03-31', 90, '进行中'),
('移动APP开发', '周十', '13800138008', 300000.00, 310000.00, '2025-02-28', 95, '已超支'),
('数据中心建设', '赵六', '13800138004', 1000000.00, 200000.00, '2025-12-31', 20, '进行中'),
('安全审计项目', '陈七', '13800138005', 80000.00, 0.00, '2025-04-15', 0, '未开始');
