CREATE TABLE orders (
    `id` bigint(11) NOT NULL AUTO_INCREMENT,
    `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记, 0 是未删除, 1 是已删除',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建的时间',
    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改的时间',
    `order_id` varchar(255) NOT NULL UNIQUE COMMENT '订单ID',
    `price` decimal(13, 5) NOT NULL DEFAULT 0 COMMENT '订单金额',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息';


INSERT INTO orders(order_id, price)
VALUES
    ("70f9a7c6-bdd3-45e1-9bf9-0d61ce736b5e", 123.82),
    ("23ff2f2a-bc09-4444-b415-121bd79df5df", 9274.87),
    ("d4cf20e1-818d-40dc-bcfa-0f13422d1bcf", 392.1722),
    ("7c4e743d-5c92-4a3c-8346-51e7512b1913", 835.2),
    ("7580b5da-1c9f-4e42-b5ce-ba6ff2459ac6", 283),
    ("29cbd137-b431-4e07-861f-5df996248d91", 682.238);
