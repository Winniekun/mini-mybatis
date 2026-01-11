-- ==========================================
-- Day 2 实践 - Product表创建脚本
-- ==========================================

USE mybatis_learn;

-- 1. 创建商品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category VARCHAR(50) COMMENT '商品分类',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT DEFAULT 0 COMMENT '库存',
    description TEXT COMMENT '商品描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_category (category),
    INDEX idx_price (price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 2. 插入测试数据
INSERT INTO product (product_name, category, price, stock, description) VALUES
('iPhone 15 Pro', '手机', 7999.00, 50, '苹果最新款旗舰手机，A17 Pro芯片'),
('iPhone 15', '手机', 5999.00, 100, '苹果iPhone 15，性能强劲'),
('MacBook Pro 16', '电脑', 19999.00, 30, 'M3 Max芯片，专业级笔记本电脑'),
('MacBook Air', '电脑', 7999.00, 80, 'M2芯片，轻薄便携笔记本'),
('AirPods Pro 2', '耳机', 1999.00, 200, '主动降噪无线耳机'),
('AirPods 3', '耳机', 1399.00, 150, '空间音频无线耳机'),
('Apple Watch Ultra', '手表', 6299.00, 40, '户外运动智能手表'),
('Apple Watch Series 9', '手表', 2999.00, 120, '健康监测智能手表');

-- 3. 验证数据
SELECT * FROM product;

-- 4. 按分类查询
SELECT category, COUNT(*) as count, AVG(price) as avg_price
FROM product
GROUP BY category;

-- ==========================================
-- 说明
-- ==========================================
-- 1. product_name使用下划线命名（数据库规范）
-- 2. Java中对应productName（驼峰命名）
-- 3. MyBatis会自动处理两种命名风格的转换
-- 4. 添加了索引优化查询性能

