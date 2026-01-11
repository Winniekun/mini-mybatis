-- ==========================================
-- Mini-MyBatis 测试数据库脚本
-- ==========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS mybatis_learn 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE mybatis_learn;

-- 删除已存在的表
DROP TABLE IF EXISTS `user`;

-- 创建用户表
CREATE TABLE `user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试数据
INSERT INTO `user` (`username`, `password`, `email`, `create_time`) VALUES
('zhangsan', '123456', 'zhangsan@example.com', NOW()),
('lisi', '123456', 'lisi@example.com', NOW()),
('wangwu', '123456', 'wangwu@example.com', NOW()),
('zhaoliu', '123456', 'zhaoliu@example.com', NOW()),
('admin', 'admin123', 'admin@example.com', NOW());

-- 查询数据
SELECT * FROM user;

-- ==========================================
-- 说明
-- ==========================================
-- 1. 这个脚本创建了一个简单的user表用于测试
-- 2. 表结构包含了常见的字段类型
-- 3. 字段名使用下划线命名（数据库规范）
-- 4. Java实体类使用驼峰命名
-- 5. MyBatis会自动处理两种命名之间的转换

-- ==========================================
-- 其他测试表（可选）
-- ==========================================

-- 订单表（用于测试一对多关系）
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
  `status` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 插入订单测试数据
INSERT INTO `orders` (`user_id`, `order_no`, `total_amount`, `status`) VALUES
(1, 'ORD20260107001', 299.00, 1),
(1, 'ORD20260107002', 599.00, 0),
(2, 'ORD20260107003', 199.00, 1),
(3, 'ORD20260107004', 899.00, 1);

